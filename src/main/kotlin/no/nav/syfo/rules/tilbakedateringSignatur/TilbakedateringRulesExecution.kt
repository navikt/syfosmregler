package no.nav.syfo.rules.tilbakedateringSignatur

import no.nav.syfo.log
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.common.MedJuridisk
import no.nav.syfo.rules.common.RuleExecution
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.ResultNode
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.TreeNode
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.join
import no.nav.syfo.rules.dsl.printRulePath
import no.nav.syfo.rules.tilbakedatering.tilbakeDatertJuridiskHenvisning
import no.nav.syfo.services.RuleMetadataSykmelding

typealias TilbakedateringSignaturTreeOutput = TreeOutput<TilbakedateringSignaturRules, RuleResult>

typealias TilbakedateringSignaturTreeNode = TreeNode<TilbakedateringSignaturRules, RuleResult>

class TilbakedateringSignaturRulesExecution(
    private val rootNode: TilbakedateringSignaturTreeNode = tilbakedateringSignaturRuleTree
) : RuleExecution<TilbakedateringSignaturRules> {
    override fun runRules(sykmelding: Sykmelding, ruleMetadata: RuleMetadataSykmelding) =
        rootNode.evaluate(sykmelding, ruleMetadata).also { tilbakedateringRulePath ->
            log.info("Rules ${sykmelding.id}, ${tilbakedateringRulePath.printRulePath()}")
        } to MedJuridisk(tilbakeDatertJuridiskHenvisning())
}

private fun TreeNode<TilbakedateringSignaturRules, RuleResult>.evaluate(
    sykmelding: Sykmelding,
    metadata: RuleMetadataSykmelding,
): TilbakedateringSignaturTreeOutput =
    when (this) {
        is ResultNode -> {
            TilbakedateringSignaturTreeOutput(treeResult = result)
        }
        is RuleNode -> {
            val rule = getRule(rule)
            val result = rule(sykmelding, metadata)
            val childNode = if (result.ruleResult) yes else no
            result join childNode.evaluate(sykmelding, metadata)
        }
    }
