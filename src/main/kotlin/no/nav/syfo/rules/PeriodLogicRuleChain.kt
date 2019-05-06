package no.nav.syfo.rules

import no.nav.syfo.model.Status
import no.nav.syfo.model.Periode
import no.nav.syfo.model.RuleMetadata
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class PeriodLogicRuleChain(
    override val ruleId: Int?,
    override val status: Status,
    override val messageForUser: String,
    override val messageForSender: String,
    override val predicate: (RuleData<RuleMetadata>) -> Boolean
) : Rule<RuleData<RuleMetadata>> {
    @Description("Behandlet dato (felt 12.1) er etter dato for mottak av sykmeldingen.")
    SIGNATURE_DATE_AFTER_RECEIVED_DATE(
            1123,
            Status.INVALID,
            "Den må ha riktig dato for behandlingen.",
            "Behandlet dato (felt 12.1) er etter dato for mottak av sykmeldingen.",
            { (healthInformation, ruleMetadata) ->
        healthInformation.behandletTidspunkt > ruleMetadata.signatureDate
    }),

    @Description("Hvis ingen perioder er oppgitt skal sykmeldingen avvises.")
    NO_PERIOD_PROVIDED(
            1200,
            Status.INVALID,
            "Det må stå hvilken periode sykmeldingen gjelder for.",
            "Hvis ingen perioder er oppgitt skal sykmeldingen avvises.",
            { (healthInformation, _) ->
        healthInformation.perioder.isNullOrEmpty()
    }),

    @Description("Hvis tildato for en periode ligger før fradato avvises meldingen og hvilken periode det gjelder oppgis.")
    TO_DATE_BEFORE_FROM_DATE(
            1201,
            Status.INVALID,
            "Til-dato er før fra-dato.",
            "Hvis tildato for en periode ligger før fradato avvises meldingen og hvilken periode det gjelder oppgis.",
            { (healthInformation, _) ->
        healthInformation.perioder.any { it.fom.isAfter(it.tom) }
    }),

    @Description("Hvis en eller flere perioder er overlappende avvises meldingen og hvilken periode det gjelder oppgis.")
    OVERLAPPING_PERIODS(
            1202,
            Status.INVALID,
            "Periodene må ikke overlappe hverandre.",
            "Hvis en eller flere perioder er overlappende avvises meldingen og hvilken periode det gjelder oppgis.",
            { (healthInformation, _) ->
        healthInformation.perioder.any { periodA ->
            healthInformation.perioder
                    .filter { periodB -> periodB != periodA }
                    .any { periodB ->
                        periodA.fom in periodB.range() || periodA.tom in periodB.range()
                    }
        }
    }),

    @Description("Hvis det finnes opphold mellom perioder i sykmeldingen avvises meldingen.")
    GAP_BETWEEN_PERIODS(
            1203,
            Status.INVALID,
            "De ulike sykmeldingsperiodene må henge sammen. ",
            "Hvis det finnes opphold mellom perioder i sykmeldingen avvises meldingen.",
            { (healthInformation, _) ->
        val ranges = healthInformation.perioder
                .sortedBy { it.fom }
                .map { it.fom to it.tom }

        var gapBetweenPeriods = false
        for (i in 1..(ranges.size - 1)) {
            gapBetweenPeriods = workdaysBetween(ranges[i - 1].second, ranges[i].first) > 0
        }
        gapBetweenPeriods
    }),

    @Description("Sykmeldinges fom-dato er mer enn 3 år tilbake i tid.")
    BACKDATED_MORE_THEN_3_YEARS(
            1206,
            Status.INVALID,
            "Startdatoen kan ikke være mer enn tre år tilbake.",
            "Sykmeldinges fom-dato er mer enn 3 år tilbake i tid.",
            { (healthInformation, _) ->
        healthInformation.perioder.sortedFOMDate().first().atStartOfDay().minusYears(3).isAfter(healthInformation.behandletTidspunkt)
    }),

    @Description("Hvis sykmeldingen er fremdatert mer enn 30 dager etter konsultasjonsdato/signaturdato avvises meldingen.")
    PRE_DATED(
            1209,
            Status.INVALID,
            "Datoen må endres fordi sykmeldingen ikke kan være datert mer enn 30 dager fram i tid.",
            "Hvis sykmeldingen er fremdatert mer enn 30 dager etter konsultasjonsdato/signaturdato avvises meldingen.",
            { (healthInformation, ruleMetadata) ->
        healthInformation.perioder.sortedFOMDate().first().atStartOfDay() > ruleMetadata.signatureDate.plusDays(30)
    }),

    @Description("Hvis sykmeldingens sluttdato er mer enn ett år frem i tid, avvises meldingen.")
    END_DATE(
            1211,
            Status.INVALID,
            "Den kan ikke ha en varighet på over ett år.",
            "Hvis sykmeldingens sluttdato er mer enn ett år frem i tid, avvises meldingen.",
            { (healthInformation, _) ->
        healthInformation.perioder.sortedTOMDate().last().atStartOfDay() > healthInformation.behandletTidspunkt.plusYears(1)
    }),

    @Description("Hvis behandletdato er etter dato for mottak av meldingen avvises meldingen")
    RECEIVED_DATE_BEFORE_PROCESSED_DATE(
            1123,
            Status.INVALID,
            "Behandlingsdatoen må rettes.",
            "Hvis behandletdato er etter dato for mottak av meldingen avvises meldingen",
            { (healthInformation, ruleMetadata) ->
        healthInformation.behandletTidspunkt > ruleMetadata.receivedDate.plusHours(2)
    }),

    @Description("Hvis avventende sykmelding er funnet og det finnes en eller flere perioder")
    PENDING_SICK_LEAVE(
            9999,
            Status.MANUAL_PROCESSING,
            "Den inneholder en avventende sykmelding. ",
            "Hvis avventende sykmelding er funnet og det finnes en eller flere perioder. ",
            { (healthInformation, _) ->
        val numberOfPendingPeriods = healthInformation.perioder.count { it.avventendeInnspillTilArbeidsgiver != null }
        numberOfPendingPeriods != 0 && healthInformation.perioder.isNotEmpty()
    }),

    @Description("Hvis innspill til arbeidsgiver om tilrettelegging i pkt 4.1.3 ikke er utfylt ved avventende sykmelding avvises meldingen")
    MISSING_INSPILL_TIL_ARBEIDSGIVER(
            1241,
            Status.INVALID,
            "Den som sykmelder deg må foreslå hvordan arbeidsgiveren din kan tilrettelegge for at du kan jobbe. Det er fordi den er en avventende sykmelding.",
            "Hvis innspill til arbeidsgiver om tilrettelegging i pkt 4.1.3 ikke er utfylt ved avventende sykmelding avvises meldingen",
            { (healthInformation, _) ->
        healthInformation.perioder
                .any { it.avventendeInnspillTilArbeidsgiver != null && it.avventendeInnspillTilArbeidsgiver?.trim().isNullOrEmpty() }
    }),

    @Description("Hvis avventende sykmelding benyttes utover i arbeidsgiverperioden på 16 kalenderdager, avvises meldingen.")
    PENDING_PERIOD_OUTSIDE_OF_EMPLOYER_PAID(
            1242,
            Status.INVALID,
            "Perioden kan ikke være lenger enn 16 dager fordi det er en avventende sykmelding. ",
            "Hvis avventende sykmelding benyttes utover i arbeidsgiverperioden på 16 kalenderdager, avvises meldingen.",
            { (healthInformation, _) ->
        healthInformation.perioder
                .filter { it.avventendeInnspillTilArbeidsgiver != null }
                .any { (it.fom..it.tom).daysBetween() > 16 }
    }),

    @Description("Hvis antall dager oppgitt for behandlingsdager periode er for høyt i forhold til periodens lengde avvises meldingen. Mer enn en dag per uke er for høyt. 1 dag per påbegynt uke.")
    TOO_MANY_TREATMENT_DAYS(
            1250,
            Status.INVALID,
            "Det kan bare angis én behandlingsdag per uke. ",
            "Hvis antall dager oppgitt for behandlingsdager periode er for høyt i forhold til periodens lengde avvises meldingen. Mer enn en dag per uke er for høyt. 1 dag per påbegynt uke.",
            { (healthInformation, _) ->
        healthInformation.perioder.any {
            it.behandlingsdager != null && it.behandlingsdager!! > it.range().startedWeeksBetween()
        }
    }),

    @Description("Hvis sykmeldingsgrad er mindre enn 20% for gradert sykmelding, avvises meldingen")
    PARTIAL_SICK_LEAVE_PERCENTAGE_TO_LOW(
            1251,
            Status.INVALID,
            "Sykmeldingsgraden kan ikke være mindre enn 20 %.",
            "Hvis sykmeldingsgrad er mindre enn 20% for gradert sykmelding, avvises meldingen",
            { (healthInformation, _) ->
        healthInformation.perioder.any {
            it.gradert != null && it.gradert!!.grad < 20
        }
    }),

    @Description("Hvis sykmeldingsgrad er høyere enn 99% for delvis sykmelding avvises meldingen")
    PARTIAL_SICK_LEAVE_TOO_HIGH_PERCENTAGE(
            1252,
            Status.INVALID,
            "Sykmeldingsgraden kan ikke være mer enn 99% fordi det er en gradert sykmelding.",
            "Hvis sykmeldingsgrad er høyere enn 99% for delvis sykmelding avvises meldingen",
            { (healthInformation, _) ->
        healthInformation.perioder.mapNotNull { it.gradert }.any { it.grad > 99 }
    }),
}

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
