package no.nav.syfo.rules.patientageover70

import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.services.sortedFOMDate

typealias Rule<T> = (sykmelding: Sykmelding, ruleMetadata: RuleMetadata) -> RuleResult<T>
typealias PatientAgeOver70Rule = Rule<PatientAgeOver70Rules>

val pasientOver70Aar: PatientAgeOver70Rule = { sykmelding, ruleMetadata ->
    val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
    val pasientFodselsdato = ruleMetadata.pasientFodselsdato

    val pasientOver70Aar = forsteFomDato > pasientFodselsdato.plusYears(70)

    RuleResult(
        ruleInputs = mapOf("pasientOver70Aar" to pasientOver70Aar),
        rule = PatientAgeOver70Rules.PASIENT_ELDRE_ENN_70,
        ruleResult = pasientOver70Aar
    )
}
