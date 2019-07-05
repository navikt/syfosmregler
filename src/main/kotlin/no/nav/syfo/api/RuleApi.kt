package no.nav.syfo.api

import com.ctc.wstx.exc.WstxException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.call
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.syfo.helpers.retry
import no.nav.syfo.metrics.RULE_HIT_STATUS_COUNTER
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
import no.nav.syfo.rules.RuleData
import no.nav.syfo.rules.RuleMetadataAndForstegangsSykemelding
import no.nav.syfo.rules.SyketilfelleRuleChain
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.syfo.rules.executeFlow
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import no.nhn.schemas.reg.hprv2.IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.xml.datatype.DatatypeFactory
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person as TPSPerson

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smregler")

val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
}

@KtorExperimentalAPI
fun Routing.registerRuleApi(
    personV3: PersonV3,
    helsepersonellClient: HelsepersonellClient,
    legeSuspensjonClient: LegeSuspensjonClient,
    syketilfelleClient: SyketilfelleClient
) {
    post("/v1/rules/validate") {
        log.info("Got an request to validate rules")

        val receivedSykmeldingText = call.receiveText()

        if (log.isDebugEnabled) {
            log.debug(receivedSykmeldingText)
        }
        val receivedSykmelding: ReceivedSykmelding = objectMapper.readValue(receivedSykmeldingText)

        val logValues = arrayOf(
            keyValue("mottakId", receivedSykmelding.navLogId),
            keyValue("organizationNumber", receivedSykmelding.legekontorOrgNr),
            keyValue("msgId", receivedSykmelding.msgId),
            keyValue("sykmeldingId", receivedSykmelding.sykmelding.id)
        )

        val logKeys = logValues.joinToString(prefix = "(", postfix = ")", separator = ",") {
            "{}"
        }

        log.info("Received a SM2013, going to rules, $logKeys", *logValues)

        val validationAndPeriodRuleResults: List<Rule<Any>> = listOf<List<Rule<RuleData<RuleMetadata>>>>(
            ValidationRuleChain.values().toList(),
            PeriodLogicRuleChain.values().toList()
        ).flatten().executeFlow(
            receivedSykmelding.sykmelding, RuleMetadata(
                receivedDate = receivedSykmelding.mottattDato,
                signatureDate = receivedSykmelding.sykmelding.signaturDato,
                patientPersonNumber = receivedSykmelding.personNrPasient,
                rulesetVersion = receivedSykmelding.rulesetVersion,
                legekontorOrgnr = receivedSykmelding.legekontorOrgNr,
                tssid = receivedSykmelding.tssid
            )
        )

        val lege: Lege
        try {
            lege = helsepersonellClient.hentLege(receivedSykmelding.personNrLege)
        } catch (e: IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage) {
            log.error(e.message)
            val validationResult = ValidationResult(
                status = Status.INVALID,
                ruleHits = listOf(RuleInfo(
                    ruleName = "BEHANDLER_NOT_IN_HPR",
                    messageForSender = "Den som har skrevet sykmeldingen din har ikke autorisasjon til dette.",
                    messageForUser = "Behandler er ikke register i HPR"))
            )
            RULE_HIT_STATUS_COUNTER.labels(validationResult.status.name).inc()
            call.respond(validationResult)
            return@post
        }

        val hprRuleResults = HPRRuleChain.values().executeFlow(receivedSykmelding.sykmelding, lege)

        // TODO kun datagrunlag for å evt legge til ny regel på tannleger, slette denne if-setningen etterpå

        if (lege.godkjenninger.any {
                erGyldigTannlege(it) && sykmeldingMedDiagnoseOgPerioder(receivedSykmelding)
            }) {
            log.info(
                "Tannlege statestikk: " +
                        "fom: ${receivedSykmelding.sykmelding.perioder.firstOrNull()?.fom} " +
                        "tom: ${receivedSykmelding.sykmelding.perioder.firstOrNull()?.tom} " +
                        "houvedDiagnose: ${receivedSykmelding.sykmelding.medisinskVurdering.hovedDiagnose?.kode}"
            )
        }

        val patient = fetchPerson(personV3, receivedSykmelding.personNrPasient)
        val tpsRuleResults = PostTPSRuleChain.values().executeFlow(receivedSykmelding.sykmelding, patient.await())

        val signaturDatoString =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").format(receivedSykmelding.sykmelding.signaturDato)
        val doctorSuspend = legeSuspensjonClient.checkTherapist(
            receivedSykmelding.personNrLege,
            receivedSykmelding.navLogId,
            signaturDatoString
        ).suspendert
        val doctorRuleResults =
            LegesuspensjonRuleChain.values().executeFlow(receivedSykmelding.sykmelding, doctorSuspend)

        val erNyttSyketilfelle = syketilfelleClient.fetchErNytttilfelle(
            receivedSykmelding.sykmelding.perioder.intoSyketilfelle(
                receivedSykmelding.sykmelding.pasientAktoerId, receivedSykmelding.mottattDato, receivedSykmelding.msgId
            ),
            receivedSykmelding.sykmelding.pasientAktoerId
        )

        val ruleMetadataAndForstegangsSykemelding = RuleMetadataAndForstegangsSykemelding(
            ruleMetadata = RuleMetadata(
                receivedDate = receivedSykmelding.mottattDato,
                signatureDate = receivedSykmelding.sykmelding.signaturDato,
                patientPersonNumber = receivedSykmelding.personNrPasient,
                rulesetVersion = receivedSykmelding.rulesetVersion,
                legekontorOrgnr = receivedSykmelding.legekontorOrgNr,
                tssid = receivedSykmelding.tssid
            ), erNyttSyketilfelle = erNyttSyketilfelle
        )
        val syketilfelleResults = SyketilfelleRuleChain.values()
            .executeFlow(receivedSykmelding.sykmelding, ruleMetadataAndForstegangsSykemelding)
        val results = listOf(
            validationAndPeriodRuleResults,
            tpsRuleResults,
            hprRuleResults,
            doctorRuleResults,
            syketilfelleResults
        ).flatten()

        log.info("Rules hit {}, $logKeys", results.map { it.name }, *logValues)

        val validationResult = validationResult(results)
        RULE_HIT_STATUS_COUNTER.labels(validationResult.status.name).inc()
        call.respond(validationResult)
    }
}

