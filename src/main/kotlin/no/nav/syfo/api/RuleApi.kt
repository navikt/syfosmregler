package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.request.receiveStream
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.kith.xmlstds.msghead._2006_05_24.XMLIdent
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.kith.xmlstds.msghead._2006_05_24.XMLRefDoc
import no.nav.model.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.RULE_HIT_COUNTER
import no.nav.syfo.Rule
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import no.trygdeetaten.xml.eiff._1.XMLMottakenhetBlokk
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import no.nav.syfo.get
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.rules.HPRRuleChain
import no.nav.syfo.rules.PeriodLogicRuleChain
import no.nav.syfo.rules.RuleData
import no.nav.syfo.rules.RuleMetadata
import no.nav.syfo.rules.PostTPSRuleChain
import no.nav.syfo.rules.ValidationRuleChain
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person as TPSPerson
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personidenter
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import no.nhn.schemas.reg.hprv2.IHPR2Service
import java.util.GregorianCalendar
import javax.xml.bind.JAXBContext
import javax.xml.bind.Unmarshaller
import javax.xml.datatype.DatatypeFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smregler")
val fellesformatJaxBContext: JAXBContext = JAXBContext.newInstance(
        XMLEIFellesformat::class.java,
        XMLMsgHead::class.java,
        HelseOpplysningerArbeidsuforhet::class.java
)
val fellesformatUnmarshaller: Unmarshaller = fellesformatJaxBContext.createUnmarshaller()

val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

fun Routing.registerRuleApi(personV3: PersonV3, helsepersonellv1: IHPR2Service) {
    post("/v1/rules/validate") {
        log.info("Got an request to validate rules")
        val fellesformat = fellesformatUnmarshaller.unmarshal(call.receiveStream()) as XMLEIFellesformat

        val mottakenhetBlokk: XMLMottakenhetBlokk = fellesformat.get()
        val msgHead: XMLMsgHead = fellesformat.get()

        val doctorPersonnumber = extractDoctorIdentFromSignature(mottakenhetBlokk)

        // TODO this is not good
        var doctor = no.nhn.schemas.reg.hprv2.Person()

        try {
            doctor = helsepersonellv1.hentPersonMedPersonnummer(doctorPersonnumber, datatypeFactory.newXMLGregorianCalendar(GregorianCalendar()))
        } catch (e: HentPersonPersonIkkeFunnet) {
            log.error("Pasient ikkje funnet i TPS")
        }

        var patientTPS = TPSPerson()

        val patientIdent = extractPatientIdent(msgHead)

        if (patientIdent?.id != null && patientIdent.typeId?.v != null) {
            try {
                patientTPS = hentPersonResponse(personV3, patientIdent)
            } catch (e: HentPersonPersonIkkeFunnet) {
                log.error("Pasient ikkje funnet i TPS")
            }
        }

        val logValues = arrayOf(
                keyValue("smId", mottakenhetBlokk.ediLoggId),
                keyValue("organizationNumber", extractOrganisationNumberFromSender(fellesformat)?.id),
                keyValue("msgId", msgHead.msgInfo.msgId)
        )

        val logKeys = logValues.joinToString(prefix = "(", postfix = ")", separator = ",") {
            "{}"
        }

        log.info("Received a SM2013, going to rules, $logKeys", *logValues)
        val ruleData = RuleData.fromFellesformat(fellesformat, patientTPS, doctorPersonnumber, doctor)
        val results = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                ValidationRuleChain.values().toList(),
                PeriodLogicRuleChain.values().toList(),
                PostTPSRuleChain.values().toList(),
                HPRRuleChain.values().toList()

        ).flatten().filter { rule -> rule.predicate(ruleData) }.onEach { RULE_HIT_COUNTER.labels(it.name).inc() }

        call.respond(ValidationResult(
                status = results.map { it.status }.firstOrNull { it == Status.INVALID } ?: Status.OK,
                ruleHits = results.map { RuleInfo(it.name) }
        ))
    }
}

inline fun <reified T> XMLEIFellesformat.get() = this.any.find { it is T } as T
inline fun <reified T> XMLRefDoc.Content.get() = this.any.find { it is T } as T

fun extractOrganisationNumberFromSender(fellesformat: XMLEIFellesformat): XMLIdent? =
        fellesformat.get<XMLMsgHead>().msgInfo.sender.organisation.ident.find {
            it.typeId.v == "ENH"
        }
fun extractHelseopplysninger(msgHead: XMLMsgHead) = msgHead.document[0].refDoc.content.get<HelseOpplysningerArbeidsuforhet>()

fun hentPersonResponse(personV3: PersonV3, ident: XMLIdent): Person =
        personV3.hentPerson(HentPersonRequest()
                .withAktoer(PersonIdent().withIdent(
                        NorskIdent()
                                .withIdent(ident.id)
                                .withType(Personidenter().withValue(ident.typeId?.v))))).person

fun extractPatientIdent(msgHead: XMLMsgHead): XMLIdent? =
        msgHead.msgInfo.patient?.ident?.find {
            it.typeId.v == "FNR"
        } ?: msgHead.msgInfo.patient?.ident?.find {
            it.typeId.v == "DNR"
        }

fun extractDoctorIdentFromSignature(mottakenhetBlokk: XMLMottakenhetBlokk): String =
        mottakenhetBlokk.avsenderFnrFraDigSignatur
