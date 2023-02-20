package no.nav.syfo.services

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.client.Behandler
import no.nav.syfo.client.LegeSuspensjonClient
import no.nav.syfo.client.NorskHelsenettClient
import no.nav.syfo.client.SmregisterClient
import no.nav.syfo.client.SyketilfelleClient
import no.nav.syfo.metrics.RULE_NODE_RULE_HIT_COUNTER
import no.nav.syfo.metrics.RULE_NODE_RULE_PATH_COUNTER
import no.nav.syfo.model.Periode
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.RuleResult
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.rules.dsl.printRulePath
import no.nav.syfo.rules.hpr.HPRRulesExecution
import no.nav.syfo.rules.legesuspensjon.LegeSuspensjonRulesExecution
import no.nav.syfo.rules.periodlogic.PeriodLogicRulesExecution
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRulesExecution
import no.nav.syfo.rules.validation.ValidationRulesExecution
import no.nav.syfo.utils.LoggingMeta
import no.nav.syfo.validation.extractBornDate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class RuleService(
    private val legeSuspensjonClient: LegeSuspensjonClient,
    private val syketilfelleClient: SyketilfelleClient,
    private val norskHelsenettClient: NorskHelsenettClient,
    private val smregisterClient: SmregisterClient,
    private val pdlService: PdlPersonService,
    private val juridiskVurderingService: JuridiskVurderingService,
    private val tilbakedateringRulesExecution: TilbakedateringRulesExecution = TilbakedateringRulesExecution(),
    private val hprRulesExecution: HPRRulesExecution = HPRRulesExecution(),
    private val legeSuspensjonRulesExecution: LegeSuspensjonRulesExecution = LegeSuspensjonRulesExecution(),
    private val periodLogicRulesExecution: PeriodLogicRulesExecution = PeriodLogicRulesExecution(),
    private val validationRulesExecution: ValidationRulesExecution = ValidationRulesExecution()
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
            LocalDate.parse(fodsel.foedselsdato)
        } else {
            log.info("Extracting fodeseldato from personNrPasient")
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

        val tilbakedateringResult = tilbakedateringRulesExecution.runRules(
            sykmelding = receivedSykmelding.sykmelding, metadata = ruleMetadataSykmelding
        )

        RULE_NODE_RULE_HIT_COUNTER.labels(
            tilbakedateringResult.treeResult.status.name,
            tilbakedateringResult.treeResult.ruleHit?.rule ?: tilbakedateringResult.treeResult.status.name
        ).inc()

        RULE_NODE_RULE_PATH_COUNTER.labels(
            tilbakedateringResult.printRulePath()
        ).inc()

        val hprResult = hprRulesExecution.runRules(
            sykmelding = receivedSykmelding.sykmelding,
            behandlerOgStartdato = BehandlerOgStartdato(behandler, syketilfelleStartdato)
        )

        RULE_NODE_RULE_HIT_COUNTER.labels(
            hprResult.treeResult.status.name,
            hprResult.treeResult.ruleHit?.rule ?: hprResult.treeResult.status.name
        ).inc()

        RULE_NODE_RULE_PATH_COUNTER.labels(
            hprResult.printRulePath()
        ).inc()

        val legesuspensjonResult = legeSuspensjonRulesExecution.runRules(
            sykmeldingId = receivedSykmelding.sykmelding.id,
            behandlerSuspendert = doctorSuspendDeferred.await()
        )

        RULE_NODE_RULE_HIT_COUNTER.labels(
            legesuspensjonResult.treeResult.status.name,
            legesuspensjonResult.treeResult.ruleHit?.rule ?: legesuspensjonResult.treeResult.status.name
        ).inc()

        RULE_NODE_RULE_PATH_COUNTER.labels(
            legesuspensjonResult.printRulePath()
        ).inc()

        val periodLogicResult = periodLogicRulesExecution.runRules(
            sykmelding = receivedSykmelding.sykmelding,
            ruleMetadata = ruleMetadata
        )

        RULE_NODE_RULE_HIT_COUNTER.labels(
            periodLogicResult.treeResult.status.name,
            periodLogicResult.treeResult.ruleHit?.rule ?: periodLogicResult.treeResult.status.name
        ).inc()

        RULE_NODE_RULE_PATH_COUNTER.labels(
            periodLogicResult.printRulePath()
        ).inc()

        val validationResult = validationRulesExecution.runRules(
            sykmelding = receivedSykmelding.sykmelding,
            ruleMetadata = ruleMetadata
        )

        RULE_NODE_RULE_HIT_COUNTER.labels(
            validationResult.treeResult.status.name,
            validationResult.treeResult.ruleHit?.rule ?: validationResult.treeResult.status.name
        ).inc()

        RULE_NODE_RULE_PATH_COUNTER.labels(
            validationResult.printRulePath()
        ).inc()

        val result = listOf(
            tilbakedateringResult,
            hprResult,
            legesuspensjonResult,
            periodLogicResult,
            validationResult
        )

        // juridiskVurderingService.processRuleResults(receivedSykmelding, result)

        // return validationResult(result)
        return ValidationResult(
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
}
data class BehandlerOgStartdato(
    val behandler: Behandler,
    val startdato: LocalDate?,
)

data class RuleMetadataSykmelding(
    val ruleMetadata: RuleMetadata,
    val erNyttSyketilfelle: Boolean,
    val erEttersendingAvTidligereSykmelding: Boolean?,
)

fun List<Periode>.sortedFOMDate(): List<LocalDate> =
    map { it.fom }.sorted()

fun List<Periode>.sortedTOMDate(): List<LocalDate> =
    map { it.tom }.sorted()

fun ClosedRange<LocalDate>.daysBetween(): Long = ChronoUnit.DAYS.between(start, endInclusive)
