package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.MANUAL_PROCESSING
import no.nav.syfo.model.Status.OK
import no.nav.syfo.rules.dsl.tree
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

val tilbakedateringRuleTree = tree(TILBAKEDATERING) {
    yes(ETTERSENDING) {
        yes(OK)
        no(TILBAKEDATERT_INNTIL_8_DAGER) {
            yes(BEGRUNNELSE_MIN_1_ORD) {
                yes(OK)
                no(FORLENGELSE) {
                    yes(OK)
                    no(SPESIALISTHELSETJENESTEN) {
                        yes(OK)
                        no(INVALID)
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
                                no(MANUAL_PROCESSING)
                            }
                        }
                    }
                    no(SPESIALISTHELSETJENESTEN) {
                        yes(OK)
                        no(INVALID)
                    }
                }
                no(BEGRUNNELSE_MIN_3_ORD) {
                    yes(MANUAL_PROCESSING)
                    no(SPESIALISTHELSETJENESTEN) {
                        yes(MANUAL_PROCESSING)
                        no(INVALID)
                    }
                }
            }
        }
    }
    no(OK)
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
        TILBAKEDATERT_INNTIL_30_DAGER -> tilbakedateringInntil31Dager
    }
}
