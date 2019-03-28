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
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.syfo.RULE_HIT_STATUS_COUNTER
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.executeFlow
import no.nav.syfo.model.Periode
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.model.Syketilfelle
import no.nav.syfo.model.SyketilfelleTag
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.retryAsync
import no.nav.syfo.rules.HPRRuleChain
import no.nav.syfo.rules.LegesuspensjonRuleChain
import no.nav.syfo.rules.PeriodLogicRuleChain
import no.nav.syfo.rules.PostTPSRuleChain
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import no.nhn.schemas.reg.hprv2.IHPR2Service
import no.nhn.schemas.reg.hprv2.IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person as TPSPerson
import no.nhn.schemas.reg.hprv2.Person as HPRPerson

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smregler")

val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
}

@KtorExperimentalAPI
fun Routing.registerRuleApi(personV3: PersonV3, helsepersonellv1: IHPR2Service, legeSuspensjonClient: LegeSuspensjonClient, syketilfelleClient: SyketilfelleClient) {
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
        ).flatten().executeFlow(receivedSykmelding.sykmelding, RuleMetadata(
                receivedDate = receivedSykmelding.mottattDato,
                signatureDate = receivedSykmelding.signaturDato,
                patientPersonNumber = receivedSykmelding.personNrPasient,
                rulesetVersion = receivedSykmelding.rulesetVersion,
                legekontorOrgnr = receivedSykmelding.legekontorOrgNr
        ))

        try {
            val doctor = fetchDoctor(helsepersonellv1, receivedSykmelding.personNrLege).await()

            val hprRuleResults = HPRRuleChain.values().executeFlow(receivedSykmelding.sykmelding, doctor)

            val patient = fetchPerson(personV3, receivedSykmelding.personNrPasient)
            val tpsRuleResults = PostTPSRuleChain.values().executeFlow(receivedSykmelding.sykmelding, patient.await())

            val signaturDatoString = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(receivedSykmelding.signaturDato)
            val doctorSuspend = legeSuspensjonClient.checkTherapist(receivedSykmelding.personNrLege, receivedSykmelding.navLogId, signaturDatoString).await().suspendert
            val doctorRuleResults = LegesuspensjonRuleChain.values().executeFlow(receivedSykmelding.sykmelding, doctorSuspend)

            /*val syketilfelle = syketilfelleClient.fetchSyketilfelle(
                    receivedSykmelding.sykmelding.perioder.intoSyketilfelle(
                            receivedSykmelding.sykmelding.pasientAktoerId, receivedSykmelding.mottattDato, receivedSykmelding.msgId),
                    receivedSykmelding.sykmelding.pasientAktoerId).await()
            val syketilfelleResults = SyketillfelleRuleChain.values().executeFlow(receivedSykmelding.sykmelding, syketilfelle)*/
            val results = listOf(
                    validationAndPeriodRuleResults,
                    tpsRuleResults,
                    hprRuleResults,
                    doctorRuleResults
            ).flatten()

            log.info("Rules hit {}, $logKeys", results.map { it.name }, *logValues)

            val validationResult = validationResult(results)
            RULE_HIT_STATUS_COUNTER.labels(validationResult.status.name).inc()
            call.respond(validationResult)
        } catch (e: IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage) {
            val validationResult = ValidationResult(
                    status = Status.INVALID,
                    ruleHits = listOf(RuleInfo(
                            ruleName = "BEHANDLER_NOT_IN_HPR",
                            textToTreater = "Behandler er ikkje register i HPR",
                            textToUser = "Behandler er ikkje register i HPR"))
            )
            call.respond(validationResult)
        }
    }
}

fun CoroutineScope.fetchDoctor(hprService: IHPR2Service, doctorIdent: String): Deferred<HPRPerson> =
        retryAsync("hpr_hent_person_med_personnummer", IOException::class, WstxException::class) {
            hprService.hentPersonMedPersonnummer(doctorIdent, datatypeFactory.newXMLGregorianCalendar(GregorianCalendar()))
        }

fun List<Periode>.intoSyketilfelle(aktoerId: String, received: LocalDateTime, resourceId: String): List<Syketilfelle> = map {
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

fun CoroutineScope.fetchPerson(personV3: PersonV3, ident: String): Deferred<TPSPerson> =
        retryAsync("tps_hent_person", IOException::class, WstxException::class) {
            personV3.hentPerson(HentPersonRequest()
                    .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(ident)))
            ).person
        }

fun validationResult(results: List<Rule<Any>>): ValidationResult =
        ValidationResult(
                status = results
                        .map { status -> status.status }.let {
                            it.firstOrNull { status -> status == Status.INVALID }
                            ?: it.firstOrNull { status -> status == Status.MANUAL_PROCESSING }
                            ?: Status.OK
                        },
                ruleHits = results.map { rule -> RuleInfo(rule.name, rule.textToUser, rule.textToTreater) }
        )
