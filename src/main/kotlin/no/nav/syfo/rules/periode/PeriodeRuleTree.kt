package no.nav.syfo.rules.periode

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.OK
import no.nav.syfo.model.juridisk.JuridiskEnum
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree
import no.nav.syfo.rules.periode.PeriodeRuleHit.*
import no.nav.syfo.rules.periodvalidering.PeriodLogicRuleHit

enum class PeriodeRules {
    FREMDATERT,
    TILBAKEDATERT_MER_ENN_3_AR,
    TOTAL_VARIGHET_OVER_ETT_AAR,
}

val periodeRuleTree =
    tree<PeriodeRules, RuleResult>(PeriodeRules.FREMDATERT) {
        yes(INVALID, JuridiskEnum.INGEN, FREMDATERT)
        no(PeriodeRules.TILBAKEDATERT_MER_ENN_3_AR) {
            yes(INVALID, JuridiskEnum.INGEN, TILBAKEDATERT_MER_ENN_3_AR)
            no(PeriodeRules.TOTAL_VARIGHET_OVER_ETT_AAR) {
                yes(INVALID, JuridiskEnum.INGEN, TOTAL_VARIGHET_OVER_ETT_AAR)
                no(OK, JuridiskEnum.INGEN)
            }
        }
    }

internal fun RuleNode<PeriodeRules, RuleResult>.yes(
    status: Status,
    juridisk: JuridiskEnum,
    ruleHit: PeriodeRuleHit? = null
) {
    yes(RuleResult(status, juridisk.JuridiskHenvisning, ruleHit?.ruleHit))
}

internal fun RuleNode<PeriodeRules, RuleResult>.no(
    status: Status,
    juridisk: JuridiskEnum,
    ruleHit: PeriodLogicRuleHit? = null
) {
    no(RuleResult(status, juridisk.JuridiskHenvisning, ruleHit?.ruleHit))
}

fun getRule(rules: PeriodeRules): Rule<PeriodeRules> {
    return when (rules) {
        PeriodeRules.FREMDATERT -> fremdatertOver30Dager
        PeriodeRules.TILBAKEDATERT_MER_ENN_3_AR -> tilbakeDatertOver3Ar
        PeriodeRules.TOTAL_VARIGHET_OVER_ETT_AAR -> varighetOver1AAr
    }
}
