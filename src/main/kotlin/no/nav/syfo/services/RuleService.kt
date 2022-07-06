package no.nav.syfo.services

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.LoggingMeta
import no.nav.syfo.client.LegeSuspensjonClient
import no.nav.syfo.client.NorskHelsenettClient
import no.nav.syfo.client.SmregisterClient
import no.nav.syfo.client.SyketilfelleClient
import no.nav.syfo.metrics.FODSELSDATO_FRA_IDENT_COUNTER
import no.nav.syfo.metrics.FODSELSDATO_FRA_PDL_COUNTER
import no.nav.syfo.metrics.RULE_HIT_COUNTER
import no.nav.syfo.model.AnnenFraverGrunn
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.RuleResult
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.rules.BehandlerOgStartdato
import no.nav.syfo.rules.HPRRuleChain
import no.nav.syfo.rules.LegesuspensjonRuleChain
import no.nav.syfo.rules.PeriodLogicRuleChain
import no.nav.syfo.rules.RuleMetadataSykmelding
import no.nav.syfo.rules.SyketilfelleRuleChain
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.syfo.rules.sortedFOMDate
import no.nav.syfo.sm.isICD10
import no.nav.syfo.sm.isICPC2
import no.nav.syfo.validation.extractBornDate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RuleService(
    private val legeSuspensjonClient: LegeSuspensjonClient,
    private val syketilfelleClient: SyketilfelleClient,
    private val norskHelsenettClient: NorskHelsenettClient,
    private val smregisterClient: SmregisterClient,
    private val pdlService: PdlPersonService,
    private val juridiskVurderingService: JuridiskVurderingService,
) {
    private val log: Logger = LoggerFactory.getLogger("ruleservice")

    @DelicateCoroutinesApi
    suspend fun executeRuleChains(receivedSykmelding: ReceivedSykmelding): ValidationResult = with(GlobalScope) {
        val loggingMeta = LoggingMeta(
            mottakId = receivedSykmelding.navLogId,
            orgNr = receivedSykmelding.legekontorOrgNr,
            msgId = receivedSykmelding.msgId,
            sykmeldingId = receivedSykmelding.sykmelding.id
        )

        log.info("Received a SM2013, going to rules, {}", fields(loggingMeta))

        val pdlPerson = pdlService.getPdlPerson(receivedSykmelding.personNrPasient, loggingMeta)
        val fodsel = pdlPerson.foedsel?.firstOrNull()
        val fodselsdato = if (fodsel?.foedselsdato?.isNotEmpty() == true) {
            log.info("Extracting fodeseldato from PDL date")
            FODSELSDATO_FRA_PDL_COUNTER.inc()
            LocalDate.parse(fodsel.foedselsdato)
        } else {
            log.info("Extracting fodeseldato from personNrPasient")
            FODSELSDATO_FRA_IDENT_COUNTER.inc()
            extractBornDate(receivedSykmelding.personNrPasient)
        }

        val ruleMetadata = RuleMetadata(
            receivedDate = receivedSykmelding.mottattDato,
            signatureDate = receivedSykmelding.sykmelding.signaturDato,
            behandletTidspunkt = receivedSykmelding.sykmelding.behandletTidspunkt,
            patientPersonNumber = receivedSykmelding.personNrPasient,
            rulesetVersion = receivedSykmelding.rulesetVersion,
            legekontorOrgnr = receivedSykmelding.legekontorOrgNr,
            tssid = receivedSykmelding.tssid,
            avsenderFnr = receivedSykmelding.personNrLege,
            pasientFodselsdato = fodselsdato
        )

        val doctorSuspendDeferred = async {
            val signaturDatoString = DateTimeFormatter.ISO_DATE.format(receivedSykmelding.sykmelding.signaturDato)
            legeSuspensjonClient.checkTherapist(
                receivedSykmelding.personNrLege,
                receivedSykmelding.navLogId,
                signaturDatoString,
                loggingMeta
            ).suspendert
        }
        val syketilfelleStartdatoDeferred = async {
            syketilfelleClient.finnStartdatoForSammenhengendeSyketilfelle(
                receivedSykmelding.personNrPasient,
                receivedSykmelding.sykmelding.perioder,
                loggingMeta
            )
        }

        val behandler = norskHelsenettClient.finnBehandler(
            behandlerFnr = receivedSykmelding.personNrLege,
            msgId = receivedSykmelding.msgId,
            loggingMeta = loggingMeta
        )
            ?: return ValidationResult(
                status = Status.INVALID,
                ruleHits = listOf(
                    RuleInfo(
                        ruleName = "BEHANDLER_NOT_IN_HPR",
                        messageForSender = "Den som har skrevet sykmeldingen ble ikke funnet i Helsepersonellregisteret (HPR)",
                        messageForUser = "Avsender fodselsnummer er ikke registert i Helsepersonellregisteret (HPR)",
                        ruleStatus = Status.INVALID
                    )
                )
            )

        log.info("Avsender behandler har hprnummer: ${behandler.hprNummer}, {}", fields(loggingMeta))

        val erEttersendingAvTidligereSykmelding = if (erTilbakedatertMedBegrunnelse(receivedSykmelding)) {
            smregisterClient.finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert(
                receivedSykmelding.personNrPasient,
                receivedSykmelding.sykmelding.perioder,
                receivedSykmelding.sykmelding.medisinskVurdering.hovedDiagnose?.kode,
                loggingMeta
            )
        } else {
            null
        }

        val syketilfelleStartdato = syketilfelleStartdatoDeferred.await()
        val ruleMetadataSykmelding = RuleMetadataSykmelding(
            ruleMetadata = ruleMetadata,
            erNyttSyketilfelle = syketilfelleStartdato == null,
            erEttersendingAvTidligereSykmelding = erEttersendingAvTidligereSykmelding
        )

        val result = listOf(
            ValidationRuleChain(receivedSykmelding.sykmelding, ruleMetadata).executeRules(),
            PeriodLogicRuleChain(receivedSykmelding.sykmelding, ruleMetadata).executeRules(),
            HPRRuleChain(
                receivedSykmelding.sykmelding,
                BehandlerOgStartdato(behandler, syketilfelleStartdato)
            ).executeRules(),
            LegesuspensjonRuleChain(doctorSuspendDeferred.await()).executeRules(),
            SyketilfelleRuleChain(receivedSykmelding.sykmelding, ruleMetadataSykmelding).executeRules(),
        ).flatten()

        logRuleResultMetrics(result)

        juridiskVurderingService.processRuleResults(receivedSykmelding, result)

        log.info("Rules hit ${result.filter { it.result }.map { it.rule.name }}, ${fields(loggingMeta)}")

        return validationResult(result)
    }

    private fun validationResult(results: List<RuleResult<*>>): ValidationResult = ValidationResult(
        status = results
            .filter { it.result }
            .map { result -> result.rule.status }.let {
                it.firstOrNull { status -> status == Status.INVALID }
                    ?: it.firstOrNull { status -> status == Status.MANUAL_PROCESSING }
                    ?: Status.OK
            },
        ruleHits = results
            .filter { it.result }
            .map { result ->
                RuleInfo(
                    result.rule.name,
                    result.rule.messageForSender,
                    result.rule.messageForUser,
                    result.rule.status
                )
            }
    )

    private fun erTilbakedatertMedBegrunnelse(receivedSykmelding: ReceivedSykmelding): Boolean =
        receivedSykmelding.sykmelding.behandletTidspunkt.toLocalDate() > receivedSykmelding.sykmelding.perioder.sortedFOMDate()
            .first().plusDays(8) &&
            !receivedSykmelding.sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty()

    private fun logRuleResultMetrics(result: List<RuleResult<*>>) {
        result
            .filter { it.result }
            .forEach {
                RULE_HIT_COUNTER.labels(it.rule.name).inc()
            }
    }
}

