package no.nav.syfo.rules.dsl

import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleMetadataSykmelding

typealias Rule<T> = (sykmelding: Sykmelding, metadata: RuleMetadataSykmelding) -> RuleResult<T>

data class RuleResult<T>(
    val ruleInputs: Map<String, Any> = emptyMap(),
    val ruleResult: Boolean,
    val rule: T,
)

data class TreeOutput<T>(
    val ruleInputs: Map<String, Any> = mapOf(),
    val rulePath: List<RuleResult<T>> = emptyList(),
    val status: Status
)

fun <T> TreeOutput<T>.printRulePath() {
    rulePath.joinToString(separator = "->") { "${it.rule}(${if (it.ruleResult) "yes" else "no"})" }
        .plus("->$status")
}
infix fun <T> RuleResult<T>.join(rulesOutput: TreeOutput<T>) = TreeOutput(
    ruleInputs = ruleInputs + rulesOutput.ruleInputs,
    rulePath = listOf(this) + rulesOutput.rulePath,
    status = rulesOutput.status
)
