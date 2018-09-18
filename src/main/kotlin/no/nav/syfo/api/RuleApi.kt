package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.request.receiveStream
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.kith.xmlstds.msghead._2006_05_24.XMLIdent
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import no.trygdeetaten.xml.eiff._1.XMLMottakenhetBlokk
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import no.nav.syfo.get
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.rules.fellesformatValidationChain
import no.nav.syfo.rules.validationChain
import javax.xml.bind.JAXBContext
import javax.xml.bind.Unmarshaller

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smregler")
val fellesformatJaxBContext: JAXBContext = JAXBContext.newInstance(XMLEIFellesformat::class.java, XMLMsgHead::class.java)
val fellesformatUnmarshaller: Unmarshaller = fellesformatJaxBContext.createUnmarshaller()

fun Routing.registerRuleApi() {
    post("/v1/rules/validate") {
        log.info("Got an request to validate rules")
        val fellesformat = fellesformatUnmarshaller.unmarshal(call.receiveStream()) as XMLEIFellesformat
        val logValues = arrayOf(
                keyValue("smId", fellesformat.get<XMLMottakenhetBlokk>().ediLoggId),
                keyValue("organizationNumber", extractOrganisationNumberFromSender(fellesformat)?.id),
                keyValue("msgId", fellesformat.get<XMLMsgHead>().msgInfo.msgId)
        )

        val logKeys = logValues.joinToString(prefix = "(", postfix = ")", separator = ",") {
            "{}"
        }

        log.info("Received a SM2013, going to rules, $logKeys", *logValues)
        val results = listOf(
                fellesformatValidationChain.executeFlow(fellesformat),
                validationChain.executeFlow(fellesformat.get())
        ).flatMap { it }

        call.respond(ValidationResult(
                status = results.map { it.outcomeType.status }.firstOrNull { it == Status.INVALID } ?: Status.OK,
                ruleHits = results.map { RuleInfo(it.description) }
        ))
    }
}

inline fun <reified T> XMLEIFellesformat.get() = this.any.find { it is T } as T

fun extractOrganisationNumberFromSender(fellesformat: XMLEIFellesformat): XMLIdent? =
        fellesformat.get<XMLMsgHead>().msgInfo.sender.organisation.ident.find {
            it.typeId.v == "ENH"
        }
