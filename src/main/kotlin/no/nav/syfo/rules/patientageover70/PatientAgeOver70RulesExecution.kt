package no.nav.syfo.rules.patientageover70

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

typealias PatientAgeOver70TreeOutput = TreeOutput<PatientAgeOver70Rules, RuleResult>

class PatientAgeOver70RulesExecution(
    val rootNode: TreeNode<PatientAgeOver70Rules, RuleResult> = patientAgeOver70RuleTree
) : RuleExecution<PatientAgeOver70Rules> {
    override fun runRules(sykmelding: Sykmelding, ruleMetadata: RuleMetadataSykmelding) =
        rootNode.evaluate(sykmelding, ruleMetadata).also { patientAgeOver70RulePath ->
            logger.info("Rules ${sykmelding.id}, ${patientAgeOver70RulePath.printRulePath()}")
        } to
            MedJuridisk(
                JuridiskHenvisning(
                    lovverk = Lovverk.FOLKETRYGDLOVEN,
                    paragraf = "8-3",
                    ledd = 1,
                    punktum = 2,
                    bokstav = null,
                ),
            )
}

private fun TreeNode<PatientAgeOver70Rules, RuleResult>.evaluate(
    sykmelding: Sykmelding,
    ruleMetadata: RuleMetadataSykmelding,
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
