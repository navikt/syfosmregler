package no.nav.syfo.rules.periodlogic

import no.nav.syfo.log
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.ResultNode
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.TreeNode
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.join
import no.nav.syfo.rules.dsl.printRulePath

typealias PeriodLogicTreeOutput = TreeOutput<PeriodLogicRules, PeriodLogicResult>
typealias PeriodLogicTreeNode = TreeNode<PeriodLogicRules, PeriodLogicResult>

class PeriodLogicRulesExecution(private val rootNode: PeriodLogicTreeNode = periodLogicRuleTree) {
    fun runRules(sykmelding: Sykmelding, ruleMetadata: RuleMetadata): PeriodLogicTreeOutput =
        rootNode
            .evaluate(sykmelding, ruleMetadata)
            .also { periodLogicRulePath ->
                log.info("Rules ${sykmelding.id}, ${periodLogicRulePath.printRulePath()}")
            }
}

private fun TreeNode<PeriodLogicRules, PeriodLogicResult>.evaluate(
    sykmelding: Sykmelding,
    ruleMetadata: RuleMetadata,
): PeriodLogicTreeOutput =
    when (this) {
        is ResultNode -> PeriodLogicTreeOutput(treeResult = result)
        is RuleNode -> {
            val rule = getRule(rule)
            val result = rule(sykmelding, ruleMetadata)
            val childNode = if (result.ruleResult) yes else no
            result join childNode.evaluate(sykmelding, ruleMetadata)
        }
    }
