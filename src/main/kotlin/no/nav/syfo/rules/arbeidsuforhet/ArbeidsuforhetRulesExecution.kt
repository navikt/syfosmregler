package no.nav.syfo.rules.arbeidsuforhet

import no.nav.syfo.log
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.ResultNode
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.TreeNode
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.join
import no.nav.syfo.rules.dsl.printRulePath

typealias ArbeidsuforhetTreeOutput = TreeOutput<ArbeidsuforhetRules, RuleResult>
typealias ArbeidsuforhetTreeNode = TreeNode<ArbeidsuforhetRules, RuleResult>

class ArbeidsuforhetRulesExecution(private val rootNode: ArbeidsuforhetTreeNode = arbeidsuforhetRuleTree) {
    fun runRules(sykmelding: Sykmelding, ruleMetadata: RuleMetadata): ArbeidsuforhetTreeOutput =
        rootNode
            .evaluate(sykmelding, ruleMetadata)
            .also { validationRulePath ->
                log.info("Rules ${sykmelding.id}, ${validationRulePath.printRulePath()}")
            }
}

private fun TreeNode<ArbeidsuforhetRules, RuleResult>.evaluate(
    sykmelding: Sykmelding,
    ruleMetadata: RuleMetadata,
): ArbeidsuforhetTreeOutput =
    when (this) {
        is ResultNode -> ArbeidsuforhetTreeOutput(treeResult = result)
        is RuleNode -> {
            val rule = getRule(rule)
            val result = rule(sykmelding, ruleMetadata)
            val childNode = if (result.ruleResult) yes else no
            result join childNode.evaluate(sykmelding, ruleMetadata)
        }
    }
