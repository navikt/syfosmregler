package no.nav.syfo.rules.validation

import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.rules.sortedFOMDate
import no.nav.syfo.rules.sortedTOMDate

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

// TODO
