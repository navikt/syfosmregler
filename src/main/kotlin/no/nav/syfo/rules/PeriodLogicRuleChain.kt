package no.nav.syfo.rules

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import no.nav.syfo.model.Periode
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status

enum class PeriodLogicRuleChain(
    override val ruleId: Int?,
    override val status: Status,
    override val messageForUser: String,
    override val messageForSender: String,
    override val predicate: (RuleData<RuleMetadata>) -> Boolean
) : Rule<RuleData<RuleMetadata>> {
    @Description("Behandlet dato (felt 12.1) er etter dato for mottak av sykmeldingen.")
    SIGNATURDATO_ETTER_MOTTATT_DATO(
            1123,
            Status.INVALID,
            "Den må ha riktig dato for behandlingen.",
            "Behandlet dato (felt 12.1) er etter dato for mottak av sykmeldingen.",
            { (healthInformation, ruleMetadata) ->
        healthInformation.behandletTidspunkt > ruleMetadata.signatureDate.plusHours(3)
    }),

    @Description("Hvis ingen perioder er oppgitt skal sykmeldingen avvises.")
    PERIODER_MANGLER(
            1200,
            Status.INVALID,
            "Det er ikke oppgitt hvilken periode sykmeldingen gjelder for.",
            "Hvis ingen perioder er oppgitt skal sykmeldingen avvises.",
            { (healthInformation, _) ->
        healthInformation.perioder.isNullOrEmpty()
    }),

    @Description("Hvis tildato for en periode ligger før fradato avvises meldingen og hvilken periode det gjelder oppgis.")
    FRADATO_ETTER_TILDATO(
            1201,
            Status.INVALID,
            "Det er lagt inn datoer som ikke stemmer innbyrdes.",
            "Hvis tildato for en periode ligger før fradato avvises meldingen og hvilken periode det gjelder oppgis.",
            { (healthInformation, _) ->
        healthInformation.perioder.any { it.fom.isAfter(it.tom) }
    }),

    @Description("Hvis en eller flere perioder er overlappende avvises meldingen og hvilken periode det gjelder oppgis.")
    OVERLAPPENDE_PERIODER(
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
    OPPHOLD_MELLOM_PERIODER(
            1203,
            Status.INVALID,
            "Det er opphold mellom sykmeldingsperiodene.",
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
    TILBAKEDATERT_MER_ENN_3_AR(
            1206,
            Status.INVALID,
            "Startdatoen er mer enn tre år tilbake.",
            "Sykmeldinges fom-dato er mer enn 3 år tilbake i tid.",
            { (healthInformation, _) ->
        healthInformation.perioder.sortedFOMDate().first().atStartOfDay().minusYears(3).isAfter(healthInformation.behandletTidspunkt)
    }),

    @Description("Hvis sykmeldingen er fremdatert mer enn 30 dager etter konsultasjonsdato/signaturdato avvises meldingen.")
    FREMDATERT(
            1209,
            Status.INVALID,
            "Sykmeldingen er datert mer enn 30 dager fram i tid.",
            "Hvis sykmeldingen er fremdatert mer enn 30 dager etter konsultasjonsdato/signaturdato avvises meldingen.",
            { (healthInformation, ruleMetadata) ->
        healthInformation.perioder.sortedFOMDate().first().atStartOfDay() > ruleMetadata.signatureDate.plusDays(30)
    }),

    @Description("Hvis sykmeldingens sluttdato er mer enn ett år frem i tid, avvises meldingen.")
    VARIGHET_OVER_ETT_AAR(
            1211,
            Status.INVALID,
            "Den kan ikke ha en varighet på over ett år.",
            "Hvis sykmeldingens sluttdato er mer enn ett år frem i tid, avvises meldingen.",
            { (healthInformation, _) ->
        healthInformation.perioder.sortedTOMDate().last().atStartOfDay() > healthInformation.behandletTidspunkt.plusYears(1)
    }),

    @Description("Hvis behandletdato er etter dato for mottak av meldingen avvises meldingen")
    BEHANDLINGSDATO_ETTER_MOTTATTDATO(
            1123,
            Status.INVALID,
            "Behandlingsdatoen må rettes.",
            "Hvis behandletdato er etter dato for mottak av meldingen avvises meldingen",
            { (healthInformation, ruleMetadata) ->
        healthInformation.behandletTidspunkt > ruleMetadata.receivedDate.plusHours(2)
    }),

    @Description("Hvis avventende sykmelding er funnet og det finnes en eller flere perioder")
    AVVENTENDE_SYKMELDING_KOMBINERT(
            9999,
            Status.MANUAL_PROCESSING,
            "En avventende sykmelding kan bare inneholde én periode.",
            "Hvis avventende sykmelding er funnet og det finnes en eller flere perioder. ",
            { (healthInformation, _) ->
        val numberOfPendingPeriods = healthInformation.perioder.count { it.avventendeInnspillTilArbeidsgiver != null }
        numberOfPendingPeriods != 0 && healthInformation.perioder.isNotEmpty()
    }),

    @Description("Hvis innspill til arbeidsgiver om tilrettelegging i pkt 4.1.3 ikke er utfylt ved avventende sykmelding avvises meldingen")
    MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER(
            1241,
            Status.INVALID,
            "En avventende sykmelding forutsetter at du kan jobbe hvis arbeidsgiveren din legger til rette for det. Den som har sykmeldt deg har ikke foreslått hva arbeidsgiveren kan gjøre, noe som kreves for denne typen sykmelding.",
            "Hvis innspill til arbeidsgiver om tilrettelegging i pkt 4.1.3 ikke er utfylt ved avventende sykmelding avvises meldingen",
            { (healthInformation, _) ->
        healthInformation.perioder
                .any { it.avventendeInnspillTilArbeidsgiver != null && it.avventendeInnspillTilArbeidsgiver?.trim().isNullOrEmpty() }
    }),

    @Description("Hvis avventende sykmelding benyttes utover i arbeidsgiverperioden på 16 kalenderdager, avvises meldingen.")
    AVVENTENDE_SYKMELDING_OVER_16_DAGER(
            1242,
            Status.INVALID,
            "En avventende sykmelding kan bare gis for 16 dager.",
            "Hvis avventende sykmelding benyttes utover i arbeidsgiverperioden på 16 kalenderdager, avvises meldingen.",
            { (healthInformation, _) ->
        healthInformation.perioder
                .filter { it.avventendeInnspillTilArbeidsgiver != null }
                .any { (it.fom..it.tom).daysBetween() > 16 }
    }),

    @Description("Hvis antall dager oppgitt for behandlingsdager periode er for høyt i forhold til periodens lengde avvises meldingen. Mer enn en dag per uke er for høyt. 1 dag per påbegynt uke.")
    FOR_MANGE_BEHANDLINGSDAGER_PER_UKE(
            1250,
            Status.INVALID,
            "Det er angitt for mange behandlingsdager. Det kan bare angis én behandlingsdag per uke.",
            "Hvis antall dager oppgitt for behandlingsdager periode er for høyt i forhold til periodens lengde avvises meldingen. Mer enn en dag per uke er for høyt. 1 dag per påbegynt uke.",
            { (healthInformation, _) ->
        healthInformation.perioder.any {
            it.behandlingsdager != null && it.behandlingsdager!! > it.range().startedWeeksBetween()
        }
    }),

    @Description("Hvis sykmeldingsgrad er mindre enn 20% for gradert sykmelding, avvises meldingen")
    GRADERT_SYKMELDING_UNDER_20_PROSENT(
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
    GRADERT_SYKMELDING_OVER_99_PROSENT(
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
