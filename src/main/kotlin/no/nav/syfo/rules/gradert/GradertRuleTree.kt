package no.nav.syfo.rules.gradert

import no.nav.syfo.model.Status
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree

val gradertRuleTree = tree<GradertRules, RuleResult>(GradertRules.GRADERT_UNDER_20_PROSENT) {
    yes(Status.INVALID, GradertRuleHit.GRADERT_SYKMELDING_UNDER_20_PROSENT)
    no(Status.OK)
}

internal fun RuleNode<GradertRules, RuleResult>.yes(status: Status, ruleHit: GradertRuleHit? = null) {
    yes(RuleResult(status, ruleHit?.ruleHit))
}

internal fun RuleNode<GradertRules, RuleResult>.no(status: Status, ruleHit: GradertRuleHit? = null) {
    no(RuleResult(status, ruleHit?.ruleHit))
}

fun getRule(rules: GradertRules): Rule<GradertRules> {
    return when (rules) {
        GradertRules.GRADERT_UNDER_20_PROSENT -> gradertUnder20Prosent
    }
}
