package no.nav.syfo.rules.periodlogic

import no.nav.syfo.model.Periode
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.rules.range
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

typealias Rule<T> = (sykmelding: Sykmelding, ruleMetadata: RuleMetadata,) -> RuleResult<T>
typealias PeriodLogicRule = Rule<PeriodLogicRules>

val periodeMangler: PeriodLogicRule = { sykmelding, _ ->
    val periodeMangler = sykmelding.perioder.isEmpty()

    RuleResult(
        ruleInputs = mapOf("periodeMangler" to periodeMangler),
        rule = PeriodLogicRules.PERIODER_MANGLER,
        ruleResult = periodeMangler
    )
}

val fraDatoEtterTilDato: PeriodLogicRule = { sykmelding, _ ->
    val perioder = sykmelding.perioder

    val fraDatoEtterTilDato = perioder.any { it.fom.isAfter(it.tom) }

    RuleResult(
        ruleInputs = mapOf("fraDatoEtterTilDato" to fraDatoEtterTilDato),
        rule = PeriodLogicRules.FRADATO_ETTER_TILDATO,
        ruleResult = fraDatoEtterTilDato
    )
}

val overlappendePerioder: PeriodLogicRule = { sykmelding, _ ->
    val perioder = sykmelding.perioder

    val overlappendePerioder = perioder.any { periodA ->
        perioder
            .filter { periodB -> periodB != periodA }
            .any { periodB ->
                periodA.fom in periodB.range() || periodA.tom in periodB.range()
            }
    }

    RuleResult(
        ruleInputs = mapOf("overlappendePerioder" to overlappendePerioder),
        rule = PeriodLogicRules.OVERLAPPENDE_PERIODER,
        ruleResult = overlappendePerioder
    )
}

// TODO impl rest of rules

fun List<Periode>.sortedFOMDate(): List<LocalDate> =
    map { it.fom }.sorted()

fun List<Periode>.sortedTOMDate(): List<LocalDate> =
    map { it.tom }.sorted()

fun workdaysBetween(a: LocalDate, b: LocalDate): Int = (1..(ChronoUnit.DAYS.between(a, b) - 1))
    .map { a.plusDays(it) }
    .filter { it.dayOfWeek !in arrayOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
    .count()

fun ClosedRange<LocalDate>.daysBetween(): Long = ChronoUnit.DAYS.between(start, endInclusive)
fun ClosedRange<LocalDate>.startedWeeksBetween(): Int = ChronoUnit.WEEKS.between(start, endInclusive).toInt() + 1
fun Periode.range(): ClosedRange<LocalDate> = fom.rangeTo(tom)
