package no.nav.syfo.rules.periode

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.rules.periodvalidering.sortedTOMDate
import no.nav.syfo.services.sortedFOMDate

typealias Rule<T> = (sykmelding: Sykmelding, ruleMetadata: RuleMetadata) -> RuleResult<T>

typealias PeriodeRule = Rule<PeriodeRules>

val fremdatertOver30Dager: PeriodeRule = { sykmelding, ruleMetadata ->
    val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
    val genereringsTidspunkt = ruleMetadata.signatureDate

    val fremdatert = forsteFomDato > genereringsTidspunkt.plusDays(30).toLocalDate()

    RuleResult(
        ruleInputs =
            mapOf(
                "genereringsTidspunkt" to genereringsTidspunkt,
                "fom" to forsteFomDato,
                "fremdatert" to fremdatert
            ),
        rule = PeriodeRules.FREMDATERT_MER_ENN_30_DAGER,
        ruleResult = fremdatert,
    )
}

val varighetOver1AAr: PeriodeRule = { sykmelding, _ ->
    val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
    val sisteTomDato = sykmelding.perioder.sortedTOMDate().last()

    val varighetOver1AAr = ChronoUnit.DAYS.between(forsteFomDato, sisteTomDato) > 365

    RuleResult(
        ruleInputs =
            mapOf(
                "fom" to forsteFomDato,
                "tom" to sisteTomDato,
                "varighetOver1AAr" to varighetOver1AAr
            ),
        rule = PeriodeRules.TOTAL_VARIGHET_OVER_ETT_AAR,
        ruleResult = varighetOver1AAr,
    )
}

val tilbakeDatertOver3Ar: PeriodeRule = { sykmelding, ruleMetadata ->
    val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
    val tilbakeDatertMerEnn3AAr = forsteFomDato.isBefore(LocalDate.now().minusYears(3))
    val genereringsTidspunkt = ruleMetadata.signatureDate

    RuleResult(
        ruleInputs =
            mapOf(
                "genereringsTidspunkt" to genereringsTidspunkt,
                "fom" to forsteFomDato,
                "tilbakeDatertMerEnn3AAr" to tilbakeDatertMerEnn3AAr,
            ),
        rule = PeriodeRules.TILBAKEDATERT_MER_ENN_3_AR,
        ruleResult = tilbakeDatertMerEnn3AAr,
    )
}
