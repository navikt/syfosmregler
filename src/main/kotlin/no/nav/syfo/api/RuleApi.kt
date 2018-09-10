package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import net.logstash.logback.marker.LogstashMarker
import net.logstash.logback.marker.Markers
import no.kith.xmlstds.msghead._2006_05_24.XMLIdent
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import no.trygdeetaten.xml.eiff._1.XMLMottakenhetBlokk
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.bind.Unmarshaller

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smregler")
val fellesformatJaxBContext: JAXBContext = JAXBContext.newInstance(XMLEIFellesformat::class.java, XMLMsgHead::class.java)
val fellesformatUnmarshaller: Unmarshaller = fellesformatJaxBContext.createUnmarshaller()

fun Routing.registerRuleApi() {
    post("/v1/rules/validate") {
        log.info("Got an request to validate rules")
        val text = call.receiveText()
        if (log.isDebugEnabled) {
            log.debug(text)
        }
        val fellesformat = fellesformatUnmarshaller.unmarshal(StringReader(text)) as XMLEIFellesformat
        val marker = Markers.append("msgId", fellesformat.get<XMLMsgHead>().msgInfo.msgId)
                .and<LogstashMarker>(Markers.append("organisationNumber", extractOrganisationNumberFromSender(fellesformat)?.id))
                .and<LogstashMarker>(Markers.append("smId", fellesformat.get<XMLMottakenhetBlokk>().ediLoggId))
        log.info(marker, "Received a SM2013, going to rules")

        call.respond(ValidationResult(
                status = when {
                    text.contains("TMP_MANUAL") -> Status.MANUAL_PROCESSING
                    text.contains("TMP_INVALID") -> Status.INVALID
                    else -> Status.OK
                },
                ruleHits = listOf()
        ))
    }
}

inline fun <reified T> XMLEIFellesformat.get() = this.any.find { it is T } as T

fun extractOrganisationNumberFromSender(fellesformat: XMLEIFellesformat): XMLIdent? =
        fellesformat.get<XMLMsgHead>().msgInfo.sender.organisation.ident.find {
            it.typeId.v == "ENH"
        }