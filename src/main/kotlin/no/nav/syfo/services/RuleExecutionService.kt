package no.nav.syfo.services

import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.arbeidsuforhet.ArbeidsuforhetRulesExecution
import no.nav.syfo.rules.arbeidsuforhet.arbeidsuforhetRuleTree
import no.nav.syfo.rules.common.Juridisk
import no.nav.syfo.rules.common.RuleExecution
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.gradert.GradertRulesExecution
import no.nav.syfo.rules.gradert.gradertRuleTree
import no.nav.syfo.rules.hpr.HPRRulesExecution
import no.nav.syfo.rules.hpr.hprRuleTree
import no.nav.syfo.rules.legesuspensjon.LegeSuspensjonRulesExecution
import no.nav.syfo.rules.legesuspensjon.legeSuspensjonRuleTree
import no.nav.syfo.rules.patientageover70.PatientAgeOver70RulesExecution
import no.nav.syfo.rules.patientageover70.patientAgeOver70RuleTree
import no.nav.syfo.rules.patientunder13.PatientAgeUnder13RulesExecution
import no.nav.syfo.rules.patientunder13.patientAgeUnder13RuleTree
import no.nav.syfo.rules.periode.PeriodeRulesExecution
import no.nav.syfo.rules.periode.periodeRuleTree
import no.nav.syfo.rules.periodvalidering.PeriodLogicRulesExecution
import no.nav.syfo.rules.periodvalidering.periodLogicRuleTree
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRulesExecution
import no.nav.syfo.rules.tilbakedatering.tilbakedateringRuleTree
import no.nav.syfo.rules.validation.ValidationRulesExecution
import no.nav.syfo.rules.validation.validationRuleTree

class RuleExecutionService() {

    private val ruleExecution =
        sequenceOf(
            LegeSuspensjonRulesExecution(legeSuspensjonRuleTree),
            ValidationRulesExecution(validationRuleTree),
            PeriodLogicRulesExecution(periodLogicRuleTree),
            HPRRulesExecution(hprRuleTree),
            ArbeidsuforhetRulesExecution(arbeidsuforhetRuleTree),
            PatientAgeUnder13RulesExecution(patientAgeUnder13RuleTree),
            PatientAgeOver70RulesExecution(patientAgeOver70RuleTree),
            PeriodeRulesExecution(periodeRuleTree),
            GradertRulesExecution(gradertRuleTree),
            TilbakedateringRulesExecution(tilbakedateringRuleTree),
        )

    fun runRules(
        sykmelding: Sykmelding,
        ruleMetadataSykmelding: RuleMetadataSykmelding,
        sequence: Sequence<RuleExecution<out Enum<*>>> = ruleExecution,
    ): List<Pair<TreeOutput<out Enum<*>, RuleResult>, Juridisk>> {
        var lastStatus = Status.OK
        val results =
            sequence
                .map { it.runRules(sykmelding, ruleMetadataSykmelding) }
                .takeWhile {
                    if (lastStatus == Status.OK) {
                        lastStatus = it.first.treeResult.status
                        true
                    } else {
                        false
                    }
                }
        return results.toList()
    }
}
