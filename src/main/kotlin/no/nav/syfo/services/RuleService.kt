package no.nav.syfo.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.client.Behandler
import no.nav.syfo.client.LegeSuspensjonClient
import no.nav.syfo.client.NorskHelsenettClient
import no.nav.syfo.metrics.RULE_NODE_RULE_HIT_COUNTER
import no.nav.syfo.metrics.RULE_NODE_RULE_PATH_COUNTER
import no.nav.syfo.model.Periode
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.objectMapper
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.printRulePath
import no.nav.syfo.secureLog
import no.nav.syfo.utils.LoggingMeta
import no.nav.syfo.validation.extractBornDate
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.RegulaStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RuleService(
    private val legeSuspensjonClient: LegeSuspensjonClient,
    private val norskHelsenettClient: NorskHelsenettClient,
    private val sykmeldingService: SykmeldingService,
    private val pdlService: PdlPersonService,
    private val juridiskVurderingService: JuridiskVurderingService,
    private val ruleExecutionService: RuleExecutionService,
) {
    private val log: Logger = LoggerFactory.getLogger("ruleservice")

    @DelicateCoroutinesApi
    suspend fun executeRuleChains(receivedSykmelding: ReceivedSykmelding): RegulaResult =
        with(GlobalScope) {
            val loggingMeta =
                LoggingMeta(
                    mottakId = receivedSykmelding.navLogId,
                    orgNr = receivedSykmelding.legekontorOrgNr,
                    msgId = receivedSykmelding.msgId,
                    sykmeldingId = receivedSykmelding.sykmelding.id,
                )

            log.info("Received a SM2013, going to rules, {}", fields(loggingMeta))

            val pdlPerson = pdlService.getPdlPerson(receivedSykmelding.personNrPasient, loggingMeta)
            val fodsel = pdlPerson.foedselsdato?.firstOrNull()
            val fodselsdato =
                if (fodsel?.foedselsdato?.isNotEmpty() == true) {
                    log.info("Extracting fodeseldato from PDL date")
                    LocalDate.parse(fodsel.foedselsdato)
                } else {
                    log.info("Extracting fodeseldato from personNrPasient")
                    extractBornDate(receivedSykmelding.personNrPasient)
                }

            val ruleMetadata =
                RuleMetadata(
                    receivedDate = receivedSykmelding.mottattDato,
                    signatureDate = receivedSykmelding.sykmelding.signaturDato,
                    behandletTidspunkt = receivedSykmelding.sykmelding.behandletTidspunkt,
                    patientPersonNumber = receivedSykmelding.personNrPasient,
                    rulesetVersion = receivedSykmelding.rulesetVersion,
                    legekontorOrgnr = receivedSykmelding.legekontorOrgNr,
                    tssid = receivedSykmelding.tssid,
                    avsenderFnr = receivedSykmelding.personNrLege,
                    pasientFodselsdato = fodselsdato,
                )

            val doctorSuspendDeferred = async {
                val signaturDatoString =
                    DateTimeFormatter.ISO_DATE.format(receivedSykmelding.sykmelding.signaturDato)
                // TODO: HISTO HER
                legeSuspensjonClient
                    .checkTherapist(
                        receivedSykmelding.personNrLege,
                        receivedSykmelding.navLogId,
                        signaturDatoString,
                        loggingMeta,
                    )
                    .suspendert
            }

            val behandler =
                norskHelsenettClient.finnBehandler(
                    behandlerFnr = receivedSykmelding.personNrLege,
                    msgId = receivedSykmelding.msgId,
                    loggingMeta = loggingMeta,
                )

            log.info(
                "Avsender behandler har hprnummer: ${behandler?.hprNummer ?: "[finnes ikke i hpr]"}, {}",
                fields(loggingMeta),
            )

            val sykmeldingMetadata =
                sykmeldingService.getSykmeldingMetadataInfo(
                    receivedSykmelding.personNrPasient,
                    receivedSykmelding.sykmelding,
                    loggingMeta,
                )

            val ruleMetadataSykmelding =
                RuleMetadataSykmelding(
                    ruleMetadata = ruleMetadata,
                    sykmeldingMetadataInfo = sykmeldingMetadata,
                    doctorSuspensjon = doctorSuspendDeferred.await(),
                    behandlerOgStartdato =
                        if (behandler != null)
                            BehandlerOgStartdato(
                                behandler,
                                sykmeldingMetadata.startdato,
                            )
                        else
                            BehandlerOgStartdato(
                                Behandler(godkjenninger = emptyList(), hprNummer = null),
                                startdato = sykmeldingMetadata.startdato,
                            ),
                )

            // TODO: OLD EXECUTION ONLY FOR COMPARISON
            val oldResult =
                ruleExecutionService.runRules(receivedSykmelding.sykmelding, ruleMetadataSykmelding)

            // TODO: OLD EXECUTION ONLY FOR COMPARISON
            oldResult.forEach {
                RULE_NODE_RULE_PATH_COUNTER.labels(
                        it.printRulePath(),
                    )
                    .inc()
            }

            // TODO: OLD EXECUTION ONLY FOR COMPARISON
            val oldValidationResult = validationResult(oldResult.map { it })
            RULE_NODE_RULE_HIT_COUNTER.labels(
                    oldValidationResult.status.name,
                    oldValidationResult.ruleHits.firstOrNull()?.ruleName
                        ?: oldValidationResult.status.name,
                )
                .inc()

            val regulaResult =
                runRegula(
                    receivedSykmelding = receivedSykmelding,
                    ruleMetadataSykmelding = ruleMetadataSykmelding,
                    tidligereSykmeldinger = sykmeldingMetadata.sykmeldingerFraRegister,
                )
            juridiskVurderingService.processRuleResults(receivedSykmelding, regulaResult)

            if (regulaResult.status != RegulaStatus.OK) {
                secureLog.info(
                    "RuleResult for ${receivedSykmelding.sykmelding.id}: ${
                        objectMapper
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(regulaResult.results.filter { it.outcome?.status != RegulaStatus.OK })
                    }",
                )
            }

            compareNewVsOld(
                sykmeldingId = receivedSykmelding.sykmelding.id,
                newResult = regulaResult,
                oldResult = oldResult,
                oldValidationResult = oldValidationResult,
            )

            return regulaResult
        }

    private fun validationResult(
        results: List<TreeOutput<out Enum<*>, RuleResult>>
    ): ValidationResult =
        ValidationResult(
            status =
                results
                    .map { result -> result.treeResult.status }
                    .let {
                        it.firstOrNull { status -> status == Status.INVALID }
                            ?: it.firstOrNull { status -> status == Status.MANUAL_PROCESSING }
                                ?: Status.OK
                    },
            ruleHits =
                results
                    .mapNotNull { it.treeResult.ruleHit }
                    .map { result ->
                        RuleInfo(
                            result.rule,
                            result.messageForSender,
                            result.messageForUser,
                            result.status,
                        )
                    },
        )

    private fun erTilbakedatert(receivedSykmelding: ReceivedSykmelding): Boolean =
        receivedSykmelding.sykmelding.signaturDato
            .toLocalDate()
            .isAfter(receivedSykmelding.sykmelding.perioder.sortedFOMDate().first().plusDays(3))
}

data class BehandlerOgStartdato(
    val behandler: Behandler,
    val startdato: LocalDate?,
)

data class RuleMetadataSykmelding(
    val ruleMetadata: RuleMetadata,
    val sykmeldingMetadataInfo: SykmeldingMetadataInfo,
    val doctorSuspensjon: Boolean,
    val behandlerOgStartdato: BehandlerOgStartdato,
)

fun List<Periode>.sortedFOMDate(): List<LocalDate> = map { it.fom }.sorted()

fun List<Periode>.sortedTOMDate(): List<LocalDate> = map { it.tom }.sorted()

fun ClosedRange<LocalDate>.daysBetween(): Long = ChronoUnit.DAYS.between(start, endInclusive)
