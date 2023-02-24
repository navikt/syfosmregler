package no.nav.syfo.rules.validation

import no.nav.syfo.log
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.ResultNode
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.TreeNode
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.join
import no.nav.syfo.rules.dsl.printRulePath

typealias ValidationTreeOutput = TreeOutput<ValidationRuleHit, RuleResult>
typealias ValidationTreeNode = TreeNode<ValidationRuleHit, RuleResult>

class ValidationRulesExecution(private val rootNode: ValidationTreeNode = validationRuleTree) {
    fun runRules(sykmelding: Sykmelding, ruleMetadata: RuleMetadata): ValidationTreeOutput =
        rootNode
            .evaluate(sykmelding, ruleMetadata)
            .also { validationRulePath ->
                log.info("Rules ${sykmelding.id}, ${validationRulePath.printRulePath()}")
            }
}

private fun TreeNode<ValidationRuleHit, RuleResult>.evaluate(
    sykmelding: Sykmelding,
    ruleMetadata: RuleMetadata,
): ValidationTreeOutput =
    when (this) {
        is ResultNode -> ValidationTreeOutput(treeResult = result)
        is RuleNode -> {
            val rule = getRule(rule)
            val result = rule(sykmelding, ruleMetadata)
            val childNode = if (result.ruleResult) yes else no
            result join childNode.evaluate(sykmelding, ruleMetadata)
        }
    }
