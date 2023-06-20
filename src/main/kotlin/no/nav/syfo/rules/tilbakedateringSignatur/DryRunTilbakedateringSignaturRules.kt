package no.nav.syfo.rules.tilbakedateringSignatur

import no.nav.syfo.metrics.TILBAKEDATERING_SIGNATUR_RULE_HIT_COUNTER
import no.nav.syfo.metrics.TILBAKEDATERING_SIGNATUR_RULE_PATH_COUNTER
import no.nav.syfo.model.RuleInfo
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.rules.common.Juridisk
import no.nav.syfo.rules.common.RuleExecution
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.printRulePath
import no.nav.syfo.services.RuleMetadataSykmelding

private val ruleExecution =
    sequenceOf(
        TilbakedateringSignaturRulesExecution(tilbakedateringSignaturRuleTree),
    )

private fun runRules(
    sykmelding: Sykmelding,
    ruleMetadataSykmelding: RuleMetadataSykmelding,
    sequence: Sequence<RuleExecution<out Enum<*>>> = ruleExecution,
): List<Pair<TreeOutput<out Enum<*>, RuleResult>, Juridisk>> {
    var lastStatus = Status.OK
    val results =
        sequence
            .map { it.runRules(sykmelding, ruleMetadataSykmelding) }
            .takeWhile {
                if (lastStatus == Status.OK) {
                    lastStatus = it.first.treeResult.status
                    true
                } else {
                    false
                }
            }
    return results.toList()
}

fun dryRunTilbakedateringSignatur(
    sykmelding: Sykmelding,
    ruleMetadataSykmelding: RuleMetadataSykmelding,
) {
    val dryRuns = runRules(sykmelding, ruleMetadataSykmelding)
    dryRuns.forEach {
        TILBAKEDATERING_SIGNATUR_RULE_PATH_COUNTER.labels(
                it.first.printRulePath(),
            )
            .inc()
    }

    val validationResult = validationResult(dryRuns.map { it.first })
    TILBAKEDATERING_SIGNATUR_RULE_HIT_COUNTER.labels(
            validationResult.status.name,
            validationResult.ruleHits.firstOrNull()?.ruleName ?: validationResult.status.name,
        )
        .inc()
}

private fun validationResult(results: List<TreeOutput<out Enum<*>, RuleResult>>): ValidationResult =
    ValidationResult(
        status =
            results
                .map { result -> result.treeResult.status }
                .let {
                    it.firstOrNull { status -> status == Status.INVALID }
                        ?: it.firstOrNull { status -> status == Status.MANUAL_PROCESSING }
                            ?: Status.OK
                },
        ruleHits =
            results
                .mapNotNull { it.treeResult.ruleHit }
                .map { result ->
                    RuleInfo(
                        result.rule,
                        result.messageForSender,
                        result.messageForUser,
                        result.status,
                    )
                },
    )
