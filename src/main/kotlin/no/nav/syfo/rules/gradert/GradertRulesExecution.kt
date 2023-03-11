package no.nav.syfo.rules.gradert

import no.nav.syfo.log
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.model.juridisk.JuridiskHenvisning
import no.nav.syfo.model.juridisk.Lovverk
import no.nav.syfo.rules.common.Juridisk
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

typealias GradertTreeOutput = TreeOutput<GradertRules, RuleResult>

class GradertRulesExecution(val rootNode: TreeNode<GradertRules, RuleResult> = gradertRuleTree) : RuleExecution<GradertRules> {
    override fun runRules(sykmelding: Sykmelding, ruleMetadata: RuleMetadataSykmelding): Pair<GradertTreeOutput, Juridisk> =
        rootNode
            .evaluate(sykmelding, ruleMetadata)
            .also { gradertRulePath ->
                log.info("Rules ${sykmelding.id}, ${gradertRulePath.printRulePath()}")
            } to MedJuridisk(
            JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-13",
                ledd = 1,
                punktum = null,
                bokstav = null
            )
        )
}

private fun TreeNode<GradertRules, RuleResult>.evaluate(
    sykmelding: Sykmelding,
    ruleMetadata: RuleMetadataSykmelding
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
