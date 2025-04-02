package no.nav.syfo.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.client.LegeSuspensjonClient
import no.nav.syfo.client.NorskHelsenettClient
import no.nav.syfo.client.SmregisterClient
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.objectMapper
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.secureLog
import no.nav.syfo.utils.LoggingMeta
import no.nav.syfo.validation.extractBornDate
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.regulus.regula.executeRegulaRules
import no.nav.tsm.regulus.regula.executor.ExecutionMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RuleService(
    private val legeSuspensjonClient: LegeSuspensjonClient,
    private val norskHelsenettClient: NorskHelsenettClient,
    private val pdlService: PdlPersonService,
    private val juridiskVurderingService: JuridiskVurderingService,
    private val syfosmregisterClient: SmregisterClient
) {
    private val log: Logger = LoggerFactory.getLogger("ruleservice")

    @DelicateCoroutinesApi
    suspend fun executeRuleChains(
        receivedSykmelding: ReceivedSykmelding,
        mode: ExecutionMode,
    ): RegulaResult =
        with(GlobalScope) {
            val loggingMeta =
                LoggingMeta(
                    mottakId = receivedSykmelding.navLogId,
                    orgNr = receivedSykmelding.legekontorOrgNr,
                    msgId = receivedSykmelding.msgId,
                    sykmeldingId = receivedSykmelding.sykmelding.id,
                )

            log.info("Received a SM2013, going to rules, {}", fields(loggingMeta))

            // PDL PASIENT: Trengs for regula
            val pdlPerson = pdlService.getPdlPerson(receivedSykmelding.personNrPasient, loggingMeta)
            val fodsel = pdlPerson.foedselsdato?.firstOrNull()
            val fodselsdato =
                if (fodsel?.foedselsdato?.isNotEmpty() == true) {
                    log.info("Extracting fodeseldato from PDL date")
                    LocalDate.parse(fodsel.foedselsdato)
                } else {
                    log.info("Extracting fodeseldato from personNrPasient")
                    // TODO: Dette må inn i regula
                    extractBornDate(receivedSykmelding.personNrPasient)
                }

            // Metadata, mye her kan nukes:
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

            // BTSYS: Trengs for regula
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

            // HPR Behandler: Trengs for regula
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

            // Tidligere fra register: Trengs for regula
            val sykmeldingerFromRegister =
                syfosmregisterClient.getSykmeldinger(receivedSykmelding.personNrPasient)

            val regulaPayload =
                mapToRegulaPayload(
                    sykmelding = receivedSykmelding.sykmelding,
                    pasientIdent = receivedSykmelding.personNrPasient,
                    tidligereSykmeldinger = sykmeldingerFromRegister,
                    ruleMetadata = ruleMetadata,
                    behandler = behandler,
                    behandlerSuspendert = doctorSuspendDeferred.await(),
                )
            val regulaResult = executeRegulaRules(regulaPayload, mode)
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

            return regulaResult
        }
}
