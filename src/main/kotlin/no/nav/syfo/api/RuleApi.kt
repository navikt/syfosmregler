package no.nav.syfo.api

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.executeFlow
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.model.Syketilfelle
import no.nav.syfo.model.SyketilfelleTag
import no.nav.syfo.rules.HPRRuleChain
import no.nav.syfo.rules.PeriodLogicRuleChain
import no.nav.syfo.rules.RuleMetadata
import no.nav.syfo.rules.PostTPSRuleChain
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person as TPSPerson
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import no.nhn.schemas.reg.hprv2.IHPR2Service
import java.time.LocalDateTime
import no.nhn.schemas.reg.hprv2.Person as HPRPerson
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smregler")

val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

fun Routing.registerRuleApi(personV3: PersonV3, helsepersonellv1: IHPR2Service) {
    post("/v1/rules/validate") {
        log.info("Got an request to validate rules")

        val receivedSykmelding: ReceivedSykmelding = call.receive()

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

        val syketilfelle = fetchSyketilfelle(receivedSykmelding.sykmelding.aktivitet.periode.intoSyketilfelle(receivedSykmelding.aktoerIdPasient, receivedSykmelding.mottattDato, receivedSykmelding.msgId))

        val doctor = fetchDoctor(helsepersonellv1, receivedSykmelding.personNrLege)
        val hprRuleResults = HPRRuleChain.values().executeFlow(receivedSykmelding.sykmelding, doctor.await())

        val patient = fetchPerson(personV3, receivedSykmelding.personNrPasient)
        val tpsRuleResults = PostTPSRuleChain.values().executeFlow(receivedSykmelding.sykmelding, patient.await())

        val results = listOf(validationAndPeriodRuleResults, tpsRuleResults, hprRuleResults).flatten()

        call.respond(ValidationResult(
                status = results
                        .map { status -> status.status }
                        .firstOrNull { status -> status == Status.INVALID } ?: Status.OK,
                ruleHits = results.map { rule -> RuleInfo(rule.name) }
        ))
    }
}

fun CoroutineScope.fetchDoctor(hprService: IHPR2Service, doctorIdent: String): Deferred<HPRPerson> = async {
    hprService.hentPersonMedPersonnummer(doctorIdent, datatypeFactory.newXMLGregorianCalendar(GregorianCalendar()))
}

val httpClient = HttpClient(CIO) {
    install(JsonFeature) {
        serializer = JacksonSerializer {
            registerModule(JavaTimeModule())
        }
    }
}

fun CoroutineScope.fetchSyketilfelle(input: List<Syketilfelle>): Deferred<Any> = async {
    httpClient.post<Any>("/") {
        accept(ContentType.Application.Json)
        contentType(ContentType.Application.Json)
        body = input
    }
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

fun CoroutineScope.fetchPerson(personV3: PersonV3, ident: String): Deferred<TPSPerson> = async {
        personV3.hentPerson(HentPersonRequest()
                .withAktoer(PersonIdent().withIdent(NorskIdent()
                                .withIdent(ident)
                ) // .withType(Personidenter().withValue("FNR"))) // TODO?
                )
        ).person
}
