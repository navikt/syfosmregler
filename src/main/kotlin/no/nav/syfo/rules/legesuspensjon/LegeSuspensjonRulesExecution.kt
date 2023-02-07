package no.nav.syfo.rules.legesuspensjon

import no.nav.syfo.log
import no.nav.syfo.rules.dsl.ResultNode
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.TreeNode
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.join
import no.nav.syfo.rules.dsl.printRulePath

typealias LegeSuspensjonTreeOutput = TreeOutput<LegeSuspensjonRules, LegeSuspensjonResult>
typealias LegeSuspensjonTreeNode = TreeNode<LegeSuspensjonRules, LegeSuspensjonResult>

class LegeSuspensjonRulesExecution(private val rootNode: LegeSuspensjonTreeNode = legeSuspensjonRuleTree) {
    fun runRules(sykmeldingId: String, behandlerSuspendert: Boolean): LegeSuspensjonTreeOutput =
        rootNode
            .evaluate(sykmeldingId, behandlerSuspendert)
            .also { legeSuspensjonRulePath ->
                log.info("Rules $sykmeldingId, ${legeSuspensjonRulePath.printRulePath()}")
            }
}

private fun TreeNode<LegeSuspensjonRules, LegeSuspensjonResult>.evaluate(
    sykmeldingId: String,
    behandlerSuspendert: Boolean,
): LegeSuspensjonTreeOutput =
    when (this) {
        is ResultNode -> LegeSuspensjonTreeOutput(treeResult = result)
        is RuleNode -> {
            val rule = getRule(rule)
            val result = rule(behandlerSuspendert)
            val childNode = if (result.ruleResult) yes else no
            result join childNode.evaluate(sykmeldingId, behandlerSuspendert)
        }
    }
