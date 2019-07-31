package no.nav.syfo.services

import com.ctc.wstx.exc.WstxException
import io.ktor.util.KtorExperimentalAPI
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.api.LegeSuspensjonClient
import no.nav.syfo.api.SyketilfelleClient
import no.nav.syfo.extractLogMeta
import no.nav.syfo.helpers.retry
import no.nav.syfo.model.Periode
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.model.Syketilfelle
import no.nav.syfo.model.SyketilfelleTag
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.rules.HPRRuleChain
import no.nav.syfo.rules.LegesuspensjonRuleChain
import no.nav.syfo.rules.PeriodLogicRuleChain
import no.nav.syfo.rules.PostTPSRuleChain
import no.nav.syfo.rules.Rule
import no.nav.syfo.rules.RuleMetadataAndForstegangsSykemelding
import no.nav.syfo.rules.SyketilfelleRuleChain
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.syfo.rules.executeFlow
import no.nhn.schemas.reg.hprv2.IHPR2Service
import no.nhn.schemas.reg.hprv2.IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.hprv2.Person
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@KtorExperimentalAPI
class RuleService(
    private val tpsService: TPSService,
    private val helsepersonellv1: IHPR2Service,
    private val legeSuspensjonClient: LegeSuspensjonClient,
    private val syketilfelleClient: SyketilfelleClient
) {
    private val log: Logger = LoggerFactory.getLogger("ruleservice")
    private val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()
    suspend fun executeRuleChains(receivedSykmelding: ReceivedSykmelding): ValidationResult = with(GlobalScope) {
        val logMeta = receivedSykmelding.extractLogMeta()

        log.info("Received a SM2013, going to rules, {}", fields(logMeta))

        val ruleMetadata = RuleMetadata(
                receivedDate = receivedSykmelding.mottattDato,
                signatureDate = receivedSykmelding.sykmelding.signaturDato,
                patientPersonNumber = receivedSykmelding.personNrPasient,
                rulesetVersion = receivedSykmelding.rulesetVersion,
                legekontorOrgnr = receivedSykmelding.legekontorOrgNr,
                tssid = receivedSykmelding.tssid
        )

        val doctorDeferred = async { fetchDoctor(receivedSykmelding.personNrLege) }
        val patientDeferred = async { tpsService.fetchPerson(receivedSykmelding.personNrPasient) }
        val doctorSuspendDeferred = async {
            val signaturDatoString = DateTimeFormatter.ISO_DATE.format(receivedSykmelding.sykmelding.signaturDato)
            legeSuspensjonClient.checkTherapist(receivedSykmelding.personNrLege, receivedSykmelding.navLogId, signaturDatoString).suspendert
        }
        val erNyttSyketilfelleDeferred = async {
            val syketilfelle = receivedSykmelding.sykmelding.perioder.intoSyketilfelle(
                    receivedSykmelding.sykmelding.pasientAktoerId, receivedSykmelding.mottattDato,
                    receivedSykmelding.msgId)

            syketilfelleClient.fetchErNytttilfelle(syketilfelle, receivedSykmelding.sykmelding.pasientAktoerId)
        }

        val doctor = try {
            doctorDeferred.await()
        } catch (e: IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage) {
            return ValidationResult(
                    status = Status.INVALID,
                    ruleHits = listOf(RuleInfo(
                            ruleName = "BEHANDLER_NOT_IN_HPR",
                            messageForSender = "Den som har skrevet sykmeldingen din har ikke autorisasjon til dette.",
                            messageForUser = "Behandler er ikke register i HPR"))
            )
        }

        // TODO kun datagrunlag for å evt legge til ny regel på tannleger, slette denne if-setningen etterpå
        if (doctor.godkjenninger.godkjenning.any {
                    it.autorisasjon?.isAktiv == true &&
                            it.helsepersonellkategori.isAktiv == true &&
                            it.helsepersonellkategori.verdi in listOf("TL") &&
                            receivedSykmelding.sykmelding.medisinskVurdering.hovedDiagnose?.kode != null &&
                            receivedSykmelding.sykmelding.perioder.isNotEmpty()
                }) {
            log.info("Tannlege statestikk: " +
                    "fom: ${receivedSykmelding.sykmelding.perioder.firstOrNull()?.fom} " +
                    "tom: ${receivedSykmelding.sykmelding.perioder.firstOrNull()?.tom} " +
                    "hovedDiagnose: ${receivedSykmelding.sykmelding.medisinskVurdering.hovedDiagnose?.kode }")
        }

        val ruleMetadataAndForstegangsSykemelding = RuleMetadataAndForstegangsSykemelding(
                ruleMetadata = ruleMetadata, erNyttSyketilfelle = erNyttSyketilfelleDeferred.await())

        val results = listOf(
                ValidationRuleChain.values().executeFlow(receivedSykmelding.sykmelding, ruleMetadata),
                PeriodLogicRuleChain.values().executeFlow(receivedSykmelding.sykmelding, ruleMetadata),
                HPRRuleChain.values().executeFlow(receivedSykmelding.sykmelding, doctorDeferred.await()),
                PostTPSRuleChain.values().executeFlow(receivedSykmelding.sykmelding, patientDeferred.await()),
                LegesuspensjonRuleChain.values().executeFlow(receivedSykmelding.sykmelding, doctorSuspendDeferred.await()),
                SyketilfelleRuleChain.values().executeFlow(receivedSykmelding.sykmelding, ruleMetadataAndForstegangsSykemelding)
        ).flatten()

        log.info("Rules hit {}, {}", results.map { it.name }, fields(logMeta))

        return validationResult(results)
    }

    private suspend fun fetchDoctor(doctorIdent: String): Person = retry(
            callName = "hpr_hent_person_med_personnummer",
            retryIntervals = arrayOf(500L, 1000L, 3000L, 5000L, 10000L),
            legalExceptions = *arrayOf(IOException::class, WstxException::class)
    ) {
        helsepersonellv1.hentPersonMedPersonnummer(doctorIdent, datatypeFactory.newXMLGregorianCalendar(GregorianCalendar()))
    }

    private fun List<Periode>.intoSyketilfelle(aktoerId: String, received: LocalDateTime, resourceId: String): List<Syketilfelle> = map {
        Syketilfelle(
                aktorId = aktoerId,
                orgnummer = null,
                inntruffet = received,
                tags = listOf(SyketilfelleTag.SYKMELDING, SyketilfelleTag.PERIODE, when {
                    it.aktivitetIkkeMulig != null -> SyketilfelleTag.INGEN_AKTIVITET
                    it.reisetilskudd -> SyketilfelleTag.FULL_AKTIVITET
                    it.gradert != null -> SyketilfelleTag.GRADERT_AKTIVITET
                    it.behandlingsdager != null -> SyketilfelleTag.BEHANDLINGSDAGER
                    it.avventendeInnspillTilArbeidsgiver != null -> SyketilfelleTag.FULL_AKTIVITET
                    else -> throw RuntimeException("Could not find aktivitetstype, this should never happen")
                }).joinToString(",") { tag -> tag.name },
                ressursId = resourceId,
                fom = it.fom.atStartOfDay(),
                tom = it.tom.atStartOfDay()
        )
    }

    private fun validationResult(results: List<Rule<Any>>): ValidationResult = ValidationResult(
            status = results
                    .map { status -> status.status }.let {
                        it.firstOrNull { status -> status == Status.INVALID }
                                ?: it.firstOrNull { status -> status == Status.MANUAL_PROCESSING }
                                ?: Status.OK
                    },
            ruleHits = results.map { rule -> RuleInfo(rule.name, rule.messageForSender!!, rule.messageForUser!!) }
    )
}
