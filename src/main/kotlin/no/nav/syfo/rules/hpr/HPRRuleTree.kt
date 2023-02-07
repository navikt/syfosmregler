package no.nav.syfo.rules.hpr

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.OK
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree

enum class HPRRules {
    BEHANDLER_IKKE_GYLDIG_I_HPR,
    BEHANDLER_MANGLER_AUTORISASJON_I_HPR,
    BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR,
    BEHANDLER_MT_FT_KI_OVER_12_UKER
}

data class HPRResult(
    val status: Status,
    val ruleHit: RuleHit?
) {
    override fun toString(): String {
        return status.name + (ruleHit?.let { "->${it.name}" } ?: "")
    }
}

val hprRuleTree = tree<HPRRules, HPRResult>(HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR) {
    yes(Status.INVALID, RuleHit.BEHANDLER_IKKE_GYLDIG_I_HPR)
    no(HPRRules.BEHANDLER_MANGLER_AUTORISASJON_I_HPR) {
        yes(Status.INVALID, RuleHit.BEHANDLER_MANGLER_AUTORISASJON_I_HPR)
        no(HPRRules.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR) {
            yes(Status.INVALID, RuleHit.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR)
            no(HPRRules.BEHANDLER_MT_FT_KI_OVER_12_UKER) {
                yes(Status.INVALID, RuleHit.BEHANDLER_MT_FT_KI_OVER_12_UKER)
                no(OK)
            }
        }
    }
}

internal fun RuleNode<HPRRules, HPRResult>.yes(status: Status, ruleHit: RuleHit? = null) {
    yes(HPRResult(status, ruleHit))
}

internal fun RuleNode<HPRRules, HPRResult>.no(status: Status, ruleHit: RuleHit? = null) {
    no(HPRResult(status, ruleHit))
}

fun getRule(rules: HPRRules): Rule<HPRRules> {
    return when (rules) {
        HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR -> behanderIkkeGyldigHPR
        HPRRules.BEHANDLER_MANGLER_AUTORISASJON_I_HPR -> behandlerManglerAutorisasjon
        HPRRules.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR -> behandlerIkkeLEKIMTTLFT
        HPRRules.BEHANDLER_MT_FT_KI_OVER_12_UKER -> behandlerMTFTKISykmeldtOver12Uker
    }
}