private fun erGyldigTannlege(
    it: Godkjenning
): Boolean {
    return it.autorisasjon?.aktiv == true &&
            it.helsepersonellkategori != null &&
            it.helsepersonellkategori.aktiv == true &&
            it.helsepersonellkategori.verdi == "TL"
}

private fun sykmeldingMedDiagnoseOgPerioder(receivedSykmelding: ReceivedSykmelding): Boolean {
    return receivedSykmelding.sykmelding.medisinskVurdering.hovedDiagnose?.kode != null &&
            receivedSykmelding.sykmelding.perioder.isNotEmpty()
}

fun List<Periode>.intoSyketilfelle(aktoerId: String, received: LocalDateTime, resourceId: String): List<Syketilfelle> =
    map {
        Syketilfelle(
            aktorId = aktoerId,
            orgnummer = null,
            inntruffet = received,
            tags = listOf(
                SyketilfelleTag.SYKMELDING, SyketilfelleTag.PERIODE, when {
                    it.aktivitetIkkeMulig != null -> SyketilfelleTag.INGEN_AKTIVITET
                    it.reisetilskudd -> SyketilfelleTag.FULL_AKTIVITET
                    it.gradert != null -> SyketilfelleTag.GRADERT_AKTIVITET
                    it.behandlingsdager != null -> SyketilfelleTag.BEHANDLINGSDAGER
                    it.avventendeInnspillTilArbeidsgiver != null -> SyketilfelleTag.FULL_AKTIVITET
                    else -> throw RuntimeException("Could not find aktivitetstype, this should never happen")
                }
            ).joinToString(",") { tag -> tag.name },
            ressursId = resourceId,
            fom = it.fom.atStartOfDay(),
            tom = it.tom.atStartOfDay()
        )
    }

fun CoroutineScope.fetchPerson(personV3: PersonV3, ident: String): Deferred<TPSPerson> = async {
    retry(
        callName = "tps_hent_person",
        retryIntervals = arrayOf(500L, 1000L, 3000L, 5000L, 10000L, 60000L),
        legalExceptions = *arrayOf(IOException::class, WstxException::class)
    ) {
        personV3.hentPerson(
            HentPersonRequest()
                .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(ident)))
        ).person
    }
}

fun validationResult(results: List<Rule<Any>>): ValidationResult =
    ValidationResult(
        status = results
            .map { status -> status.status }.let {
                it.firstOrNull { status -> status == Status.INVALID }
                    ?: it.firstOrNull { status -> status == Status.MANUAL_PROCESSING }
                    ?: Status.OK
            },
        ruleHits = results.map { rule -> RuleInfo(rule.name, rule.messageForSender!!, rule.messageForUser!!) }
    )
