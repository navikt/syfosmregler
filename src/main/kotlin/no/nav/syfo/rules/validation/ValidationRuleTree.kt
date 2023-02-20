package no.nav.syfo.rules.validation

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.OK
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree

enum class ValidationRules {
    PASIENT_YNGRE_ENN_13,
    PASIENT_ELDRE_ENN_70,
    UKJENT_DIAGNOSEKODETYPE,
    ICPC_2_Z_DIAGNOSE,
    HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER,
    UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE,
    UGYLDIG_KODEVERK_FOR_BIDIAGNOSE,
    UGYLDIG_REGELSETTVERSJON,
    MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39,
    UGYLDIG_ORGNR_LENGDE,
    AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR,
    BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR
}

val validationRuleTree = tree<ValidationRules, RuleResult>(ValidationRules.PASIENT_YNGRE_ENN_13) {
    yes(INVALID, ValidationRuleHit.PASIENT_YNGRE_ENN_13)
    no(ValidationRules.PASIENT_ELDRE_ENN_70) {
        yes(INVALID, ValidationRuleHit.PASIENT_ELDRE_ENN_70)
        no(ValidationRules.UKJENT_DIAGNOSEKODETYPE) {
            yes(INVALID, ValidationRuleHit.UKJENT_DIAGNOSEKODETYPE)
            no(ValidationRules.ICPC_2_Z_DIAGNOSE) {
                yes(INVALID, ValidationRuleHit.ICPC_2_Z_DIAGNOSE)
                no(ValidationRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER) {
                    yes(INVALID, ValidationRuleHit.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER)
                    no(ValidationRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE) {
                        yes(INVALID, ValidationRuleHit.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE)
                        no(ValidationRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE) {
                            yes(INVALID, ValidationRuleHit.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE)
                            no(ValidationRules.UGYLDIG_REGELSETTVERSJON) {
                                yes(INVALID, ValidationRuleHit.UGYLDIG_REGELSETTVERSJON)
                                no(ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39) {
                                    yes(INVALID, ValidationRuleHit.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39)
                                    no(ValidationRules.UGYLDIG_ORGNR_LENGDE) {
                                        yes(INVALID, ValidationRuleHit.UGYLDIG_ORGNR_LENGDE)
                                        no(ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR) {
                                            yes(INVALID, ValidationRuleHit.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR)
                                            no(ValidationRules.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR) {
                                                yes(INVALID, ValidationRuleHit.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR)
                                                no(OK)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun RuleNode<ValidationRules, RuleResult>.yes(status: Status, ruleHit: ValidationRuleHit? = null) {
    yes(RuleResult(status, ruleHit?.ruleHit))
}

internal fun RuleNode<ValidationRules, RuleResult>.no(status: Status, ruleHit: ValidationRuleHit? = null) {
    no(RuleResult(status, ruleHit?.ruleHit))
}

fun getRule(rules: ValidationRules): Rule<ValidationRules> {
    return when (rules) {
        ValidationRules.PASIENT_YNGRE_ENN_13 -> pasientUnder13Aar
        ValidationRules.PASIENT_ELDRE_ENN_70 -> pasientOver70Aar
        ValidationRules.UKJENT_DIAGNOSEKODETYPE -> ukjentDiagnoseKodeType
        ValidationRules.ICPC_2_Z_DIAGNOSE -> icpc2ZDiagnose
        ValidationRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER -> houvedDiagnoseEllerFraversgrunnMangler
        ValidationRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE -> ugyldigKodeVerkHouvedDiagnose
        ValidationRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE -> ugyldigKodeVerkBiDiagnose
        ValidationRules.UGYLDIG_REGELSETTVERSJON -> ugyldigRegelsettversjon
        ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 -> manglendeDynamiskesporsmaalversjon2uke39
        ValidationRules.UGYLDIG_ORGNR_LENGDE -> ugyldingOrgNummerLengde
        ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR -> avsenderSammeSomPasient
        ValidationRules.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR -> behandlerSammeSomPasient
    }
}
