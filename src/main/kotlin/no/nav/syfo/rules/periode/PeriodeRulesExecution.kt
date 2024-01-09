package no.nav.syfo.rules.periode

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

typealias PeriodeRuleTreeOutput = TreeOutput<PeriodeRules, RuleResult>

typealias PeriodeRuleNode = Pair<TreeNode<PeriodeRules, RuleResult>, Juridisk>

class PeriodeRulesExecution(private val rootNode: PeriodeRuleNode = periodeRuleTree) :
    RuleExecution<PeriodeRules> {
    override fun runRules(sykmelding: Sykmelding, ruleMetadata: RuleMetadataSykmelding) =
        rootNode.first.evaluate(sykmelding, ruleMetadata).also {
            logger.info("Rules ${sykmelding.id}, ${it.printRulePath()}")
        } to rootNode.second

    private fun TreeNode<PeriodeRules, RuleResult>.evaluate(
        sykmelding: Sykmelding,
        ruleMetadata: RuleMetadataSykmelding,
    ): PeriodeRuleTreeOutput =
        when (this) {
            is ResultNode -> PeriodeRuleTreeOutput(treeResult = result)
            is RuleNode -> {
                val rule = getRule(rule)
                val result = rule(sykmelding, ruleMetadata.ruleMetadata)
                val childNode = if (result.ruleResult) yes else no
                result join childNode.evaluate(sykmelding, ruleMetadata)
            }
        }
}
