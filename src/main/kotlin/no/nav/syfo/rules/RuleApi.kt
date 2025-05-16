package no.nav.syfo.rules

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.syfo.rules.shared.ReceivedSykmelding
import no.nav.tsm.regulus.regula.RegulaOutcomeStatus
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
        when (this) {
            is RegulaResult.Ok -> emptyList()
            is RegulaResult.NotOk ->
                listOf(
                    RuleValidationRuleInfo(
                        ruleName = this.outcome.rule,
                        messageForSender = this.outcome.reason.sykmelder,
                        messageForUser = this.outcome.reason.sykmeldt,
                        ruleStatus =
                            when (this.outcome.status) {
                                RegulaOutcomeStatus.MANUAL_PROCESSING ->
                                    RuleValidationRuleStatus.MANUAL_PROCESSING
                                RegulaOutcomeStatus.INVALID -> RuleValidationRuleStatus.INVALID
                            },
                    ),
                )
        }

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
    val ruleStatus: RuleValidationRuleStatus
)

enum class RuleValidationRuleStatus {
    OK,
    MANUAL_PROCESSING,
    INVALID
}
