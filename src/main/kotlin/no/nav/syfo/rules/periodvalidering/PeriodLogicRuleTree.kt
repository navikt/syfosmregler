package no.nav.syfo.rules.periodvalidering

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.MANUAL_PROCESSING
import no.nav.syfo.model.Status.OK
import no.nav.syfo.model.juridisk.JuridiskEnum
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree

enum class PeriodLogicRules {
    PERIODER_MANGLER,
    FRADATO_ETTER_TILDATO,
    OVERLAPPENDE_PERIODER,
    OPPHOLD_MELLOM_PERIODER,
    IKKE_DEFINERT_PERIODE,
    BEHANDLINGSDATO_ETTER_MOTTATTDATO,
    AVVENTENDE_SYKMELDING_KOMBINERT,
    MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER,
    AVVENTENDE_SYKMELDING_OVER_16_DAGER,
    FOR_MANGE_BEHANDLINGSDAGER_PER_UKE,
    GRADERT_SYKMELDING_OVER_99_PROSENT,
    SYKMELDING_MED_BEHANDLINGSDAGER,
}

val periodLogicRuleTree =
    tree<PeriodLogicRules, RuleResult>(PeriodLogicRules.PERIODER_MANGLER) {
        yes(INVALID, JuridiskEnum.INGEN, PeriodLogicRuleHit.PERIODER_MANGLER)
        no(PeriodLogicRules.FRADATO_ETTER_TILDATO) {
            yes(INVALID, JuridiskEnum.INGEN, PeriodLogicRuleHit.FRADATO_ETTER_TILDATO)
            no(PeriodLogicRules.OVERLAPPENDE_PERIODER) {
                yes(INVALID, JuridiskEnum.INGEN, PeriodLogicRuleHit.OVERLAPPENDE_PERIODER)
                no(PeriodLogicRules.OPPHOLD_MELLOM_PERIODER) {
                    yes(INVALID, JuridiskEnum.INGEN, PeriodLogicRuleHit.OPPHOLD_MELLOM_PERIODER)
                    no(PeriodLogicRules.IKKE_DEFINERT_PERIODE) {
                        yes(INVALID, JuridiskEnum.INGEN, PeriodLogicRuleHit.IKKE_DEFINERT_PERIODE)
                        no(PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO) {
                            yes(
                                INVALID,
                                JuridiskEnum.INGEN,
                                PeriodLogicRuleHit.BEHANDLINGSDATO_ETTER_MOTTATTDATO,
                            )
                            no(PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT) {
                                yes(
                                    INVALID,
                                    JuridiskEnum.INGEN,
                                    PeriodLogicRuleHit.AVVENTENDE_SYKMELDING_KOMBINERT,
                                )
                                no(PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER) {
                                    yes(
                                        INVALID,
                                        JuridiskEnum.INGEN,
                                        PeriodLogicRuleHit.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER,
                                    )
                                    no(PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER) {
                                        yes(
                                            INVALID,
                                            JuridiskEnum.INGEN,
                                            PeriodLogicRuleHit.AVVENTENDE_SYKMELDING_OVER_16_DAGER,
                                        )
                                        no(PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE) {
                                            yes(
                                                INVALID,
                                                JuridiskEnum.INGEN,
                                                PeriodLogicRuleHit
                                                    .FOR_MANGE_BEHANDLINGSDAGER_PER_UKE,
                                            )
                                            no(
                                                PeriodLogicRules.GRADERT_SYKMELDING_OVER_99_PROSENT
                                            ) {
                                                yes(
                                                    INVALID,
                                                    JuridiskEnum.INGEN,
                                                    PeriodLogicRuleHit
                                                        .GRADERT_SYKMELDING_OVER_99_PROSENT,
                                                )
                                                no(
                                                    PeriodLogicRules.SYKMELDING_MED_BEHANDLINGSDAGER
                                                ) {
                                                    yes(
                                                        MANUAL_PROCESSING,
                                                        JuridiskEnum.INGEN,
                                                        PeriodLogicRuleHit
                                                            .SYKMELDING_MED_BEHANDLINGSDAGER,
                                                    )
                                                    no(
                                                        OK,
                                                        JuridiskEnum.INGEN,
                                                    )
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

internal fun RuleNode<PeriodLogicRules, RuleResult>.yes(
    status: Status,
    juridisk: JuridiskEnum,
    ruleHit: PeriodLogicRuleHit? = null
) {
    yes(RuleResult(status, juridisk.JuridiskHenvisning, ruleHit?.ruleHit))
}

internal fun RuleNode<PeriodLogicRules, RuleResult>.no(
    status: Status,
    juridisk: JuridiskEnum,
    ruleHit: PeriodLogicRuleHit? = null
) {
    no(RuleResult(status, juridisk.JuridiskHenvisning, ruleHit?.ruleHit))
}

fun getRule(rules: PeriodLogicRules): Rule<PeriodLogicRules> {
    return when (rules) {
        PeriodLogicRules.PERIODER_MANGLER -> periodeMangler
        PeriodLogicRules.FRADATO_ETTER_TILDATO -> fraDatoEtterTilDato
        PeriodLogicRules.OVERLAPPENDE_PERIODER -> overlappendePerioder
        PeriodLogicRules.OPPHOLD_MELLOM_PERIODER -> oppholdMellomPerioder
        PeriodLogicRules.IKKE_DEFINERT_PERIODE -> ikkeDefinertPeriode
        PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO -> behandslingsDatoEtterMottatDato
        PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT -> avventendeKombinert
        PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER -> manglendeInnspillArbeidsgiver
        PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER -> avventendeOver16Dager
        PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE -> forMangeBehandlingsDagerPrUke
        PeriodLogicRules.GRADERT_SYKMELDING_OVER_99_PROSENT -> gradertOver99Prosent
        PeriodLogicRules.SYKMELDING_MED_BEHANDLINGSDAGER -> inneholderBehandlingsDager
    }
}
