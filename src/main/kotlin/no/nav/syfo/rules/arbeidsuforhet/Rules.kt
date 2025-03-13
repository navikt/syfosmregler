package no.nav.syfo.rules.arbeidsuforhet

import no.nav.helse.diagnosekoder.Diagnosekoder
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.RuleResult

typealias Rule<T> = (sykmelding: Sykmelding, ruleMetadata: RuleMetadata) -> RuleResult<T>

typealias ArbeidsuforhetRule = Rule<ArbeidsuforhetRules>

val icpc2ZDiagnose: ArbeidsuforhetRule = { sykmelding, _ ->
    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose

    val icpc2ZDiagnose =
        hoveddiagnose != null && hoveddiagnose.isICPC2() && hoveddiagnose.kode.startsWith("Z")

    RuleResult(
        ruleInputs = mapOf("icpc2ZDiagnose" to icpc2ZDiagnose),
        rule = ArbeidsuforhetRules.ICPC_2_Z_DIAGNOSE,
        ruleResult = icpc2ZDiagnose,
    )
}

val manglerHovedDiagnose: ArbeidsuforhetRule = { sykmelding, _ ->
    val hovedDiagnose = sykmelding.medisinskVurdering.hovedDiagnose

    RuleResult(
        ruleInputs =
            mapOf("hovedDiagnose" to hovedDiagnose),
        rule = ArbeidsuforhetRules.HOVEDDIAGNOSE_MANGLER,
        ruleResult = hovedDiagnose == null,
    )
}

val manglerAnnenFravarsArsak: ArbeidsuforhetRule = { sykmelding, _ ->
    val annenFraversArsak = sykmelding.medisinskVurdering.annenFraversArsak

    val fraversgrunnMangler =
        (annenFraversArsak?.let { it.grunn.isEmpty() && it.beskrivelse.isNullOrBlank() } ?: true)

    RuleResult(
        ruleInputs = mapOf("annenFraversArsak" to annenFraversArsak),
        rule = ArbeidsuforhetRules.FRAVAERSGRUNN_MANGLER,
        ruleResult = fraversgrunnMangler,
    )
}

val ugyldigKodeVerkHovedDiagnose: ArbeidsuforhetRule = { sykmelding, _ ->
    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose

    val ugyldigKodeverkHovedDiagnose =
        (hoveddiagnose?.system !in arrayOf(Diagnosekoder.ICPC2_CODE, Diagnosekoder.ICD10_CODE) ||
            hoveddiagnose?.let { diagnose ->
                if (diagnose.isICPC2()) {
                    Diagnosekoder.icpc2.containsKey(diagnose.kode)
                } else {
                    Diagnosekoder.icd10.containsKey(diagnose.kode)
                }
            } != true)

    RuleResult(
        ruleInputs = mapOf("ugyldigKodeverkHovedDiagnose" to ugyldigKodeverkHovedDiagnose),
        rule = ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE,
        ruleResult = ugyldigKodeverkHovedDiagnose,
    )
}

val ugyldigKodeVerkBiDiagnose: ArbeidsuforhetRule = { sykmelding, _ ->
    val biDiagnoser = sykmelding.medisinskVurdering.biDiagnoser

    val ugyldigKodeVerkBiDiagnose =
        !biDiagnoser.all { diagnose ->
            if (diagnose.isICPC2()) {
                Diagnosekoder.icpc2.containsKey(diagnose.kode)
            } else {
                Diagnosekoder.icd10.containsKey(diagnose.kode)
            }
        }

    RuleResult(
        ruleInputs = mapOf("biDiagnoser" to biDiagnoser),
        rule = ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE,
        ruleResult = ugyldigKodeVerkBiDiagnose,
    )
}

private fun Diagnose.isICPC2(): Boolean = system == Diagnosekoder.ICPC2_CODE
