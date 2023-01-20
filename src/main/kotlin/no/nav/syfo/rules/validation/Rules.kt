package no.nav.syfo.rules.validation

import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.questions.QuestionGroup
import no.nav.syfo.rules.containsAnswersFor
import no.nav.syfo.rules.daysBetween
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.rules.sortedFOMDate
import no.nav.syfo.rules.sortedTOMDate
import no.nav.syfo.sm.Diagnosekoder
import no.nav.syfo.sm.isICPC2

typealias Rule<T> = (sykmelding: Sykmelding, ruleMetadata: RuleMetadata) -> RuleResult<T>
typealias ValidationRule = Rule<ValidationRules>

val pasientUnder13Aar: ValidationRule = { sykmelding, ruleMetadata ->

    val sisteTomDato = sykmelding.perioder.sortedTOMDate().last()
    val pasientFodselsdato = ruleMetadata.pasientFodselsdato

    val pasientUnder13Aar = sisteTomDato < pasientFodselsdato.plusYears(13)

    RuleResult(
        ruleInputs = mapOf("pasientUnder13Aar" to pasientUnder13Aar),
        rule = ValidationRules.PASIENT_YNGRE_ENN_13,
        ruleResult = pasientUnder13Aar
    )
}

val pasientOver70Aar: ValidationRule = { sykmelding, ruleMetadata ->
    val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
    val pasientFodselsdato = ruleMetadata.pasientFodselsdato

    val pasientOver70Aar = forsteFomDato > pasientFodselsdato.plusYears(70)

    RuleResult(
        ruleInputs = mapOf("pasientOver70Aar" to pasientOver70Aar),
        rule = ValidationRules.PASIENT_ELDRE_ENN_70,
        ruleResult = pasientOver70Aar
    )
}

val ukjentDiagnoseKodeType: ValidationRule = { sykmelding, _ ->
    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose

    val ukjentDiagnoseKodeType = hoveddiagnose != null && hoveddiagnose.system !in Diagnosekoder

    RuleResult(
        ruleInputs = mapOf("ukjentDiagnoseKodeType" to ukjentDiagnoseKodeType),
        rule = ValidationRules.UKJENT_DIAGNOSEKODETYPE,
        ruleResult = ukjentDiagnoseKodeType
    )
}

val icpc2ZDiagnose: ValidationRule = { sykmelding, _ ->
    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose

    val icpc2ZDiagnose = hoveddiagnose != null && hoveddiagnose.isICPC2() && hoveddiagnose.kode.startsWith("Z")

    RuleResult(
        ruleInputs = mapOf("icpc2ZDiagnose" to icpc2ZDiagnose),
        rule = ValidationRules.ICPC_2_Z_DIAGNOSE,
        ruleResult = icpc2ZDiagnose
    )
}

val houvedDiagnoseEllerFraversgrunnMangler: ValidationRule = { sykmelding, _ ->
    val annenFraversArsak = sykmelding.medisinskVurdering.annenFraversArsak
    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose

    val houvedDiagnoseEllerFraversgrunnMangler = annenFraversArsak == null && hoveddiagnose == null

    RuleResult(
        ruleInputs = mapOf("houvedDiagnoseEllerFraversgrunnMangler" to houvedDiagnoseEllerFraversgrunnMangler),
        rule = ValidationRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER,
        ruleResult = houvedDiagnoseEllerFraversgrunnMangler
    )
}

val ugyldigKodeVerkHouvedDiagnose: ValidationRule = { sykmelding, _ ->
    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose
    val annenFravarsArsak = sykmelding.medisinskVurdering.annenFraversArsak

    val ugyldigKodeVerkHouvedDiagnose = (
            hoveddiagnose?.system !in arrayOf(Diagnosekoder.ICPC2_CODE, Diagnosekoder.ICD10_CODE) ||
                    hoveddiagnose?.let { diagnose ->
                        if (diagnose.isICPC2()) {
                            Diagnosekoder.icpc2.containsKey(diagnose.kode)
                        } else {
                            Diagnosekoder.icd10.containsKey(diagnose.kode)
                        }
                    } != true
            ) && annenFravarsArsak == null

    RuleResult(
        ruleInputs = mapOf("ugyldigKodeVerkHouvedDiagnose" to ugyldigKodeVerkHouvedDiagnose),
        rule = ValidationRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE,
        ruleResult = ugyldigKodeVerkHouvedDiagnose
    )
}

