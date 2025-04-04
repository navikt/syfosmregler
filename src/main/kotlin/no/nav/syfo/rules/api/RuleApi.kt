package no.nav.syfo.rules.api

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.Status
import no.nav.syfo.services.RuleService
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.regulus.regula.executor.ExecutionMode
import org.slf4j.LoggerFactory

@DelicateCoroutinesApi
fun Route.registerRuleApi(ruleService: RuleService) {
    val log = LoggerFactory.getLogger(this::class.java)

    post("/v1/rules/validate") {
        log.info("Got a request to validate rules")

        val receivedSykmelding: ReceivedSykmelding = call.receive()
        val validationResult: RegulaResult =
            ruleService.executeRuleChains(receivedSykmelding, ExecutionMode.NORMAL)
        val apiResponse: RuleValidationResponse = validationResult.toApiResponse()

        call.respond(apiResponse)
    }

    post("/v1/rules/validate/papir") {
        log.info("Got a request to validate (papir) rules")

        val receivedSykmelding: ReceivedSykmelding = call.receive()
        val validationResult: RegulaResult =
            ruleService.executeRuleChains(receivedSykmelding, ExecutionMode.PAPIR)
        val apiResponse: RuleValidationResponse = validationResult.toApiResponse()

        call.respond(apiResponse)
    }
}

private fun RegulaResult.toApiResponse(): RuleValidationResponse {
    val apiResponseStatus =
        when (this.status) {
            RegulaStatus.OK -> RuleValidationRuleStatus.OK
            RegulaStatus.MANUAL_PROCESSING -> RuleValidationRuleStatus.MANUAL_PROCESSING
            RegulaStatus.INVALID -> RuleValidationRuleStatus.INVALID
        }
    val apiResponseRuleHits =
        this.outcome?.let {
            listOf(
                RuleValidationRuleInfo(
                    ruleName = it.rule,
                    messageForSender = it.messageForSender,
                    messageForUser = it.messageForUser,
                    ruleStatus =
                        when (it.status) {
                            RegulaStatus.OK -> Status.OK
                            RegulaStatus.MANUAL_PROCESSING -> Status.MANUAL_PROCESSING
                            RegulaStatus.INVALID -> Status.INVALID
                        },
                ),
            )
        }
            ?: emptyList()

    return RuleValidationResponse(
        status = apiResponseStatus,
        ruleHits = apiResponseRuleHits,
    )
}

/** These data classes are used to keep the API stable. */
data class RuleValidationResponse(
    val status: RuleValidationRuleStatus,
    val ruleHits: List<RuleValidationRuleInfo>
)

data class RuleValidationRuleInfo(
    val ruleName: String,
    val messageForSender: String,
    val messageForUser: String,
    val ruleStatus: Status
)

enum class RuleValidationRuleStatus {
    OK,
    MANUAL_PROCESSING,
    INVALID
}
