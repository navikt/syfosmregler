package no.nav.syfo.rules.hpr

import HPRRule
import behanderGyldigHPR
import behandlerErFTMedTilligskompetanseSykmelding
import behandlerErKIMedTilligskompetanseSykmelding
import behandlerErLege
import behandlerErManuellterapeut
import behandlerErTannlege
import behandlerHarAutorisasjon
import no.nav.syfo.model.Status
import no.nav.syfo.model.juridisk.JuridiskEnum
import no.nav.syfo.model.juridisk.JuridiskHenvisning
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree
import sykefravarOver12Uker

enum class HPRRules {
    BEHANDLER_GYLIDG_I_HPR,
    BEHANDLER_HAR_AUTORISASJON_I_HPR,
    BEHANDLER_ER_LEGE_I_HPR,
    BEHANDLER_ER_TANNLEGE_I_HPR,
    BEHANDLER_ER_MANUELLTERAPEUT_I_HPR,
    BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR,
    BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR,
    SYKEFRAVAR_OVER_12_UKER,
}

val hprRuleTree =
    tree<HPRRules, RuleResult>(HPRRules.BEHANDLER_GYLIDG_I_HPR) {
        no(
            Status.INVALID,
            JuridiskEnum.FOLKETRYGDLOVEN_8_7_1,
            HPRRuleHit.BEHANDLER_IKKE_GYLDIG_I_HPR,
        )
        yes(HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR) {
            no(
                Status.INVALID,
                JuridiskEnum.FOLKETRYGDLOVEN_8_7_1,
                HPRRuleHit.BEHANDLER_MANGLER_AUTORISASJON_I_HPR
            )
            yes(HPRRules.BEHANDLER_ER_LEGE_I_HPR) {
                yes(Status.OK, JuridiskEnum.FOLKETRYGDLOVEN_8_7_1)
                no(HPRRules.BEHANDLER_ER_TANNLEGE_I_HPR) {
                    yes(Status.OK, JuridiskEnum.FOLKETRYGDLOVEN_8_7_1)
                    no(HPRRules.BEHANDLER_ER_MANUELLTERAPEUT_I_HPR) {
                        yes(HPRRules.SYKEFRAVAR_OVER_12_UKER, checkSykefravarOver12Uker())
                        no(HPRRules.BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR) {
                            yes(HPRRules.SYKEFRAVAR_OVER_12_UKER, checkSykefravarOver12Uker())
                            no(HPRRules.BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR) {
                                yes(HPRRules.SYKEFRAVAR_OVER_12_UKER, checkSykefravarOver12Uker())
                                no(
                                    Status.INVALID,
                                    JuridiskEnum.FOLKETRYGDLOVEN_8_7_1,
                                    HPRRuleHit.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR
                                )
                            }
                        }
                    }
                }
            }
        }
    }

private fun checkSykefravarOver12Uker(): RuleNode<HPRRules, RuleResult>.() -> Unit = {
    yes(
        Status.INVALID,
        JuridiskEnum.FOLKETRYGDLOVEN_8_7_1,
        HPRRuleHit.BEHANDLER_MT_FT_KI_OVER_12_UKER
    )
    no(Status.OK, JuridiskEnum.FOLKETRYGDLOVEN_8_7_1)
}

internal fun RuleNode<HPRRules, RuleResult>.yes(
    status: Status,
    juridisk: JuridiskEnum,
    ruleHit: HPRRuleHit? = null
) {
    yes(RuleResult(status, juridisk.JuridiskHenvisning, ruleHit?.ruleHit))
}

internal fun RuleNode<HPRRules, RuleResult>.no(
    status: Status,
    juridisk: JuridiskEnum,
    ruleHit: HPRRuleHit? = null
) {
    no(RuleResult(status, juridisk.JuridiskHenvisning, ruleHit?.ruleHit))
}

fun getRule(rules: HPRRules): HPRRule {
    return when (rules) {
        HPRRules.BEHANDLER_GYLIDG_I_HPR -> behanderGyldigHPR(rules)
        HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR -> behandlerHarAutorisasjon(rules)
        HPRRules.BEHANDLER_ER_LEGE_I_HPR -> behandlerErLege(rules)
        HPRRules.BEHANDLER_ER_TANNLEGE_I_HPR -> behandlerErTannlege(rules)
        HPRRules.BEHANDLER_ER_MANUELLTERAPEUT_I_HPR -> behandlerErManuellterapeut(rules)
        HPRRules.BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR ->
            behandlerErFTMedTilligskompetanseSykmelding(rules)
        HPRRules.BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR ->
            behandlerErKIMedTilligskompetanseSykmelding(rules)
        HPRRules.SYKEFRAVAR_OVER_12_UKER -> sykefravarOver12Uker(rules)
    }
}
