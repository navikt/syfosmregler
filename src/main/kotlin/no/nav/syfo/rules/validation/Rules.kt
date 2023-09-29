package no.nav.syfo.rules.validation

import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.SporsmalSvar
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.questions.QuestionGroup
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.services.daysBetween

typealias Rule<T> = (sykmelding: Sykmelding, ruleMetadata: RuleMetadata) -> RuleResult<T>

typealias ValidationRule = Rule<ValidationRules>

val ugyldigRegelsettversjon: ValidationRule = { _, ruleMetadata ->
    val rulesetVersion = ruleMetadata.rulesetVersion

    val ugyldigRegelsettversjon = rulesetVersion !in arrayOf(null, "", "1", "2", "3")

    RuleResult(
        ruleInputs = mapOf("ugyldigRegelsettversjon" to ugyldigRegelsettversjon),
        rule = ValidationRules.UGYLDIG_REGELSETTVERSJON,
        ruleResult = ugyldigRegelsettversjon,
    )
}

val manglendeDynamiskesporsmaalversjon2uke39: ValidationRule = { sykmelding, ruleMetadata ->
    val rulesetVersion = ruleMetadata.rulesetVersion
    val sykmeldingPerioder = sykmelding.perioder
    val utdypendeOpplysinger = sykmelding.utdypendeOpplysninger

    val manglendeDynamiskesporsmaalversjon2uke39 =
        rulesetVersion in arrayOf("2") &&
            sykmeldingPerioder.any { (it.fom..it.tom).daysBetween() > 273 } &&
            utdypendeOpplysinger.containsAnswersFor(QuestionGroup.GROUP_6_5) != true

    RuleResult(
        ruleInputs =
            mapOf(
                "manglendeDynamiskesporsmaalversjon2uke39" to
                    manglendeDynamiskesporsmaalversjon2uke39
            ),
        rule = ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39,
        ruleResult = manglendeDynamiskesporsmaalversjon2uke39,
    )
}

val ugyldingOrgNummerLengde: ValidationRule = { _, ruleMetadata ->
    val legekontorOrgnr = ruleMetadata.legekontorOrgnr

    val ugyldingOrgNummerLengde = legekontorOrgnr != null && legekontorOrgnr.length != 9

    RuleResult(
        ruleInputs = mapOf("ugyldingOrgNummerLengde" to ugyldingOrgNummerLengde),
        rule = ValidationRules.UGYLDIG_ORGNR_LENGDE,
        ruleResult = ugyldingOrgNummerLengde,
    )
}

val avsenderSammeSomPasient: ValidationRule = { _, ruleMetadata ->
    val avsenderFnr = ruleMetadata.avsenderFnr
    val patientPersonNumber = ruleMetadata.patientPersonNumber

    val avsenderSammeSomPasient = avsenderFnr == patientPersonNumber

    RuleResult(
        ruleInputs = mapOf("avsenderSammeSomPasient" to avsenderSammeSomPasient),
        rule = ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR,
        ruleResult = avsenderSammeSomPasient,
    )
}

val behandlerSammeSomPasient: ValidationRule = { sykmelding, ruleMetadata ->
    val behandlerFnr = sykmelding.behandler.fnr
    val pasientFodselsNummer = ruleMetadata.patientPersonNumber

    val behandlerSammeSomPasient = behandlerFnr == pasientFodselsNummer

    RuleResult(
        ruleInputs = mapOf("behandlerSammeSomPasient" to behandlerSammeSomPasient),
        rule = ValidationRules.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR,
        ruleResult = behandlerSammeSomPasient,
    )
}

fun Map<String, Map<String, SporsmalSvar>>.containsAnswersFor(questionGroup: QuestionGroup) =
    this[questionGroup.spmGruppeId]?.all { (spmId, _) ->
        spmId in questionGroup.spmsvar.map { it.spmId }
    }
