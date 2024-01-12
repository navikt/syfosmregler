package no.nav.syfo.rules.gradert

import no.nav.syfo.logger
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.common.Juridisk
import no.nav.syfo.rules.common.RuleExecution
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.ResultNode
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.TreeNode
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.join
import no.nav.syfo.rules.dsl.printRulePath
import no.nav.syfo.services.RuleMetadataSykmelding

typealias GradertTreeOutput = TreeOutput<GradertRules, RuleResult>

typealias GradertTreeNode = Pair<TreeNode<GradertRules, RuleResult>, Juridisk>

class GradertRulesExecution(val rootNode: GradertTreeNode = gradertRuleTree) :
    RuleExecution<GradertRules> {
    override fun runRules(
        sykmelding: Sykmelding,
        ruleMetadata: RuleMetadataSykmelding
    ): Pair<GradertTreeOutput, Juridisk> =
        rootNode.first.evaluate(sykmelding, ruleMetadata).also { gradertRulePath ->
            logger.info("Rules ${sykmelding.id}, ${gradertRulePath.printRulePath()}")
        } to rootNode.second
}

private fun TreeNode<GradertRules, RuleResult>.evaluate(
    sykmelding: Sykmelding,
    ruleMetadata: RuleMetadataSykmelding,
): GradertTreeOutput =
    when (this) {
        is ResultNode -> GradertTreeOutput(treeResult = result)
        is RuleNode -> {
            val rule = getRule(rule)
            val result = rule(sykmelding, ruleMetadata)
            val childNode = if (result.ruleResult) yes else no
            result join childNode.evaluate(sykmelding, ruleMetadata)
        }
    }
