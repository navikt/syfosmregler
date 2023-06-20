package no.nav.syfo.rules.tilbakedateringSignatur

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.MANUAL_PROCESSING
import no.nav.syfo.model.Status.OK
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRuleHit.INNTIL_30_DAGER
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRuleHit.INNTIL_30_DAGER_MED_BEGRUNNELSE
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRuleHit.INNTIL_8_DAGER
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRuleHit.OVER_30_DAGER
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRuleHit.OVER_30_DAGER_MED_BEGRUNNELSE
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRuleHit.OVER_30_DAGER_SPESIALISTHELSETJENESTEN
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.ARBEIDSGIVERPERIODE
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.BEGRUNNELSE_MIN_1_ORD
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.BEGRUNNELSE_MIN_3_ORD
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.ETTERSENDING
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.FORLENGELSE
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.SPESIALISTHELSETJENESTEN
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.TILBAKEDATERING
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.TILBAKEDATERT_INNTIL_30_DAGER
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.TILBAKEDATERT_INNTIL_8_DAGER

// 1. ListMedRegler
// 2. forEach (if status != OK) return rulehit
// 3. return OK

enum class TilbakedateringSignaturRules {
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

val tilbakedateringSignaturRuleTree =
    tree<TilbakedateringSignaturRules, RuleResult>(TILBAKEDATERING) {
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

internal fun RuleNode<TilbakedateringSignaturRules, RuleResult>.yes(
    status: Status,
    ruleHit: TilbakedateringSignaturRuleHit? = null,
) {
    yes(RuleResult(status = status, ruleHit = ruleHit?.ruleHit))
}

internal fun RuleNode<TilbakedateringSignaturRules, RuleResult>.no(
    status: Status,
    ruleHit: TilbakedateringSignaturRuleHit? = null,
) {
    no(RuleResult(status = status, ruleHit = ruleHit?.ruleHit))
}

fun getRule(rules: TilbakedateringSignaturRules): Rule<TilbakedateringSignaturRules> {
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
