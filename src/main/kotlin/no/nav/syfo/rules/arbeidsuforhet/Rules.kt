package no.nav.syfo.rules.arbeidsuforhet

import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.sm.Diagnosekoder
import no.nav.syfo.sm.isICPC2

typealias Rule<T> = (sykmelding: Sykmelding, ruleMetadata: RuleMetadata) -> RuleResult<T>

typealias ArbeidsuforhetRule = Rule<ArbeidsuforhetRules>

val ukjentDiagnoseKodeType: ArbeidsuforhetRule = { sykmelding, _ ->
    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose

    val ukjentDiagnoseKodeType = hoveddiagnose != null && hoveddiagnose.system !in Diagnosekoder

    RuleResult(
        ruleInputs = mapOf("ukjentDiagnoseKodeType" to ukjentDiagnoseKodeType),
        rule = ArbeidsuforhetRules.UKJENT_DIAGNOSEKODETYPE,
        ruleResult = ukjentDiagnoseKodeType,
    )
}

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

val houvedDiagnoseEllerFraversgrunnMangler: ArbeidsuforhetRule = { sykmelding, _ ->
    val annenFraversArsak = sykmelding.medisinskVurdering.annenFraversArsak
    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose

    val houvedDiagnoseEllerFraversgrunnMangler = annenFraversArsak == null && hoveddiagnose == null

    RuleResult(
        ruleInputs =
            mapOf(
                "houvedDiagnoseEllerFraversgrunnMangler" to houvedDiagnoseEllerFraversgrunnMangler
            ),
        rule = ArbeidsuforhetRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER,
        ruleResult = houvedDiagnoseEllerFraversgrunnMangler,
    )
}

val ugyldigKodeVerkHouvedDiagnose: ArbeidsuforhetRule = { sykmelding, _ ->
    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose
    val annenFravarsArsak = sykmelding.medisinskVurdering.annenFraversArsak

    val ugyldigKodeVerkHouvedDiagnose =
        (hoveddiagnose?.system !in arrayOf(Diagnosekoder.ICPC2_CODE, Diagnosekoder.ICD10_CODE) ||
            hoveddiagnose?.let { diagnose ->
                if (diagnose.isICPC2()) {
                    Diagnosekoder.icpc2.containsKey(diagnose.kode)
                } else {
                    Diagnosekoder.icd10.containsKey(diagnose.kode)
                }
            } != true) && annenFravarsArsak == null

    RuleResult(
        ruleInputs = mapOf("ugyldigKodeVerkHouvedDiagnose" to ugyldigKodeVerkHouvedDiagnose),
        rule = ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE,
        ruleResult = ugyldigKodeVerkHouvedDiagnose,
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
        ruleInputs = mapOf("ugyldigKodeVerkBiDiagnose" to ugyldigKodeVerkBiDiagnose),
        rule = ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE,
        ruleResult = ugyldigKodeVerkBiDiagnose,
    )
}
