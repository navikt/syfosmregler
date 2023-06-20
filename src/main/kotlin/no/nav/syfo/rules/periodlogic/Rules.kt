package no.nav.syfo.rules.periodlogic

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import no.nav.syfo.model.Periode
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.services.daysBetween
import no.nav.syfo.services.sortedFOMDate

typealias Rule<T> = (sykmelding: Sykmelding, ruleMetadata: RuleMetadata) -> RuleResult<T>

typealias PeriodLogicRule = Rule<PeriodLogicRules>

val periodeMangler: PeriodLogicRule = { sykmelding, _ ->
    val perioder = sykmelding.perioder
    val periodeMangler = perioder.isEmpty()

    RuleResult(
        ruleInputs = mapOf("perioder" to perioder),
        rule = PeriodLogicRules.PERIODER_MANGLER,
        ruleResult = periodeMangler,
    )
}

val fraDatoEtterTilDato: PeriodLogicRule = { sykmelding, _ ->
    val perioder = sykmelding.perioder

    val fraDatoEtterTilDato = perioder.any { it.fom.isAfter(it.tom) }

    RuleResult(
        ruleInputs = mapOf("perioder" to perioder),
        rule = PeriodLogicRules.FRADATO_ETTER_TILDATO,
        ruleResult = fraDatoEtterTilDato,
    )
}

val overlappendePerioder: PeriodLogicRule = { sykmelding, _ ->
    val perioder = sykmelding.perioder

    val overlappendePerioder =
        perioder.any { periodA ->
            perioder
                .filter { periodB -> periodB != periodA }
                .any { periodB -> periodA.fom in periodB.range() || periodA.tom in periodB.range() }
        }

    RuleResult(
        ruleInputs = mapOf("perioder" to perioder),
        rule = PeriodLogicRules.OVERLAPPENDE_PERIODER,
        ruleResult = overlappendePerioder,
    )
}

val oppholdMellomPerioder: PeriodLogicRule = { sykmelding, _ ->
    val periodeRanges = sykmelding.perioder.sortedBy { it.fom }.map { it.fom to it.tom }

    var oppholdMellomPerioder = false
    for (i in 1 until periodeRanges.size) {
        oppholdMellomPerioder =
            workdaysBetween(periodeRanges[i - 1].second, periodeRanges[i].first) > 0
        if (oppholdMellomPerioder == true) {
            break
        }
    }

    RuleResult(
        ruleInputs = mapOf("periodeRanges" to periodeRanges),
        rule = PeriodLogicRules.OPPHOLD_MELLOM_PERIODER,
        ruleResult = oppholdMellomPerioder,
    )
}

val ikkeDefinertPeriode: PeriodLogicRule = { sykmelding, _ ->
    val perioder = sykmelding.perioder

    val ikkeDefinertPeriode =
        perioder.any {
            it.aktivitetIkkeMulig == null &&
                it.gradert == null &&
                it.avventendeInnspillTilArbeidsgiver.isNullOrEmpty() &&
                !it.reisetilskudd &&
                (it.behandlingsdager == null || it.behandlingsdager == 0)
        }

    RuleResult(
        ruleInputs = mapOf("perioder" to perioder),
        rule = PeriodLogicRules.IKKE_DEFINERT_PERIODE,
        ruleResult = ikkeDefinertPeriode,
    )
}

val fremdatertOver30Dager: PeriodLogicRule = { sykmelding, ruleMetadata ->
    val forsteFomDato = sykmelding.perioder.sortedFOMDate().firstOrNull()
    val behandletTidspunkt = ruleMetadata.behandletTidspunkt

    val fremdatert =
        when (forsteFomDato) {
            null -> false
            else -> forsteFomDato > behandletTidspunkt.plusDays(30).toLocalDate()
        }

    RuleResult(
        ruleInputs = mapOf("fremdatert" to fremdatert),
        rule = PeriodLogicRules.FREMDATERT,
        ruleResult = fremdatert,
    )
}
val tilbakeDatertOver3Ar: PeriodLogicRule = { sykmelding, _ ->
    val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
    val tilbakeDatertMerEnn3AAr =
        forsteFomDato.atStartOfDay().isBefore(LocalDate.now().minusYears(3).atStartOfDay())

    RuleResult(
        ruleInputs =
            mapOf(
                "tilbakeDatertMerEnn3AAr" to tilbakeDatertMerEnn3AAr,
            ),
        rule = PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR,
        ruleResult = tilbakeDatertMerEnn3AAr,
    )
}

