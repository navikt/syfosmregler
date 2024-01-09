package no.nav.syfo.rules.patientunder13

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

typealias PatientAgeUnder13TreeOutput = TreeOutput<PatientAgeUnder13Rules, RuleResult>

typealias PatientAgeUnder12TreeNode = Pair<TreeNode<PatientAgeUnder13Rules, RuleResult>, Juridisk>

class PatientAgeUnder13RulesExecution(
    val rootNode: PatientAgeUnder12TreeNode = patientAgeUnder13RuleTree
) : RuleExecution<PatientAgeUnder13Rules> {
    override fun runRules(sykmelding: Sykmelding, ruleMetadata: RuleMetadataSykmelding) =
        rootNode.first.evaluate(sykmelding, ruleMetadata).also { patientAgeUnder13 ->
            logger.info("Rules ${sykmelding.id}, ${patientAgeUnder13.printRulePath()}")
        } to rootNode.second
}

private fun TreeNode<PatientAgeUnder13Rules, RuleResult>.evaluate(
    sykmelding: Sykmelding,
    ruleMetadata: RuleMetadataSykmelding,
): PatientAgeUnder13TreeOutput =
    when (this) {
        is ResultNode -> PatientAgeUnder13TreeOutput(treeResult = result)
        is RuleNode -> {
            val rule = getRule(rule)
            val result = rule(sykmelding, ruleMetadata)
            val childNode = if (result.ruleResult) yes else no
            result join childNode.evaluate(sykmelding, ruleMetadata)
        }
    }
