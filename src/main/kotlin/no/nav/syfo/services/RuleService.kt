package no.nav.syfo.services

import io.ktor.util.KtorExperimentalAPI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.LoggingMeta
import no.nav.syfo.client.LegeSuspensjonClient
import no.nav.syfo.client.NorskHelsenettClient
import no.nav.syfo.client.SyketilfelleClient
import no.nav.syfo.model.AnnenFraverGrunn
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.rules.BehandlerOgStartdato
import no.nav.syfo.rules.HPRRuleChain
import no.nav.syfo.rules.LegesuspensjonRuleChain
import no.nav.syfo.rules.PeriodLogicRuleChain
import no.nav.syfo.rules.Rule
import no.nav.syfo.rules.RuleMetadataSykmelding
import no.nav.syfo.rules.SyketilfelleRuleChain
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.syfo.rules.executeFlow
import no.nav.syfo.sm.isICPC2
import no.nav.syfo.sm.isICpc10
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@KtorExperimentalAPI
class RuleService(
    private val legeSuspensjonClient: LegeSuspensjonClient,
    private val syketilfelleClient: SyketilfelleClient,
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
                tssid = receivedSykmelding.tssid,
                avsenderFnr = receivedSykmelding.personNrLege
        )

        val doctorSuspendDeferred = async {
            val signaturDatoString = DateTimeFormatter.ISO_DATE.format(receivedSykmelding.sykmelding.signaturDato)
            legeSuspensjonClient.checkTherapist(receivedSykmelding.personNrLege, receivedSykmelding.navLogId, signaturDatoString, loggingMeta).suspendert
        }
        val syketilfelleStartdatoDeferred = async {
            syketilfelleClient.finnStartdatoForSammenhengendeSyketilfelle(receivedSykmelding.sykmelding.pasientAktoerId, receivedSykmelding.sykmelding.perioder, loggingMeta)
        }

        val behandler = norskHelsenettClient.finnBehandler(behandlerFnr = receivedSykmelding.personNrLege, msgId = receivedSykmelding.msgId, loggingMeta = loggingMeta)
                ?: return ValidationResult(
                        status = Status.INVALID,
                        ruleHits = listOf(RuleInfo(
                                ruleName = "BEHANDLER_NOT_IN_HPR",
                                messageForSender = "Den som har skrevet sykmeldingen ble ikke funnet i Helsepersonellregisteret (HPR)",
                                messageForUser = "Avsender fodselsnummer er ikke registert i Helsepersonellregisteret (HPR)",
                                ruleStatus = Status.INVALID))
                )

        log.info("Avsender behandler har hprnummer: ${behandler.hprNummer}, {}", fields(loggingMeta))

        val syketilfelleStartdato = syketilfelleStartdatoDeferred.await()
        val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = ruleMetadata, erNyttSyketilfelle = syketilfelleStartdato == null)

        val results = listOf(
                ValidationRuleChain.values().executeFlow(receivedSykmelding.sykmelding, ruleMetadata),
                PeriodLogicRuleChain.values().executeFlow(receivedSykmelding.sykmelding, ruleMetadata),
                HPRRuleChain.values().executeFlow(receivedSykmelding.sykmelding, BehandlerOgStartdato(behandler, syketilfelleStartdato)),
                LegesuspensjonRuleChain.values().executeFlow(receivedSykmelding.sykmelding, doctorSuspendDeferred.await()),
                SyketilfelleRuleChain.values().executeFlow(receivedSykmelding.sykmelding, ruleMetadataSykmelding)
        ).flatten()

        log.info("Rules hit {}, {}", results.map { it.name }, fields(loggingMeta))

        return validationResult(results)
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

/**
 * Spesialsjekk for å avlaste behandlere i kjølvannet av COVID-19-utbruddet mars 2020.
 */
fun erCoronaRelatert(sykmelding: Sykmelding): Boolean {
    return ((sykmelding.medisinskVurdering.hovedDiagnose?.isICPC2() ?: false && sykmelding.medisinskVurdering.hovedDiagnose?.kode == "R991") ||
            (sykmelding.medisinskVurdering.hovedDiagnose?.isICPC2() ?: false && sykmelding.medisinskVurdering.biDiagnoser.any { it.kode == "R991" }) ||
            (sykmelding.medisinskVurdering.hovedDiagnose?.isICPC2() ?: false && sykmelding.medisinskVurdering.hovedDiagnose?.kode == "R992") ||
            (sykmelding.medisinskVurdering.hovedDiagnose?.isICPC2() ?: false && sykmelding.medisinskVurdering.biDiagnoser.any { it.kode == "R992" }) ||
            (sykmelding.medisinskVurdering.hovedDiagnose?.isICpc10() ?: false && sykmelding.medisinskVurdering.hovedDiagnose?.kode == "U071") ||
            (sykmelding.medisinskVurdering.hovedDiagnose?.isICpc10() ?: false && sykmelding.medisinskVurdering.biDiagnoser.any { it.kode == "U071" }) ||
            (sykmelding.medisinskVurdering.hovedDiagnose?.isICpc10() ?: false && sykmelding.medisinskVurdering.hovedDiagnose?.kode == "U072") ||
            (sykmelding.medisinskVurdering.hovedDiagnose?.isICpc10() ?: false && sykmelding.medisinskVurdering.biDiagnoser.any { it.kode == "U072" }) ||
            sykmelding.medisinskVurdering.annenFraversArsak?.grunn?.any { it == AnnenFraverGrunn.SMITTEFARE } ?: false) &&
            sykmelding.perioder.any { it.fom.isAfter(LocalDate.of(2020, 2, 24)) }
}
