package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.MANUAL_PROCESSING
import no.nav.syfo.model.Status.OK
import no.nav.syfo.model.juridisk.JuridiskHenvisning
import no.nav.syfo.model.juridisk.Lovverk
import no.nav.syfo.rules.common.MedJuridisk
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRuleHit.INNTIL_30_DAGER
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRuleHit.INNTIL_30_DAGER_MED_BEGRUNNELSE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRuleHit.INNTIL_8_DAGER
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRuleHit.OVER_30_DAGER
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRuleHit.OVER_30_DAGER_MED_BEGRUNNELSE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRuleHit.OVER_30_DAGER_SPESIALISTHELSETJENESTEN
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.ARBEIDSGIVERPERIODE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.BEGRUNNELSE_MIN_1_ORD
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.BEGRUNNELSE_MIN_3_ORD
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.ETTERSENDING
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.FORLENGELSE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.SPESIALISTHELSETJENESTEN
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERING
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERING_OVER_4_DAGER
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_INNTIL_1_MAANDE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_INNTIL_8_DAGER

enum class TilbakedateringRules {
    ARBEIDSGIVERPERIODE,
    BEGRUNNELSE_MIN_1_ORD,
    BEGRUNNELSE_MIN_3_ORD,
    ETTERSENDING,
    FORLENGELSE,
    SPESIALISTHELSETJENESTEN,
    TILBAKEDATERING,
    TILBAKEDATERING_OVER_4_DAGER,
    TILBAKEDATERT_INNTIL_8_DAGER,
    TILBAKEDATERT_INNTIL_1_MAANDE,
}

val tilbakedateringRuleTree =
    tree<TilbakedateringRules, RuleResult>(TILBAKEDATERING) {
        yes(ETTERSENDING) {
            yes(OK)
            no(TILBAKEDATERING_OVER_4_DAGER) {
                no(OK)
                yes(TILBAKEDATERT_INNTIL_8_DAGER) {
                    yes(BEGRUNNELSE_MIN_1_ORD) {
                        yes(OK)
                        no(FORLENGELSE) {
                            yes(OK)
                            no(SPESIALISTHELSETJENESTEN) {
                                yes(OK)
                                no(INVALID, INNTIL_8_DAGER)
                            }
                        }
                    }
                    no(TILBAKEDATERT_INNTIL_1_MAANDE) {
                        yes(BEGRUNNELSE_MIN_1_ORD) {
                            yes(FORLENGELSE) {
                                yes(OK)
                                no(ARBEIDSGIVERPERIODE) {
                                    yes(OK)
                                    no(SPESIALISTHELSETJENESTEN) {
                                        yes(OK)
                                        no(MANUAL_PROCESSING, INNTIL_30_DAGER_MED_BEGRUNNELSE)
                                    }
                                }
                            }
                            no(SPESIALISTHELSETJENESTEN) {
                                yes(OK)
                                no(INVALID, INNTIL_30_DAGER)
                            }
                        }
                        no(BEGRUNNELSE_MIN_3_ORD) {
                            yes(MANUAL_PROCESSING, OVER_30_DAGER_MED_BEGRUNNELSE)
                            no(SPESIALISTHELSETJENESTEN) {
                                yes(MANUAL_PROCESSING, OVER_30_DAGER_SPESIALISTHELSETJENESTEN)
                                no(INVALID, OVER_30_DAGER)
                            }
                        }
                    }
                }
            }
        }
        no(OK)
    } to
        MedJuridisk(
            JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-7",
                ledd = 2,
                punktum = null,
                bokstav = null,
            ),
        )

internal fun RuleNode<TilbakedateringRules, RuleResult>.yes(
    status: Status,
    ruleHit: TilbakedateringRuleHit? = null
) {
    yes(RuleResult(status = status, ruleHit = ruleHit?.ruleHit))
}

internal fun RuleNode<TilbakedateringRules, RuleResult>.no(
    status: Status,
    ruleHit: TilbakedateringRuleHit? = null
) {
    no(RuleResult(status = status, ruleHit = ruleHit?.ruleHit))
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
        TILBAKEDATERING_OVER_4_DAGER -> tilbakedateringOver4Dager
        TILBAKEDATERT_INNTIL_8_DAGER -> tilbakedateringInntil8Dager
        TILBAKEDATERT_INNTIL_1_MAANDE -> tilbakedateringInntil1Maande
    }
}
