package no.nav.syfo.rules.periode

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.OK
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree
import no.nav.syfo.rules.periode.PeriodeRuleHit.*
import no.nav.syfo.rules.periodvalidering.PeriodLogicRuleHit

enum class PeriodeRules {
    FREMDATERT_MER_ENN_30_DAGER,
    TILBAKEDATERT_MER_ENN_3_AR,
    TOTAL_VARIGHET_OVER_ETT_AAR,
}

val periodeRuleTree =
    tree<PeriodeRules, RuleResult>(PeriodeRules.FREMDATERT_MER_ENN_30_DAGER) {
        yes(INVALID, FREMDATERT_MER_ENN_30_DAGER)
        no(PeriodeRules.TILBAKEDATERT_MER_ENN_3_AR) {
            yes(INVALID, TILBAKEDATERT_MER_ENN_3_AR)
            no(PeriodeRules.TOTAL_VARIGHET_OVER_ETT_AAR) {
                yes(INVALID, TOTAL_VARIGHET_OVER_ETT_AAR)
                no(OK)
            }
        }
    }

internal fun RuleNode<PeriodeRules, RuleResult>.yes(
    status: Status,
    ruleHit: PeriodeRuleHit? = null
) {
    yes(RuleResult(status, ruleHit?.ruleHit))
}

internal fun RuleNode<PeriodeRules, RuleResult>.no(
    status: Status,
    ruleHit: PeriodLogicRuleHit? = null
) {
    no(RuleResult(status, ruleHit?.ruleHit))
}

fun getRule(rules: PeriodeRules): Rule<PeriodeRules> {
    return when (rules) {
        PeriodeRules.FREMDATERT_MER_ENN_30_DAGER -> fremdatertOver30Dager
        PeriodeRules.TILBAKEDATERT_MER_ENN_3_AR -> tilbakeDatertOver3Ar
        PeriodeRules.TOTAL_VARIGHET_OVER_ETT_AAR -> varighetOver1AAr
    }
}
