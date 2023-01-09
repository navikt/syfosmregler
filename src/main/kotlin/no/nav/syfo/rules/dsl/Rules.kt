package no.nav.syfo.rules.dsl

import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleMetadataSykmelding

typealias Rule<T> = (sykmelding: Sykmelding, metadata: RuleMetadataSykmelding) -> RuleResult<T>

data class RuleExecution<T>(
    val rule: T,
    val result: Boolean
)

data class RuleResult<T>(
    val ruleInputs: Map<String, Any> = emptyMap(),
    val ruleResult: RuleExecution<T>
)

data class TreeOutput<T>(
    val ruleInputs: Map<String, Any> = mapOf(),
    val rulePath: List<RuleExecution<T>> = emptyList(),
    val status: Status
)

infix fun <T> RuleResult<T>.join(rulesOutput: TreeOutput<T>) =
    TreeOutput(
        ruleInputs = ruleInputs + rulesOutput.ruleInputs,
        rulePath = listOf(ruleResult) + rulesOutput.rulePath,
        status = rulesOutput.status
    )

infix fun <T> Status.join(treeOutput: TreeOutput<T>) = TreeOutput(
    ruleInputs = treeOutput.ruleInputs,
    rulePath = treeOutput.rulePath,
    status = this
)
