package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.log
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.ResultNode
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.TreeNode
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.join
import no.nav.syfo.rules.dsl.printRulePath
import no.nav.syfo.services.RuleMetadataSykmelding

typealias TilbakedateringTreeOutput = TreeOutput<TilbakedateringRules, TilbakedateringResult>
typealias TilbakedateringTreeNode = TreeNode<TilbakedateringRules, TilbakedateringResult>

class TilbakedateringRulesExecution(private val rootNode: TilbakedateringTreeNode = tilbakedateringRuleTree) {
    fun runRules(sykmelding: Sykmelding, metadata: RuleMetadataSykmelding): TilbakedateringTreeOutput =
        rootNode
            .evaluate(sykmelding, metadata)
            .also { tilbakedateringRulePath ->
                log.info("Rules ${sykmelding.id}, ${tilbakedateringRulePath.printRulePath()}")
            }
}

private fun TreeNode<TilbakedateringRules, TilbakedateringResult>.evaluate(
    sykmelding: Sykmelding,
    metadata: RuleMetadataSykmelding,
): TilbakedateringTreeOutput =
    when (this) {
        is ResultNode -> TilbakedateringTreeOutput(treeResult = result)
        is RuleNode -> {
            val rule = getRule(rule)
            val result = rule(sykmelding, metadata)
            val childNode = if (result.ruleResult) yes else no
            result join childNode.evaluate(sykmelding, metadata)
        }
    }
