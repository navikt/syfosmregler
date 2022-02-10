package no.nav.syfo.api

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.syfo.metrics.RULE_HIT_STATUS_COUNTER
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.services.RuleService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smregler")

@DelicateCoroutinesApi
fun Route.registerRuleApi(ruleService: RuleService) {
    post("/v1/rules/validate") {
        log.info("Got a request to validate rules")

        val receivedSykmelding: ReceivedSykmelding = call.receive()
        val validationResult: ValidationResult = ruleService.executeRuleChains(receivedSykmelding)

        RULE_HIT_STATUS_COUNTER.labels(validationResult.status.name).inc()
        call.respond(validationResult)
    }
}
