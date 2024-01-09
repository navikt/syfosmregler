package no.nav.syfo.rules.legesuspensjon

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

typealias LegeSuspensjonTreeOutput = TreeOutput<LegeSuspensjonRules, RuleResult>

typealias LegeSuspensjonTreeNode = TreeNode<LegeSuspensjonRules, RuleResult>

class LegeSuspensjonRulesExecution(
    val rootNode: Pair<TreeNode<LegeSuspensjonRules, RuleResult>, Juridisk> = legeSuspensjonRuleTree
) : RuleExecution<LegeSuspensjonRules> {
    override fun runRules(
        sykmelding: Sykmelding,
        ruleMetadata: RuleMetadataSykmelding
    ): Pair<LegeSuspensjonTreeOutput, Juridisk> =
        rootNode.first.evaluate(sykmelding.id, ruleMetadata.doctorSuspensjon).also {
            legeSuspensjonRulePath ->
            logger.info("Rules ${sykmelding.id}, ${legeSuspensjonRulePath.printRulePath()}")
        } to rootNode.second
}

private fun TreeNode<LegeSuspensjonRules, RuleResult>.evaluate(
    sykmeldingId: String,
    behandlerSuspendert: Boolean,
): LegeSuspensjonTreeOutput =
    when (this) {
        is ResultNode -> LegeSuspensjonTreeOutput(treeResult = result)
        is RuleNode -> {
            val rule = getRule(rule)
            val result = rule(behandlerSuspendert)
            val childNode = if (result.ruleResult) yes else no
            result join childNode.evaluate(sykmeldingId, behandlerSuspendert)
        }
    }
