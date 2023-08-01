package no.nav.syfo.rules.arbeidsuforhet

import no.nav.syfo.logger
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.common.RuleExecution
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.common.UtenJuridisk
import no.nav.syfo.rules.dsl.ResultNode
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.TreeNode
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.join
import no.nav.syfo.rules.dsl.printRulePath
import no.nav.syfo.services.RuleMetadataSykmelding

typealias ArbeidsuforhetTreeOutput = TreeOutput<ArbeidsuforhetRules, RuleResult>

typealias ArbeidsuforhetTreeNode = TreeNode<ArbeidsuforhetRules, RuleResult>

class ArbeidsuforhetRulesExecution(
    private val rootNode: ArbeidsuforhetTreeNode = arbeidsuforhetRuleTree
) : RuleExecution<ArbeidsuforhetRules> {
    override fun runRules(sykmelding: Sykmelding, ruleMetadata: RuleMetadataSykmelding) =
        rootNode.evaluate(sykmelding, ruleMetadata.ruleMetadata).also { validationRulePath ->
            logger.info("Rules ${sykmelding.id}, ${validationRulePath.printRulePath()}")
        } to UtenJuridisk
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
