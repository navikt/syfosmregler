package no.nav.syfo.rules.hpr

import no.nav.syfo.logger
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.model.juridisk.JuridiskHenvisning
import no.nav.syfo.model.juridisk.Lovverk
import no.nav.syfo.rules.common.MedJuridisk
import no.nav.syfo.rules.common.RuleExecution
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.ResultNode
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.TreeNode
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.join
import no.nav.syfo.rules.dsl.printRulePath
import no.nav.syfo.services.BehandlerOgStartdato
import no.nav.syfo.services.RuleMetadataSykmelding

typealias HPRTreeOutput = TreeOutput<HPRRules, RuleResult>

typealias HPRTreeNode = TreeNode<HPRRules, RuleResult>

class HPRRulesExecution(private val rootNode: HPRTreeNode = hprRuleTree) : RuleExecution<HPRRules> {
    override fun runRules(sykmelding: Sykmelding, ruleMetadata: RuleMetadataSykmelding) =
        rootNode.evaluate(sykmelding, ruleMetadata.behandlerOgStartdato).also { hprRulePath ->
            logger.info("Rules ${sykmelding.id}, ${hprRulePath.printRulePath()}")
        } to
            MedJuridisk(
                JuridiskHenvisning(
                    lovverk = Lovverk.HELSEPERSONELLOVEN,
                    paragraf = "3",
                    ledd = null,
                    punktum = null,
                    bokstav = null,
                )
            )
}

private fun TreeNode<HPRRules, RuleResult>.evaluate(
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
