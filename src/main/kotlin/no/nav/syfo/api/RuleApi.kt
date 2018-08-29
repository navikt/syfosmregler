package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smregler")

fun Routing.registerRuleApi() {
    post("/v1/rules/validate") {
        log.info("Got an request to validate rules")
        val text = call.receiveText()
        if (log.isDebugEnabled) {
            log.debug(text)
        }
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
