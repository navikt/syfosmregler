package no.nav.syfo.rules.periodlogic

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.OK
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree

enum class PeriodLogicRules {
    PERIODER_MANGLER,
    FRADATO_ETTER_TILDATO,
    OVERLAPPENDE_PERIODER,
    OPPHOLD_MELLOM_PERIODER,
    IKKE_DEFINERT_PERIODE,
    TILBAKEDATERT_MER_ENN_3_AR,
    FREMDATERT,
    TOTAL_VARIGHET_OVER_ETT_AAR,
    BEHANDLINGSDATO_ETTER_MOTTATTDATO,
    AVVENTENDE_SYKMELDING_KOMBINERT,
    MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER,
    AVVENTENDE_SYKMELDING_OVER_16_DAGER,
    FOR_MANGE_BEHANDLINGSDAGER_PER_UKE,
    GRADERT_SYKMELDING_UNDER_20_PROSENT,
    GRADERT_SYKMELDING_OVER_99_PROSENT,
    SYKMELDING_MED_BEHANDLINGSDAGER
}

data class PeriodLogicResult(
    val status: Status,
    val ruleHit: RuleHit?
) {
    override fun toString(): String {
        return status.name + (ruleHit?.let { "->${it.name}" } ?: "")
    }
}

// TODO impl ruletree
val periodLogicRuleTree = tree<PeriodLogicRules, PeriodLogicResult>(PeriodLogicRules.PERIODER_MANGLER) {
    yes(Status.INVALID, RuleHit.PERIODER_MANGLER)
    no(OK)
}

internal fun RuleNode<PeriodLogicRules, PeriodLogicResult>.yes(status: Status, ruleHit: RuleHit? = null) {
    yes(PeriodLogicResult(status, ruleHit))
}

internal fun RuleNode<PeriodLogicRules, PeriodLogicResult>.no(status: Status, ruleHit: RuleHit? = null) {
    no(PeriodLogicResult(status, ruleHit))
}

// TODO impl rules
fun getRule(rules: PeriodLogicRules): Rule<PeriodLogicRules> {
    return when (rules) {
        PeriodLogicRules.PERIODER_MANGLER -> periodeMangler
        PeriodLogicRules.FRADATO_ETTER_TILDATO -> fraDatoEtterTilDato
        PeriodLogicRules.OVERLAPPENDE_PERIODER -> overlappendePerioder
        PeriodLogicRules.OPPHOLD_MELLOM_PERIODER -> periodeMangler
        PeriodLogicRules.IKKE_DEFINERT_PERIODE -> periodeMangler
        PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR -> periodeMangler
        PeriodLogicRules.FREMDATERT -> periodeMangler
        PeriodLogicRules.TOTAL_VARIGHET_OVER_ETT_AAR -> periodeMangler
        PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO -> periodeMangler
        PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT -> periodeMangler
        PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER -> periodeMangler
        PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER -> periodeMangler
        PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE -> periodeMangler
        PeriodLogicRules.GRADERT_SYKMELDING_UNDER_20_PROSENT -> periodeMangler
        PeriodLogicRules.GRADERT_SYKMELDING_OVER_99_PROSENT -> periodeMangler
        PeriodLogicRules.SYKMELDING_MED_BEHANDLINGSDAGER -> periodeMangler
    }
}
