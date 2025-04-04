package no.nav.syfo.rules

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.syfo.rules.shared.ReceivedSykmelding
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.RegulaStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smregler")

@DelicateCoroutinesApi
fun Route.registerRuleApi(ruleService: RuleService) {
    post("/v1/rules/validate") {
        log.info("Got a request to validate rules")

        val receivedSykmelding: ReceivedSykmelding = call.receive()
        val validationResult: RegulaResult = ruleService.executeRuleChains(receivedSykmelding)

        // TODO: Map to old result as to not change the API
        val oldResult =
            ValidationResponse(
                status =
                    when (validationResult.status) {
                        RegulaStatus.OK -> ValidationResponseStatus.OK
                        RegulaStatus.MANUAL_PROCESSING -> ValidationResponseStatus.MANUAL_PROCESSING
                        RegulaStatus.INVALID -> ValidationResponseStatus.INVALID
                    },
                ruleHits =
                    validationResult.outcome?.let {
                        listOf(
                            ValidationResponseRuleInfo(
                                ruleName = it.rule,
                                messageForSender = it.messageForSender,
                                messageForUser = it.messageForUser,
                                ruleStatus =
                                    when (it.status) {
                                        RegulaStatus.OK -> ValidationResponseStatus.OK
                                        RegulaStatus.MANUAL_PROCESSING ->
                                            ValidationResponseStatus.MANUAL_PROCESSING
                                        RegulaStatus.INVALID -> ValidationResponseStatus.INVALID
                                    },
                            ),
                        )
                    }
                        ?: emptyList(),
            )

        call.respond(oldResult)
    }
}
