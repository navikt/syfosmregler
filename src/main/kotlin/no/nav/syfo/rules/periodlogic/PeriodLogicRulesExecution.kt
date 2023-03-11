package no.nav.syfo.rules.periodlogic

import no.nav.syfo.log
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

typealias PeriodLogicTreeOutput = TreeOutput<PeriodLogicRules, RuleResult>
typealias PeriodLogicTreeNode = TreeNode<PeriodLogicRules, RuleResult>

class PeriodLogicRulesExecution(private val rootNode: TreeNode<PeriodLogicRules, RuleResult> = periodLogicRuleTree) :
    RuleExecution<PeriodLogicRules> {
    override fun runRules(sykmelding: Sykmelding, ruleMetadata: RuleMetadataSykmelding) =
        rootNode
            .evaluate(sykmelding, ruleMetadata)
            .also { periodLogicRulePath ->
                log.info("Rules ${sykmelding.id}, ${periodLogicRulePath.printRulePath()}")
            } to UtenJuridisk
}

private fun TreeNode<PeriodLogicRules, RuleResult>.evaluate(
    sykmelding: Sykmelding,
    ruleMetadata: RuleMetadataSykmelding
): PeriodLogicTreeOutput =
    when (this) {
        is ResultNode -> PeriodLogicTreeOutput(treeResult = result)
        is RuleNode -> {
            val rule = getRule(rule)
            val result = rule(sykmelding, ruleMetadata.ruleMetadata)
            val childNode = if (result.ruleResult) yes else no
            result join childNode.evaluate(sykmelding, ruleMetadata)
        }
    }
