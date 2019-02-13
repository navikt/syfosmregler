package no.nav.syfo.api

import com.ctc.wstx.exc.WstxEOFException
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
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.executeFlow
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.model.Status
import no.nav.syfo.model.Syketilfelle
import no.nav.syfo.model.SyketilfelleTag
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.retryAsync
import no.nav.syfo.rules.HPRRuleChain
import no.nav.syfo.rules.PeriodLogicRuleChain
import no.nav.syfo.rules.PostTPSRuleChain
import no.nav.syfo.rules.RuleMetadata
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import no.nhn.schemas.reg.hprv2.IHPR2Service
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.LocalDateTime
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
                keyValue("smId", receivedSykmelding.navLogId),
                keyValue("organizationNumber", receivedSykmelding.legekontorOrgNr),
                keyValue("msgId", receivedSykmelding.msgId)
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
                signatureDate = receivedSykmelding.signaturDato
        ))

        // TODO no.nhn.schemas.reg.hprv2.IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage: ArgumentException: Personnummer ikke funnet
        // add rule 1401 when this happens
        val doctor = fetchDoctor(helsepersonellv1, receivedSykmelding.personNrLege).await()
        val hprRuleResults = HPRRuleChain.values().executeFlow(receivedSykmelding.sykmelding, doctor)

        val patient = fetchPerson(personV3, receivedSykmelding.personNrPasient)
        val tpsRuleResults = PostTPSRuleChain.values().executeFlow(receivedSykmelding.sykmelding, patient.await())

        // TODO remove after api i ready in prod
        // val syketilfelle = syketilfelleClient.fetchSyketilfelle(receivedSykmelding.sykmelding.aktivitet.periode.intoSyketilfelle(receivedSykmelding.aktoerIdPasient, receivedSykmelding.mottattDato, receivedSykmelding.msgId), receivedSykmelding.aktoerIdPasient)

        // TODO remove after api i ready in prod https://jira.adeo.no/browse/REG-1397
        // val signaturDatoString = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(receivedSykmelding.signaturDato)
        // val doctorSuspend = legeSuspensjonClient.checkTherapist(receivedSykmelding.personNrLege, receivedSykmelding.navLogId, signaturDatoString).suspendert
        // val doctorRuleResults = LegesuspensjonRuleChain.values().executeFlow(receivedSykmelding.sykmelding, doctorSuspend)
        // val results = listOf(validationAndPeriodRuleResults, tpsRuleResults, hprRuleResults, doctorRuleResults).flatten()
        val results = listOf(validationAndPeriodRuleResults, tpsRuleResults, hprRuleResults).flatten()
        log.info("Rules hit {}, $logKeys", results.map { it.name }, *logValues)

        call.respond(ValidationResult(
                status = results
                        .map { status -> status.status }
                        .firstOrNull { status -> status == Status.INVALID } ?: Status.OK,
                ruleHits = results.map { rule -> RuleInfo(rule.name) }
        ))
    }
}

fun CoroutineScope.fetchDoctor(hprService: IHPR2Service, doctorIdent: String): Deferred<HPRPerson> =
        retryAsync("hpr_hent_person_med_personnummer", IOException::class, WstxEOFException::class) {
            hprService.hentPersonMedPersonnummer(doctorIdent, datatypeFactory.newXMLGregorianCalendar(GregorianCalendar()))
        }

fun List<HelseOpplysningerArbeidsuforhet.Aktivitet.Periode>.intoSyketilfelle(aktoerId: String, received: LocalDateTime, resourceId: String): List<Syketilfelle> = map {
    Syketilfelle(
            aktorId = aktoerId,
            orgnummer = null,
            inntruffet = received,
            tags = listOf(SyketilfelleTag.SYKMELDING, SyketilfelleTag.PERIODE, when {
                it.aktivitetIkkeMulig != null -> SyketilfelleTag.INGEN_AKTIVITET
                it.isReisetilskudd == true -> SyketilfelleTag.INGEN_AKTIVITET
                it.gradertSykmelding != null -> SyketilfelleTag.GRADERT_AKTIVITET
                it.behandlingsdager != null -> SyketilfelleTag.BEHANDLINGSDAGER
                it.avventendeSykmelding != null -> SyketilfelleTag.FULL_AKTIVITET
                else -> throw RuntimeException("Could not find aktivitetstype, this should never happen")
            }).joinToString(",") { tag -> tag.name },
            ressursId = resourceId,
            fom = it.periodeFOMDato.atStartOfDay(),
            tom = it.periodeTOMDato.atStartOfDay()
    )
}

fun CoroutineScope.fetchPerson(personV3: PersonV3, ident: String): Deferred<TPSPerson> =
        retryAsync("tps_hent_person", IOException::class, WstxEOFException::class) {
            personV3.hentPerson(HentPersonRequest()
                    .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(ident)))
            ).person
        }
