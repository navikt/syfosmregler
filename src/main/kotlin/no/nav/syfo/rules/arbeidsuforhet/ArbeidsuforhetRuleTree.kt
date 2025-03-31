package no.nav.syfo.rules.arbeidsuforhet

import no.nav.syfo.model.Status
import no.nav.syfo.model.Status.INVALID
import no.nav.syfo.model.Status.OK
import no.nav.syfo.model.juridisk.JuridiskEnum
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.tree

enum class ArbeidsuforhetRules {
    ICPC_2_Z_DIAGNOSE,
    HOVEDDIAGNOSE_MANGLER,
    FRAVAERSGRUNN_MANGLER,
    UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE,
    UGYLDIG_KODEVERK_FOR_BIDIAGNOSE,
}

val arbeidsuforhetRuleTree =
    tree<ArbeidsuforhetRules, RuleResult>(ArbeidsuforhetRules.HOVEDDIAGNOSE_MANGLER) {
        yes(ArbeidsuforhetRules.FRAVAERSGRUNN_MANGLER) {
            yes(
                INVALID,
                JuridiskEnum.FOLKETRYGDLOVEN_8_4_1,
                ArbeidsuforhetRuleHit.FRAVAERSGRUNN_MANGLER
            )
            no(ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE) {
                yes(
                    INVALID,
                    JuridiskEnum.FOLKETRYGDLOVEN_8_4_1,
                    ArbeidsuforhetRuleHit.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE
                )
                no(OK, JuridiskEnum.FOLKETRYGDLOVEN_8_4_1)
            }
        }
        no(ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE) {
            yes(
                INVALID,
                JuridiskEnum.FOLKETRYGDLOVEN_8_4_1,
                ArbeidsuforhetRuleHit.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE
            )
            no(ArbeidsuforhetRules.ICPC_2_Z_DIAGNOSE) {
                yes(
                    INVALID,
                    JuridiskEnum.FOLKETRYGDLOVEN_8_4_1,
                    ArbeidsuforhetRuleHit.ICPC_2_Z_DIAGNOSE
                )
                no(ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE) {
                    yes(
                        INVALID,
                        JuridiskEnum.FOLKETRYGDLOVEN_8_4_1,
                        ArbeidsuforhetRuleHit.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE
                    )
                    no(OK, JuridiskEnum.FOLKETRYGDLOVEN_8_4_1)
                }
            }
        }
    }

internal fun RuleNode<ArbeidsuforhetRules, RuleResult>.yes(
    status: Status,
    juridisk: JuridiskEnum,
    ruleHit: ArbeidsuforhetRuleHit? = null
) {
    yes(RuleResult(status, juridisk.JuridiskHenvisning, ruleHit?.ruleHit))
}

internal fun RuleNode<ArbeidsuforhetRules, RuleResult>.no(
    status: Status,
    juridisk: JuridiskEnum,
    ruleHit: ArbeidsuforhetRuleHit? = null
) {
    no(RuleResult(status, juridisk.JuridiskHenvisning, ruleHit?.ruleHit))
}

fun getRule(rules: ArbeidsuforhetRules): Rule<ArbeidsuforhetRules> {
    return when (rules) {
        ArbeidsuforhetRules.ICPC_2_Z_DIAGNOSE -> icpc2ZDiagnose
        ArbeidsuforhetRules.HOVEDDIAGNOSE_MANGLER -> manglerHovedDiagnose
        ArbeidsuforhetRules.FRAVAERSGRUNN_MANGLER -> manglerAnnenFravarsArsak
        ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE -> ugyldigKodeVerkHovedDiagnose
        ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE -> ugyldigKodeVerkBiDiagnose
    }
}
