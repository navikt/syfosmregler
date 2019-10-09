package no.nav.syfo.services

import io.ktor.util.KtorExperimentalAPI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.LoggingMeta
import no.nav.syfo.api.LegeSuspensjonClient
import no.nav.syfo.api.NorskHelsenettClient
import no.nav.syfo.api.SyketilfelleClient
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
import no.nav.syfo.rules.PostDiskresjonskodeRuleChain
import no.nav.syfo.rules.Rule
import no.nav.syfo.rules.RuleMetadataAndForstegangsSykemelding
import no.nav.syfo.rules.SyketilfelleRuleChain
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.syfo.rules.executeFlow
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@KtorExperimentalAPI
class RuleService(
    private val legeSuspensjonClient: LegeSuspensjonClient,
    private val syketilfelleClient: SyketilfelleClient,
    private val diskresjonskodeService: DiskresjonskodeService,
    private val norskHelsenettClient: NorskHelsenettClient
) {
    private val log: Logger = LoggerFactory.getLogger("ruleservice")
    suspend fun executeRuleChains(receivedSykmelding: ReceivedSykmelding): ValidationResult = with(GlobalScope) {

        val loggingMeta = LoggingMeta(
                mottakId = receivedSykmelding.navLogId,
                orgNr = receivedSykmelding.legekontorOrgNr,
                msgId = receivedSykmelding.msgId,
                sykmeldingId = receivedSykmelding.sykmelding.id
        )

        log.info("Received a SM2013, going to rules, {}", fields(loggingMeta))

        val ruleMetadata = RuleMetadata(
                receivedDate = receivedSykmelding.mottattDato,
                signatureDate = receivedSykmelding.sykmelding.signaturDato,
                behandletTidspunkt = receivedSykmelding.sykmelding.behandletTidspunkt,
                patientPersonNumber = receivedSykmelding.personNrPasient,
                rulesetVersion = receivedSykmelding.rulesetVersion,
                legekontorOrgnr = receivedSykmelding.legekontorOrgNr,
                tssid = receivedSykmelding.tssid
        )

        val patientDiskresjonskodeDeferred = async { diskresjonskodeService.hentDiskresjonskode(receivedSykmelding.personNrPasient) }
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

        val behandler = norskHelsenettClient.finnBehandler(behandlerFnr = receivedSykmelding.personNrLege, msgId = receivedSykmelding.msgId, loggingMeta = loggingMeta) ?: return ValidationResult(
            status = Status.INVALID,
            ruleHits = listOf(RuleInfo(
                ruleName = "BEHANDLER_NOT_IN_HPR",
                messageForSender = "Den som har skrevet sykmeldingen din har ikke autorisasjon til dette.",
                messageForUser = "Behandler er ikke register i HPR",
                ruleStatus = Status.INVALID))
        )

        log.info("Avsender behandler har hprnummer: ${behandler.hprNummer}")

        val ruleMetadataAndForstegangsSykemelding = RuleMetadataAndForstegangsSykemelding(
                ruleMetadata = ruleMetadata, erNyttSyketilfelle = erNyttSyketilfelleDeferred.await())

        val results = listOf(
                ValidationRuleChain.values().executeFlow(receivedSykmelding.sykmelding, ruleMetadata),
                PeriodLogicRuleChain.values().executeFlow(receivedSykmelding.sykmelding, ruleMetadata),
                HPRRuleChain.values().executeFlow(receivedSykmelding.sykmelding, behandler),
                PostDiskresjonskodeRuleChain.values().executeFlow(receivedSykmelding.sykmelding, patientDiskresjonskodeDeferred.await()),
                LegesuspensjonRuleChain.values().executeFlow(receivedSykmelding.sykmelding, doctorSuspendDeferred.await()),
                SyketilfelleRuleChain.values().executeFlow(receivedSykmelding.sykmelding, ruleMetadataAndForstegangsSykemelding)
        ).flatten()

        log.info("Rules hit {}, {}", results.map { it.name }, fields(loggingMeta))

        return validationResult(results)
    }

    private fun List<Periode>.intoSyketilfelle(aktoerId: String, received: LocalDateTime, resourceId: String): List<Syketilfelle> = map {
        Syketilfelle(
                aktorId = aktoerId,
                orgnummer = null,
                inntruffet = received,
                tags = listOf(SyketilfelleTag.SYKMELDING, SyketilfelleTag.NY, SyketilfelleTag.PERIODE, when {
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
            ruleHits = results.map { rule -> RuleInfo(rule.name, rule.messageForSender!!, rule.messageForUser!!, rule.status) }
    )
}