val varighetOver1AAr: PeriodLogicRule = { sykmelding, _ ->
    val forsteFomDato = sykmelding.perioder.sortedFOMDate().firstOrNull()
    val sisteTomDato = sykmelding.perioder.sortedTOMDate().lastOrNull()

    val varighetOver1AAr =
        if (forsteFomDato == null || sisteTomDato == null) {
            false
        } else {
            val firstFomDate = forsteFomDato.atStartOfDay().toLocalDate()
            val lastFomDate = sisteTomDato.atStartOfDay().toLocalDate()
            (firstFomDate..lastFomDate).daysBetween() > 365
        }

    RuleResult(
        ruleInputs = mapOf("varighetOver1AAr" to varighetOver1AAr),
        rule = PeriodLogicRules.TOTAL_VARIGHET_OVER_ETT_AAR,
        ruleResult = varighetOver1AAr,
    )
}

val behandslingsDatoEtterMottatDato: PeriodLogicRule = { sykmelding, ruleMetadata ->
    val behandletTidspunkt = sykmelding.behandletTidspunkt
    val receivedDate = ruleMetadata.receivedDate

    val behandslingsDatoEtterMottatDato = behandletTidspunkt > receivedDate.plusDays(1)

    RuleResult(
        ruleInputs = mapOf("behandslingsDatoEtterMottatDato" to behandslingsDatoEtterMottatDato),
        rule = PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO,
        ruleResult = behandslingsDatoEtterMottatDato,
    )
}

val avventendeKombinert: PeriodLogicRule = { sykmelding, _ ->
    val perioder = sykmelding.perioder

    val avventendeKombinert =
        perioder.count { it.avventendeInnspillTilArbeidsgiver != null } != 0 && perioder.size > 1

    RuleResult(
        ruleInputs = mapOf("avventendeKombinert" to avventendeKombinert),
        rule = PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT,
        ruleResult = avventendeKombinert,
    )
}

val manglendeInnspillArbeidsgiver: PeriodLogicRule = { sykmelding, _ ->
    val perioder = sykmelding.perioder

    val manglendeInnspillArbeidsgiver =
        perioder.any {
            it.avventendeInnspillTilArbeidsgiver != null &&
                it.avventendeInnspillTilArbeidsgiver?.trim().isNullOrEmpty()
        }

    RuleResult(
        ruleInputs = mapOf("manglendeInnspillArbeidsgiver" to manglendeInnspillArbeidsgiver),
        rule = PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER,
        ruleResult = manglendeInnspillArbeidsgiver,
    )
}

val avventendeOver16Dager: PeriodLogicRule = { sykmelding, _ ->
    val perioder = sykmelding.perioder

    val avventendeOver16Dager =
        perioder
            .filter { it.avventendeInnspillTilArbeidsgiver != null }
            .any { (it.fom..it.tom).daysBetween() > 16 }

    RuleResult(
        ruleInputs = mapOf("avventendeOver16Dager" to avventendeOver16Dager),
        rule = PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER,
        ruleResult = avventendeOver16Dager,
    )
}

val forMangeBehandlingsDagerPrUke: PeriodLogicRule = { sykmelding, _ ->
    val perioder = sykmelding.perioder

    val forMangeBehandlingsDagerPrUke =
        perioder.any {
            it.behandlingsdager != null && it.behandlingsdager!! > it.range().startedWeeksBetween()
        }

    RuleResult(
        ruleInputs = mapOf("forMangeBehandlingsDagerPrUke" to forMangeBehandlingsDagerPrUke),
        rule = PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE,
        ruleResult = forMangeBehandlingsDagerPrUke,
    )
}

val gradertOver99Prosent: PeriodLogicRule = { sykmelding, _ ->
    val perioder = sykmelding.perioder

    val gradertOver99Prosent = perioder.mapNotNull { it.gradert }.any { it.grad > 99 }

    RuleResult(
        ruleInputs = mapOf("gradertOver99Prosent" to gradertOver99Prosent),
        rule = PeriodLogicRules.GRADERT_SYKMELDING_OVER_99_PROSENT,
        ruleResult = gradertOver99Prosent,
    )
}

val inneholderBehandlingsDager: PeriodLogicRule = { sykmelding, _ ->
    val perioder = sykmelding.perioder

    val inneholderBehandlingsDager = perioder.any { it.behandlingsdager != null }

    RuleResult(
        ruleInputs = mapOf("inneholderBehandlingsDager" to inneholderBehandlingsDager),
        rule = PeriodLogicRules.SYKMELDING_MED_BEHANDLINGSDAGER,
        ruleResult = inneholderBehandlingsDager,
    )
}

fun List<Periode>.sortedTOMDate(): List<LocalDate> = map { it.tom }.sorted()

fun workdaysBetween(a: LocalDate, b: LocalDate): Int =
    (1..(ChronoUnit.DAYS.between(a, b) - 1))
        .map { a.plusDays(it) }
        .filter { it.dayOfWeek !in arrayOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
        .count()

fun ClosedRange<LocalDate>.startedWeeksBetween(): Int =
    ChronoUnit.WEEKS.between(start, endInclusive).toInt() + 1

fun Periode.range(): ClosedRange<LocalDate> = fom.rangeTo(tom)
