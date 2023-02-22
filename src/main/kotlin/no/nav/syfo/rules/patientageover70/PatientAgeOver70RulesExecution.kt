package no.nav.syfo.rules.patientageover70

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

typealias PatientAgeOver70TreeOutput = TreeOutput<PatientAgeOver70Rules, RuleResult>
typealias PatientAgeOver70TreeNode = TreeNode<PatientAgeOver70Rules, RuleResult>

class PatientAgeOver70RulesExecution(private val rootNode: PatientAgeOver70TreeNode = patientAgeOver70RuleTree) {
    fun runRules(sykmelding: Sykmelding, ruleMetadata: RuleMetadata): PatientAgeOver70TreeOutput =
        rootNode
            .evaluate(sykmelding, ruleMetadata)
            .also { validationRulePath ->
                log.info("Rules ${sykmelding.id}, ${validationRulePath.printRulePath()}")
            }
}

private fun TreeNode<PatientAgeOver70Rules, RuleResult>.evaluate(
    sykmelding: Sykmelding,
    ruleMetadata: RuleMetadata,
): PatientAgeOver70TreeOutput =
    when (this) {
        is ResultNode -> PatientAgeOver70TreeOutput(treeResult = result)
        is RuleNode -> {
            val rule = getRule(rule)
            val result = rule(sykmelding, ruleMetadata)
            val childNode = if (result.ruleResult) yes else no
            result join childNode.evaluate(sykmelding, ruleMetadata)
        }
    }
