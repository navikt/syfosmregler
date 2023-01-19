package no.nav.syfo.rules.legesuspensjon

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.OK
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree

enum class LegeSuspensjonRules {
    BEHANDLER_SUSPENDERT
}

data class LegeSuspensjonResult(
    val status: Status,
    val ruleHit: RuleHit?
) {
    override fun toString(): String {
        return status.name + (ruleHit?.let { "->${it.name}" } ?: "")
    }
}

val legeSuspensjonRuleTree = tree<LegeSuspensjonRules, LegeSuspensjonResult>(LegeSuspensjonRules.BEHANDLER_SUSPENDERT) {
    yes(Status.INVALID, RuleHit.BEHANDLER_SUSPENDERT)
    no(OK)
}

internal fun RuleNode<LegeSuspensjonRules, LegeSuspensjonResult>.yes(status: Status, ruleHit: RuleHit? = null) {
    yes(LegeSuspensjonResult(status, ruleHit))
}

internal fun RuleNode<LegeSuspensjonRules, LegeSuspensjonResult>.no(status: Status, ruleHit: RuleHit? = null) {
    no(LegeSuspensjonResult(status, ruleHit))
}

fun getRule(rules: LegeSuspensjonRules): Rule<LegeSuspensjonRules> {
    return when (rules) {
        LegeSuspensjonRules.BEHANDLER_SUSPENDERT -> behandlerSuspendert
    }
}
