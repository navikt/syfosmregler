package no.nav.syfo.rules.validation

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.OK
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.rule
import no.nav.syfo.rules.dsl.tree
import no.nav.syfo.rules.validation.ValidationRules.*



val validationRules = listOf<RuleNode<ValidationRules, RuleResult>>(
    rule(PASIENT_YNGRE_ENN_13) {
        yes(INVALID)
        no(OK)
    }
)

fun validationRuleTree() = tree<ValidationRules, RuleResult>(PASIENT_YNGRE_ENN_13) {
    yes(INVALID)
    no(UGYLDIG_REGELSETTVERSJON) {
        yes(INVALID)
        no(MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39) {
            yes(INVALID)
            no(UGYLDIG_ORGNR_LENGDE) {
                yes(INVALID)
                no(AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR) {
                    yes(INVALID)
                    no(BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR) {
                        yes(INVALID)
                        no(OK)
                    }
                }
            }
        }
    }
}

internal fun RuleNode<ValidationRules, RuleResult>.yes(status: Status) {
    yes(RuleResult(status, rule.ruleHit))
}

internal fun RuleNode<ValidationRules, RuleResult>.no(status: Status) {
    no(RuleResult(status, rule.ruleHit))
}

fun getRule(rules: ValidationRules): Rule<ValidationRules> {
    return when (rules) {
        PASIENT_YNGRE_ENN_13 -> pasientUnder13Aar
        UGYLDIG_REGELSETTVERSJON -> ugyldigRegelsettversjon
        MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 -> manglendeDynamiskesporsmaalversjon2uke39
        UGYLDIG_ORGNR_LENGDE -> ugyldingOrgNummerLengde
        AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR -> avsenderSammeSomPasient
        BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR -> behandlerSammeSomPasient
    }
}
