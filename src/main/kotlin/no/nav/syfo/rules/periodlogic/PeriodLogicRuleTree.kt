package no.nav.syfo.rules.periodlogic

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.MANUAL_PROCESSING
import no.nav.syfo.model.Status.OK
import no.nav.syfo.rules.common.RuleResult
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

val periodLogicRuleTree = tree<PeriodLogicRules, RuleResult>(PeriodLogicRules.PERIODER_MANGLER) {
    yes(INVALID, PeriodLogicRuleHit.PERIODER_MANGLER)
    no(PeriodLogicRules.FRADATO_ETTER_TILDATO) {
        yes(INVALID, PeriodLogicRuleHit.FRADATO_ETTER_TILDATO)
        no(PeriodLogicRules.OVERLAPPENDE_PERIODER) {
            yes(INVALID, PeriodLogicRuleHit.OVERLAPPENDE_PERIODER)
            no(PeriodLogicRules.OPPHOLD_MELLOM_PERIODER) {
                yes(INVALID, PeriodLogicRuleHit.OPPHOLD_MELLOM_PERIODER)
                no(PeriodLogicRules.IKKE_DEFINERT_PERIODE) {
                    yes(INVALID, PeriodLogicRuleHit.IKKE_DEFINERT_PERIODE)
                    no(PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR) {
                        yes(INVALID, PeriodLogicRuleHit.TILBAKEDATERT_MER_ENN_3_AR)
                        no(PeriodLogicRules.FREMDATERT) {
                            yes(INVALID, PeriodLogicRuleHit.FREMDATERT)
                            no(PeriodLogicRules.TOTAL_VARIGHET_OVER_ETT_AAR) {
                                yes(INVALID, PeriodLogicRuleHit.TOTAL_VARIGHET_OVER_ETT_AAR)
                                no(PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO) {
                                    yes(INVALID, PeriodLogicRuleHit.BEHANDLINGSDATO_ETTER_MOTTATTDATO)
                                    no(PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT) {
                                        yes(INVALID, PeriodLogicRuleHit.AVVENTENDE_SYKMELDING_KOMBINERT)
                                        no(PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER) {
                                            yes(INVALID, PeriodLogicRuleHit.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER)
                                            no(PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER) {
                                                yes(INVALID, PeriodLogicRuleHit.AVVENTENDE_SYKMELDING_OVER_16_DAGER)
                                                no(PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE) {
                                                    yes(INVALID, PeriodLogicRuleHit.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE)
                                                    no(PeriodLogicRules.GRADERT_SYKMELDING_UNDER_20_PROSENT) {
                                                        yes(INVALID, PeriodLogicRuleHit.GRADERT_SYKMELDING_UNDER_20_PROSENT)
                                                        no(PeriodLogicRules.GRADERT_SYKMELDING_OVER_99_PROSENT) {
                                                            yes(INVALID, PeriodLogicRuleHit.GRADERT_SYKMELDING_OVER_99_PROSENT)
                                                            no(PeriodLogicRules.SYKMELDING_MED_BEHANDLINGSDAGER) {
                                                                yes(
                                                                    MANUAL_PROCESSING,
                                                                    PeriodLogicRuleHit.SYKMELDING_MED_BEHANDLINGSDAGER
                                                                )
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
            }
        }
    }
}

internal fun RuleNode<PeriodLogicRules, RuleResult>.yes(status: Status, ruleHit: PeriodLogicRuleHit? = null) {
    yes(RuleResult(status, ruleHit?.ruleHit))
}

internal fun RuleNode<PeriodLogicRules, RuleResult>.no(status: Status, ruleHit: PeriodLogicRuleHit? = null) {
    no(RuleResult(status, ruleHit?.ruleHit))
}

fun getRule(rules: PeriodLogicRules): Rule<PeriodLogicRules> {
    return when (rules) {
        PeriodLogicRules.PERIODER_MANGLER -> periodeMangler
        PeriodLogicRules.FRADATO_ETTER_TILDATO -> fraDatoEtterTilDato
        PeriodLogicRules.OVERLAPPENDE_PERIODER -> overlappendePerioder
        PeriodLogicRules.OPPHOLD_MELLOM_PERIODER -> oppholdMellomPerioder
        PeriodLogicRules.IKKE_DEFINERT_PERIODE -> ikkeDefinertPeriode
        PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR -> tilbakeDatertMerEnn3AAr
        PeriodLogicRules.FREMDATERT -> fremdatertOver30Dager
        PeriodLogicRules.TOTAL_VARIGHET_OVER_ETT_AAR -> varighetOver1AAr
        PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO -> behandslingsDatoEtterMottatDato
        PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT -> avventendeKombinert
        PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER -> manglendeInnspillArbeidsgiver
        PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER -> avventendeOver16Dager
        PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE -> forMangeBehandlingsDagerPrUke
        PeriodLogicRules.GRADERT_SYKMELDING_UNDER_20_PROSENT -> gradertUnder20Prosent
        PeriodLogicRules.GRADERT_SYKMELDING_OVER_99_PROSENT -> gradertOver99Prosent
        PeriodLogicRules.SYKMELDING_MED_BEHANDLINGSDAGER -> inneholderBehandlingsDager
    }
}
