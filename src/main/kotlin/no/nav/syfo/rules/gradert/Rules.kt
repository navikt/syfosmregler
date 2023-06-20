package no.nav.syfo.rules.gradert

import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.services.RuleMetadataSykmelding

typealias Rule<T> = (sykmelding: Sykmelding, ruleMetadata: RuleMetadataSykmelding) -> RuleResult<T>

typealias GradertRule = Rule<GradertRules>

val gradertUnder20Prosent: GradertRule = { sykmelding, _ ->
    val perioder = sykmelding.perioder

    val gradertUnder20Prosent = perioder.any { it.gradert != null && it.gradert!!.grad < 20 }

    RuleResult(
        ruleInputs = mapOf("gradertUnder20Prosent" to gradertUnder20Prosent),
        rule = GradertRules.GRADERT_UNDER_20_PROSENT,
        ruleResult = gradertUnder20Prosent,
    )
}
