package no.nav.syfo.rules.validation

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.OK
import no.nav.syfo.model.juridisk.JuridiskEnum
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree

enum class ValidationRules {
    UGYLDIG_REGELSETTVERSJON,
    MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39,
    UGYLDIG_ORGNR_LENGDE,
    AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR,
    BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR,
}

val validationRuleTree =
    tree<ValidationRules, RuleResult>(ValidationRules.UGYLDIG_REGELSETTVERSJON) {
        yes(INVALID, JuridiskEnum.INGEN, ValidationRuleHit.UGYLDIG_REGELSETTVERSJON)
        no(ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39) {
            yes(
                INVALID,
                JuridiskEnum.INGEN,
                ValidationRuleHit.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39
            )
            no(ValidationRules.UGYLDIG_ORGNR_LENGDE) {
                yes(INVALID, JuridiskEnum.INGEN, ValidationRuleHit.UGYLDIG_ORGNR_LENGDE)
                no(ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR) {
                    yes(
                        INVALID,
                        JuridiskEnum.INGEN,
                        ValidationRuleHit.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR
                    )
                    no(ValidationRules.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR) {
                        yes(
                            INVALID,
                            JuridiskEnum.INGEN,
                            ValidationRuleHit.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR
                        )
                        no(OK, JuridiskEnum.INGEN)
                    }
                }
            }
        }
    }

internal fun RuleNode<ValidationRules, RuleResult>.yes(
    status: Status,
    juridisk: JuridiskEnum,
    ruleHit: ValidationRuleHit? = null
) {
    yes(RuleResult(status, juridisk.JuridiskHenvisning, ruleHit?.ruleHit))
}

internal fun RuleNode<ValidationRules, RuleResult>.no(
    status: Status,
    juridisk: JuridiskEnum,
    ruleHit: ValidationRuleHit? = null
) {
    no(RuleResult(status, juridisk.JuridiskHenvisning, ruleHit?.ruleHit))
}

fun getRule(rules: ValidationRules): Rule<ValidationRules> {
    return when (rules) {
        ValidationRules.UGYLDIG_REGELSETTVERSJON -> ugyldigRegelsettversjon
        ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 ->
            manglendeDynamiskesporsmaalversjon2uke39
        ValidationRules.UGYLDIG_ORGNR_LENGDE -> ugyldingOrgNummerLengde
        ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR -> avsenderSammeSomPasient
        ValidationRules.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR -> behandlerSammeSomPasient
    }
}
