package no.nav.syfo.rules

import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.model.Status
import no.nav.syfo.RuleData
import no.nav.syfo.model.Periode
import no.nav.syfo.model.RuleMetadata
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class PeriodLogicRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<RuleMetadata>) -> Boolean) : Rule<RuleData<RuleMetadata>> {
    // TODO: gendate newer than signature date, check if emottak does this?
    @Description("Behandlet dato (felt 12.1) er etter dato for mottak av sykmeldingen.")
    SIGNATURE_DATE_AFTER_RECEIVED_DATE(1123, Status.INVALID, { (healthInformation, ruleMetadata) ->
        healthInformation.behandletTidspunkt > ruleMetadata.signatureDate
    }),

    @Description("Hvis ingen perioder er oppgitt skal sykmeldingen avvises.")
    NO_PERIOD_PROVIDED(1200, Status.INVALID, { (healthInformation, _) ->
        healthInformation.perioder.isNullOrEmpty()
    }),

    @Description("Hvis tildato for en periode ligger før fradato avvises meldingen og hvilken periode det gjelder oppgis.")
    TO_DATE_BEFORE_FROM_DATE(1201, Status.INVALID, { (healthInformation, _) ->
        healthInformation.perioder.any { it.fom.isAfter(it.tom) }
    }),

    @Description("Hvis en eller flere perioder er overlappende avvises meldingen og hvilken periode det gjelder oppgis.")
    OVERLAPPING_PERIODS(1202, Status.INVALID, { (healthInformation, _) ->
        healthInformation.perioder.any { periodA ->
            healthInformation.perioder
                    .filter { periodB -> periodB != periodA }
                    .any { periodB ->
                        periodA.fom in periodB.range() || periodA.tom in periodB.range()
                    }
        }
    }),

    // TODO
    @Description("Hvis det finnes opphold mellom perioder i sykmeldingen avvises meldingen.")
    GAP_BETWEEN_PERIODS(1203, Status.INVALID, { (healthInformation, _) ->
        val ranges = healthInformation.perioder
                .sortedBy { it.fom }
                .map { it.fom to it.tom }

        var gapBetweenPeriods = false
        for (i in 1..(ranges.size - 1)) {
            gapBetweenPeriods = workdaysBetween(ranges[i - 1].second, ranges[i].first) > 0
        }
        gapBetweenPeriods
    }),

    @Description("Hvis fom dato i varighet er lik start dato på sykdomtilfelle og første konsultasjon er mer enn 8 dager fra start dato men ikke over et år")
    BACKDATED_MORE_THEN_8_DAYS_AND_UNDER_1_YEAR_BACKDATED(1204, Status.INVALID, { (healthInformation, _) ->
        healthInformation.kontaktMedPasient.kontaktDato != null && healthInformation.perioder.any {
            // TODO: ? it.periodeFOMDato == healthInformation.syketilfelleStartDato &&
                    healthInformation.kontaktMedPasient.kontaktDato > it.fom.plusDays(7) &&
                    healthInformation.kontaktMedPasient.kontaktDato <= it.fom.plusYears(1).minusDays(1)
        }
    }),

    @Description("Sykmeldinges fom-dato er mer enn 3 år tilbake i tid.")
    BACKDATED_MORE_THEN_3_YEARS(1206, Status.INVALID, { (healthInformation, ruleMetadata) ->
        ruleMetadata.signatureDate.minusYears(3).isAfter(healthInformation.behandletTidspunkt)
    }),

    @Description("Sykmeldingens fom-dato er inntil 3 år tilbake i tid og årsak for tilbakedatering er angitt.")
    BACKDATED_WITH_REASON(1207, Status.MANUAL_PROCESSING, { (healthInformation, ruleMetadata) ->
        ruleMetadata.signatureDate.minusYears(3).isBefore(healthInformation.behandletTidspunkt) &&
                !healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty()
    }),
    @Description("Hvis sykmeldingen er fremdatert mer enn 30 dager etter konsultasjonsdato/signaturdato avvises meldingen.")
    PRE_DATED(1209, Status.INVALID, { (healthInformation, ruleMetadata) ->
        healthInformation.perioder.sortedFOMDate().first().atStartOfDay() > ruleMetadata.signatureDate.plusDays(30)
    }),

    @Description("Hvis sykmeldingens sluttdato er mer enn ett år frem i tid, avvises meldingen.")
    END_DATE(1211, Status.INVALID, { (healthInformation, _) ->
        healthInformation.perioder.sortedTOMDate().last().atStartOfDay() > healthInformation.behandletTidspunkt.plusYears(1)
    }),

    @Description("Hvis behandletdato er etter dato for mottak av meldingen avvises meldingen")
    RECEIVED_DATE_BEFORE_PROCESSED_DATE(1123, Status.INVALID, { (healthInformation, ruleMetadata) ->
        healthInformation.behandletTidspunkt > ruleMetadata.receivedDate.plusHours(2)
    }),

    // TODO: Is this even supposed to be here?
    // NOPE pål would delete this one
    @Description("Hvis avventende sykmelding er funnet og det finnes mer enn en periode")
    PENDING_SICK_LEAVE_COMBINED(1240, Status.INVALID, { (healthInformation, _) ->
        val numberOfPendingPeriods = healthInformation.perioder.count { it.avventendeInnspillTilArbeidsgiver != null }
        numberOfPendingPeriods != 0 && healthInformation.perioder.size > 1
    }),

    @Description("Hvis innspill til arbeidsgiver om tilrettelegging i pkt 4.1.3 ikke er utfylt ved avventende sykmelding avvises meldingen")
    MISSING_INSPILL_TIL_ARBEIDSGIVER(1241, Status.INVALID, { (healthInformation, _) ->
        healthInformation.perioder
                .any { it.avventendeInnspillTilArbeidsgiver != null && it.avventendeInnspillTilArbeidsgiver.trim().isEmpty() }
    }),

    @Description("Hvis avventende sykmelding benyttes utover i arbeidsgiverperioden på 16 kalenderdager, avvises meldingen.")
    PENDING_PERIOD_OUTSIDE_OF_EMPLOYER_PAID(1242, Status.INVALID, { (healthInformation, _) ->
        healthInformation.perioder
                .filter { it.avventendeInnspillTilArbeidsgiver != null }
                .any { (it.fom..it.tom).daysBetween() > 16 }
    }),

    @Description("Hvis antall dager oppgitt for behandlingsdager periode er for høyt i forhold til periodens lengde avvises meldingen. Mer enn en dag per uke er for høyt. 1 dag per påbegynt uke.")
    TOO_MANY_TREATMENT_DAYS(1250, Status.INVALID, { (healthInformation, _) ->
        healthInformation.perioder.any {
            it.behandlingsdager != null && it.behandlingsdager > it.range().startedWeeksBetween()
        }
    }),

    @Description("Hvis sykmeldingsgrad er mindre enn 20% for gradert sykmelding, avvises meldingen")
    PARTIAL_SICK_LEAVE_PERCENTAGE_TO_LOW(1251, Status.INVALID, { (healthInformation, _) ->
        healthInformation.perioder.any {
            it.gradert != null && it.gradert.grad < 20
        }
    }),

    @Description("Hvis sykmeldingsgrad er høyere enn 99% for delvis sykmelding avvises meldingen")
    PARTIAL_SICK_LEAVE_TOO_HIGH_PERCENTAGE(1252, Status.INVALID, { (healthInformation, _) ->
        healthInformation.perioder.mapNotNull { it.gradert }.any { it.grad > 99 }
    }),

    // TODO: Check persisted sykmelding if there is a gap of less then 16 days from the previous one
    @Description("Fom-dato i ny sykmelding som er en forlengelse kan maks være tilbakedatert 1 mnd fra signaturdato. Skal telles.")
    BACKDATING_SYKMELDING_EXTENSION(null, Status.INVALID, { (healthInformation, ruleMetadata) ->
        healthInformation.perioder.sortedFOMDate().first().minusMonths(1).atStartOfDay() > ruleMetadata.signatureDate
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
fun ClosedRange<LocalDate>.startedWeeksBetween(): Long = ChronoUnit.WEEKS.between(start, endInclusive) + 1
fun Periode.range(): ClosedRange<LocalDate> = fom.rangeTo(tom)
