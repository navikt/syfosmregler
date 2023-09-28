package no.nav.syfo.rules.periode

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
import no.nav.syfo.services.RuleMetadataSykmelding

typealias PeriodeRuleTreeOutput = TreeOutput<PeriodeRules, RuleResult>

class PeriodeRulesExecution(
    private val rootNode: TreeNode<PeriodeRules, RuleResult> = periodeRuleTree
) : RuleExecution<PeriodeRules> {
    override fun runRules(sykmelding: Sykmelding, ruleMetadata: RuleMetadataSykmelding) =
        rootNode.evaluate(sykmelding, ruleMetadata).also {
            logger.info("Rules ${sykmelding.id}, ${it.printRulePath()}")
        } to
            MedJuridisk(
                JuridiskHenvisning(
                    Lovverk.FOLKETRYGDLOVEN,
                    "8-13",
                    1,
                    null,
                    null,
                    null,
                )
            )

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
