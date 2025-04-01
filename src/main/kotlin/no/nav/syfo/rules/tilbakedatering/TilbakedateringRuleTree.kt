package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.MANUAL_PROCESSING
import no.nav.syfo.model.Status.OK
import no.nav.syfo.model.juridisk.JuridiskEnum
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRuleHit.INNTIL_8_DAGER
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRuleHit.MINDRE_ENN_1_MAANED
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRuleHit.MINDRE_ENN_1_MAANED_MED_BEGRUNNELSE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRuleHit.OVER_1_MND
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRuleHit.OVER_1_MND_MED_BEGRUNNELSE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.ARBEIDSGIVERPERIODE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.BEGRUNNELSE_MIN_1_ORD
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.BEGRUNNELSE_MIN_3_ORD
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.ETTERSENDING
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.FORLENGELSE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.SPESIALISTHELSETJENESTEN
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERING
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_INNTIL_4_DAGER
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_INNTIL_8_DAGER
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_MINDRE_ENN_1_MAANED

enum class TilbakedateringRules {
    ARBEIDSGIVERPERIODE,
    BEGRUNNELSE_MIN_1_ORD,
    BEGRUNNELSE_MIN_3_ORD,
    ETTERSENDING,
    FORLENGELSE,
    SPESIALISTHELSETJENESTEN,
    TILBAKEDATERING,
    TILBAKEDATERT_INNTIL_4_DAGER,
    TILBAKEDATERT_INNTIL_8_DAGER,
    TILBAKEDATERT_MINDRE_ENN_1_MAANED,
}

val tilbakedateringRuleTree =
    tree<TilbakedateringRules, RuleResult>(TILBAKEDATERING) {
        yes(SPESIALISTHELSETJENESTEN) {
            yes(OK, JuridiskEnum.FOLKETRYGDLOVEN_8_7_1_1)
            no(ETTERSENDING) {
                yes(OK, JuridiskEnum.FOLKETRYGDLOVEN_8_7_1_1)
                no(TILBAKEDATERT_INNTIL_4_DAGER) {
                    yes(OK, JuridiskEnum.FOLKETRYGDLOVEN_8_7_2_2)
                    no(TILBAKEDATERT_INNTIL_8_DAGER) {
                        yes(BEGRUNNELSE_MIN_1_ORD) {
                            yes(OK, JuridiskEnum.FOLKETRYGDLOVEN_8_7_2_2)
                            no(FORLENGELSE) {
                                yes(OK, JuridiskEnum.FOLKETRYGDLOVEN_8_7_1_1)
                                no(INVALID, JuridiskEnum.FOLKETRYGDLOVEN_8_7_1_1, INNTIL_8_DAGER)
                            }
                        }
                        no(TILBAKEDATERT_MINDRE_ENN_1_MAANED) {
                            yes(BEGRUNNELSE_MIN_1_ORD) {
                                yes(FORLENGELSE) {
                                    yes(OK, JuridiskEnum.FOLKETRYGDLOVEN_8_7_1_1)
                                    no(ARBEIDSGIVERPERIODE) {
                                        yes(OK, JuridiskEnum.FOLKETRYGDLOVEN_8_7_2_2)
                                        no(
                                            MANUAL_PROCESSING,
                                            JuridiskEnum.FOLKETRYGDLOVEN_8_7,
                                            MINDRE_ENN_1_MAANED_MED_BEGRUNNELSE
                                        )
                                    }
                                }
                                no(
                                    INVALID,
                                    JuridiskEnum.FOLKETRYGDLOVEN_8_7_1_1,
                                    MINDRE_ENN_1_MAANED
                                )
                            }
                            no(BEGRUNNELSE_MIN_3_ORD) {
                                yes(
                                    MANUAL_PROCESSING,
                                    JuridiskEnum.FOLKETRYGDLOVEN_8_7,
                                    OVER_1_MND_MED_BEGRUNNELSE
                                )
                                no(INVALID, JuridiskEnum.FOLKETRYGDLOVEN_8_7_1_1, OVER_1_MND)
                            }
                        }
                    }
                }
            }
        }
        no(OK, JuridiskEnum.FOLKETRYGDLOVEN_8_7_1_1)
    }

internal fun RuleNode<TilbakedateringRules, RuleResult>.yes(
    status: Status,
    juridisk: JuridiskEnum,
    ruleHit: TilbakedateringRuleHit? = null
) {
    yes(RuleResult(status = status, juridisk.JuridiskHenvisning, ruleHit = ruleHit?.ruleHit))
}

internal fun RuleNode<TilbakedateringRules, RuleResult>.no(
    status: Status,
    juridisk: JuridiskEnum,
    ruleHit: TilbakedateringRuleHit? = null
) {
    no(RuleResult(status = status, juridisk.JuridiskHenvisning, ruleHit = ruleHit?.ruleHit))
}

fun getRule(rules: TilbakedateringRules): Rule<TilbakedateringRules> {
    return when (rules) {
        ARBEIDSGIVERPERIODE -> arbeidsgiverperiode
        BEGRUNNELSE_MIN_1_ORD -> begrunnelse_min_1_ord
        BEGRUNNELSE_MIN_3_ORD -> begrunnelse_min_3_ord
        ETTERSENDING -> ettersending
        FORLENGELSE -> forlengelse
        SPESIALISTHELSETJENESTEN -> spesialisthelsetjenesten
        TILBAKEDATERING -> tilbakedatering
        TILBAKEDATERT_INNTIL_4_DAGER -> tilbakedateringInntil4Dager
        TILBAKEDATERT_INNTIL_8_DAGER -> tilbakedateringInntil8Dager
        TILBAKEDATERT_MINDRE_ENN_1_MAANED -> tilbakedateringMindreEnn1Maaned
    }
}
