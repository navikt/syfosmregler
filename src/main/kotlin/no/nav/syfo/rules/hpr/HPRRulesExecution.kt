package no.nav.syfo.rules.hpr

import no.nav.syfo.log
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.BehandlerOgStartdato
import no.nav.syfo.rules.dsl.ResultNode
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.TreeNode
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.join
import no.nav.syfo.rules.dsl.printRulePath

typealias HPRTreeOutput = TreeOutput<HPRRules, HPRResult>
typealias HPRTreeNode = TreeNode<HPRRules, HPRResult>

class HPRRulesExecution(private val rootNode: HPRTreeNode = hprRuleTree) {
    fun runRules(sykmelding: Sykmelding, behandlerOgStartdato: BehandlerOgStartdato): HPRTreeOutput =
        rootNode
            .evaluate(sykmelding, behandlerOgStartdato)
            .also { hprRulePath ->
                log.info("Rules ${sykmelding.id}, ${hprRulePath.printRulePath()}")
            }
}

private fun TreeNode<HPRRules, HPRResult>.evaluate(
    sykmelding: Sykmelding,
    behandlerOgStartdato: BehandlerOgStartdato,
): HPRTreeOutput =
    when (this) {
        is ResultNode -> HPRTreeOutput(treeResult = result)
        is RuleNode -> {
            val rule = getRule(rule)
            val result = rule(sykmelding, behandlerOgStartdato)
            val childNode = if (result.ruleResult) yes else no
            result join childNode.evaluate(sykmelding, behandlerOgStartdato)
        }
    }
