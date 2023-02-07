package no.nav.syfo.rules.legesuspensjon

import no.nav.syfo.rules.dsl.RuleResult

typealias Rule<T> = (behandlerSuspendert: Boolean) -> RuleResult<T>
typealias LegeSuspensjonRule = Rule<LegeSuspensjonRules>

val behandlerSuspendert: LegeSuspensjonRule = { behandlerSuspendert ->
    val suspendert = behandlerSuspendert

    RuleResult(
        ruleInputs = mapOf("suspendert" to suspendert),
        rule = LegeSuspensjonRules.BEHANDLER_SUSPENDERT,
        ruleResult = suspendert
    )
}