/**
 * Spesialsjekk for å avlaste behandlere i kjølvannet av COVID-19-utbruddet mars 2020.
 */
fun erCoronaRelatert(sykmelding: Sykmelding): Boolean {
    return (
        (sykmelding.medisinskVurdering.hovedDiagnose?.isICPC2() ?: false && sykmelding.medisinskVurdering.hovedDiagnose?.kode == "R991") ||
            (sykmelding.medisinskVurdering.hovedDiagnose?.isICPC2() ?: false && sykmelding.medisinskVurdering.biDiagnoser.any { it.kode == "R991" }) ||
            (sykmelding.medisinskVurdering.hovedDiagnose?.isICPC2() ?: false && sykmelding.medisinskVurdering.hovedDiagnose?.kode == "R992") ||
            (sykmelding.medisinskVurdering.hovedDiagnose?.isICPC2() ?: false && sykmelding.medisinskVurdering.biDiagnoser.any { it.kode == "R992" }) ||
            (sykmelding.medisinskVurdering.hovedDiagnose?.isICD10() ?: false && sykmelding.medisinskVurdering.hovedDiagnose?.kode == "U071") ||
            (sykmelding.medisinskVurdering.hovedDiagnose?.isICD10() ?: false && sykmelding.medisinskVurdering.biDiagnoser.any { it.kode == "U071" }) ||
            (sykmelding.medisinskVurdering.hovedDiagnose?.isICD10() ?: false && sykmelding.medisinskVurdering.hovedDiagnose?.kode == "U072") ||
            (sykmelding.medisinskVurdering.hovedDiagnose?.isICD10() ?: false && sykmelding.medisinskVurdering.biDiagnoser.any { it.kode == "U072" }) ||
            sykmelding.medisinskVurdering.annenFraversArsak?.grunn?.any { it == AnnenFraverGrunn.SMITTEFARE } ?: false
        ) &&
        sykmelding.perioder.any { it.fom.isAfter(LocalDate.of(2020, 2, 24)) }
}

fun kommerFraSpesialisthelsetjenesten(sykmelding: Sykmelding): Boolean {
    return sykmelding.medisinskVurdering.hovedDiagnose?.isICD10() ?: false
}
