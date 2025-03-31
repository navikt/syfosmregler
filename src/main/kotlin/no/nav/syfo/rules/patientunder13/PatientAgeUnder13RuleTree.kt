package no.nav.syfo.rules.patientunder13

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.OK
import no.nav.syfo.model.juridisk.JuridiskEnum
import no.nav.syfo.model.juridisk.JuridiskHenvisning
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree

enum class PatientAgeUnder13Rules {
    PASIENT_YNGRE_ENN_13,
}

val patientAgeUnder13RuleTree =
    tree<PatientAgeUnder13Rules, RuleResult>(PatientAgeUnder13Rules.PASIENT_YNGRE_ENN_13) {
        yes(
            INVALID,
            JuridiskEnum.FOLKETRYGDLOVEN_8_3_1,
            PatientAgeUnder13RuleHit.PASIENT_YNGRE_ENN_13
        )
        no(OK, JuridiskEnum.FOLKETRYGDLOVEN_8_3_1)
    }

internal fun RuleNode<PatientAgeUnder13Rules, RuleResult>.yes(
    status: Status,
    juridisk: JuridiskEnum,
    ruleHit: PatientAgeUnder13RuleHit? = null
) {
    yes(RuleResult(status, juridisk.JuridiskHenvisning, ruleHit?.ruleHit))
}

internal fun RuleNode<PatientAgeUnder13Rules, RuleResult>.no(
    status: Status,
    juridisk: JuridiskEnum,
    ruleHit: PatientAgeUnder13RuleHit? = null
) {
    no(RuleResult(status, juridisk.JuridiskHenvisning, ruleHit?.ruleHit))
}

fun getRule(rules: PatientAgeUnder13Rules): Rule<PatientAgeUnder13Rules> {
    return when (rules) {
        PatientAgeUnder13Rules.PASIENT_YNGRE_ENN_13 -> pasientUnder13Aar
    }
}
