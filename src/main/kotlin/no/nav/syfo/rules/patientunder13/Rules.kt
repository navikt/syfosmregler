package no.nav.syfo.rules.patientunder13

import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.services.RuleMetadataSykmelding
import no.nav.syfo.services.sortedTOMDate

typealias Rule<T> =
    (sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding) -> RuleResult<T>

typealias PatientAgeOver70Rule = Rule<PatientAgeUnder13Rules>

val pasientUnder13Aar: PatientAgeOver70Rule = { sykmelding, ruleMetadata ->
    val sisteTomDato = sykmelding.perioder.sortedTOMDate().last()
    val pasientFodselsdato = ruleMetadata.ruleMetadata.pasientFodselsdato

    val pasientUnder13Aar = sisteTomDato < pasientFodselsdato.plusYears(13)

    RuleResult(
        ruleInputs = mapOf("pasientUnder13Aar" to pasientUnder13Aar),
        rule = PatientAgeUnder13Rules.PASIENT_YNGRE_ENN_13,
        ruleResult = pasientUnder13Aar,
    )
}
