package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.MANUAL_PROCESSING
import no.nav.syfo.model.Status.OK
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree
import no.nav.syfo.rules.tilbakedatering.RuleHit.INNTIL_30_DAGER
import no.nav.syfo.rules.tilbakedatering.RuleHit.INNTIL_30_DAGER_MED_BEGRUNNELSE
import no.nav.syfo.rules.tilbakedatering.RuleHit.INNTIL_8_DAGER
import no.nav.syfo.rules.tilbakedatering.RuleHit.OVER_30_DAGER
import no.nav.syfo.rules.tilbakedatering.RuleHit.OVER_30_DAGER_MED_BEGRUNNELSE
import no.nav.syfo.rules.tilbakedatering.RuleHit.OVER_30_DAGER_SPESIALISTHELSETJENESTEN
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.ARBEIDSGIVERPERIODE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.BEGRUNNELSE_MIN_1_ORD
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.BEGRUNNELSE_MIN_3_ORD
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.ETTERSENDING
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.FORLENGELSE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.SPESIALISTHELSETJENESTEN
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERING
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_INNTIL_30_DAGER
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_INNTIL_8_DAGER

enum class TilbakedateringRules {
    ARBEIDSGIVERPERIODE,
    BEGRUNNELSE_MIN_1_ORD,
    BEGRUNNELSE_MIN_3_ORD,
    ETTERSENDING,
    FORLENGELSE,
    SPESIALISTHELSETJENESTEN,
    TILBAKEDATERING,
    TILBAKEDATERT_INNTIL_8_DAGER,
    TILBAKEDATERT_INNTIL_30_DAGER,
}

data class TilbakedateringResult(
    val status: Status,
    val ruleHit: RuleHit?
)

val tilbakedateringRuleTree = tree<TilbakedateringRules, TilbakedateringResult>(TILBAKEDATERING) {
    yes(ETTERSENDING) {
        yes(OK)
        no(TILBAKEDATERT_INNTIL_8_DAGER) {
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
            no(TILBAKEDATERT_INNTIL_30_DAGER) {
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
    no(OK)
}

internal fun RuleNode<TilbakedateringRules, TilbakedateringResult>.yes(status: Status, ruleHit: RuleHit? = null) {
    yes(TilbakedateringResult(status, ruleHit))
}

internal fun RuleNode<TilbakedateringRules, TilbakedateringResult>.no(status: Status, ruleHit: RuleHit? = null) {
    no(TilbakedateringResult(status, ruleHit))
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
        TILBAKEDATERT_INNTIL_8_DAGER -> tilbakedateringInntil8Dager
        TILBAKEDATERT_INNTIL_30_DAGER -> tilbakedateringInntil30Dager
    }
}
