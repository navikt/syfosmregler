package no.nav.syfo.rules.validation

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.OK
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree

enum class ValidationRules {
    PASIENT_YNGRE_ENN_13
}

data class ValidationResult(
    val status: Status,
    val ruleHit: RuleHit?
) {
    override fun toString(): String {
        return status.name + (ruleHit?.let { "->${it.name}" } ?: "")
    }
}

val validationRuleTree = tree<ValidationRules, ValidationResult>(ValidationRules.PASIENT_YNGRE_ENN_13) {
    yes(INVALID, RuleHit.PASIENT_YNGRE_ENN_13)
    no(OK)
}

internal fun RuleNode<ValidationRules, ValidationResult>.yes(status: Status, ruleHit: RuleHit? = null) {
    yes(ValidationResult(status, ruleHit))
}

internal fun RuleNode<ValidationRules, ValidationResult>.no(status: Status, ruleHit: RuleHit? = null) {
    no(ValidationResult(status, ruleHit))
}

fun getRule(rules: ValidationRules): Rule<ValidationRules> {
    return when (rules) {
        ValidationRules.PASIENT_YNGRE_ENN_13 -> pasientUnder13Aar
    }
}