val ugyldigKodeVerkBiDiagnose: ValidationRule = { sykmelding, _ ->
    val biDiagnoser = sykmelding.medisinskVurdering.biDiagnoser

    val ugyldigKodeVerkBiDiagnose = !biDiagnoser.all { diagnose ->
        if (diagnose.isICPC2()) {
            Diagnosekoder.icpc2.containsKey(diagnose.kode)
        } else {
            Diagnosekoder.icd10.containsKey(diagnose.kode)
        }
    }

    RuleResult(
        ruleInputs = mapOf("ugyldigKodeVerkBiDiagnose" to ugyldigKodeVerkBiDiagnose),
        rule = ValidationRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE,
        ruleResult = ugyldigKodeVerkBiDiagnose
    )
}

val ugyldigRegelsettversjon: ValidationRule = { sykmelding, ruleMetadata ->
    val rulesetVersion = ruleMetadata.rulesetVersion


    val ugyldigRegelsettversjon = rulesetVersion !in arrayOf(null, "", "1", "2", "3")

    RuleResult(
        ruleInputs = mapOf("ugyldigRegelsettversjon" to ugyldigRegelsettversjon),
        rule = ValidationRules.UGYLDIG_REGELSETTVERSJON,
        ruleResult = ugyldigRegelsettversjon
    )
}

val avsenderSammeSomPasient: ValidationRule = { sykmelding, ruleMetadata ->
    val avsenderFnr = ruleMetadata.avsenderFnr
    val patientPersonNumber = ruleMetadata.patientPersonNumber

    val avsenderSammeSomPasient = avsenderFnr == patientPersonNumber

    RuleResult(
        ruleInputs = mapOf("avsenderSammeSomPasient" to avsenderSammeSomPasient),
        rule = ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR,
        ruleResult = avsenderSammeSomPasient
    )
}

val behandlerSammeSomPasient: ValidationRule = { sykmelding, ruleMetadata ->
    val behandlerFnr = sykmelding.behandler.fnr
    val pasientFodselsNummer = ruleMetadata.patientPersonNumber

    val behandlerSammeSomPasient = behandlerFnr == pasientFodselsNummer

    RuleResult(
        ruleInputs = mapOf("behandlerSammeSomPasient" to behandlerSammeSomPasient),
        rule = ValidationRules.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR,
        ruleResult = behandlerSammeSomPasient
    )
}

val ugyldingOrgNummerLengde: ValidationRule = { sykmelding, ruleMetadata ->
    val legekontorOrgnr = ruleMetadata.legekontorOrgnr

    val ugyldingOrgNummerLengde = legekontorOrgnr != null && legekontorOrgnr.length != 9

    RuleResult(
        ruleInputs = mapOf("ugyldingOrgNummerLengde" to ugyldingOrgNummerLengde),
        rule = ValidationRules.UGYLDIG_ORGNR_LENGDE,
        ruleResult = ugyldingOrgNummerLengde
    )
}

val manglendeDynamiskesporsmaalversjon2uke39: ValidationRule = { sykmelding, ruleMetadata ->
    val rulesetVersion = ruleMetadata.rulesetVersion
    val sykmeldingPerioder = sykmelding.perioder
    val utdypendeOpplysinger = sykmelding.utdypendeOpplysninger

    val manglendeDynamiskesporsmaalversjon2uke39 = rulesetVersion in arrayOf("2") &&
            sykmeldingPerioder.any { (it.fom..it.tom).daysBetween() > 273 } &&
            utdypendeOpplysinger.containsAnswersFor(QuestionGroup.GROUP_6_5) != true

    RuleResult(
        ruleInputs = mapOf("manglendeDynamiskesporsmaalversjon2uke39" to manglendeDynamiskesporsmaalversjon2uke39),
        rule = ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39,
        ruleResult = manglendeDynamiskesporsmaalversjon2uke39
    )
}