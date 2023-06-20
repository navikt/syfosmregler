package no.nav.syfo.rules.patientageover70

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.OK
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree

enum class PatientAgeOver70Rules {
    PASIENT_ELDRE_ENN_70,
}

val patientAgeOver70RuleTree =
    tree<PatientAgeOver70Rules, RuleResult>(PatientAgeOver70Rules.PASIENT_ELDRE_ENN_70) {
        yes(INVALID, PatientAgeOver70RuleHit.PASIENT_ELDRE_ENN_70)
        no(OK)
    }

internal fun RuleNode<PatientAgeOver70Rules, RuleResult>.yes(
    status: Status,
    ruleHit: PatientAgeOver70RuleHit? = null
) {
    yes(RuleResult(status, ruleHit?.ruleHit))
}

internal fun RuleNode<PatientAgeOver70Rules, RuleResult>.no(
    status: Status,
    ruleHit: PatientAgeOver70RuleHit? = null
) {
    no(RuleResult(status, ruleHit?.ruleHit))
}

fun getRule(rules: PatientAgeOver70Rules): Rule<PatientAgeOver70Rules> {
    return when (rules) {
        PatientAgeOver70Rules.PASIENT_ELDRE_ENN_70 -> pasientOver70Aar
    }
}
