package no.nav.syfo.rules

import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.model.sm2013.CV
import no.nav.model.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.get
import no.nav.syfo.model.Status
import no.nav.syfo.ICD10
import no.nav.syfo.ICPC2
import no.nav.syfo.RuleData
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import no.trygdeetaten.xml.eiff._1.XMLMottakenhetBlokk
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.xml.datatype.XMLGregorianCalendar

data class RuleMetadata(
    val signatureDate: ZonedDateTime,
    val receivedDate: ZonedDateTime
) {
    companion object {
        fun from(fellesformat: XMLEIFellesformat): RuleMetadata {
            val msgHead = fellesformat.get<XMLMsgHead>()
            val mottakEnhetBlokk = fellesformat.get<XMLMottakenhetBlokk>()
            return RuleMetadata(
                    signatureDate = msgHead.msgInfo.genDate.atZone(ZoneId.systemDefault()),
                    receivedDate = mottakEnhetBlokk.mottattDatotid.toZoned()
            )
        }
    }
}

enum class PeriodLogicRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<RuleMetadata>) -> Boolean) : Rule<RuleData<RuleMetadata>> {
    // TODO: gendate newer than signature date, check if emottak does this?
    @Description("Behandlet dato (felt 12.1) er etter dato for mottak av sykmeldingen.")
    SIGNATURE_DATE_AFTER_RECEIVED_DATE(1110, Status.INVALID, { (healthInformation, ruleMetadata) ->
        healthInformation.kontaktMedPasient.behandletDato.toZoned() > ruleMetadata.signatureDate
    }),

    @Description("Hvis ingen perioder er oppgitt skal sykmeldingen avvises.")
    NO_PERIOD_PROVIDED(1200, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode == null || healthInformation.aktivitet.periode.isEmpty()
    }),

    @Description("Hvis tildato for en periode ligger før fradato avvises meldingen og hvilken periode det gjelder oppgis.")
    TO_DATE_BEFORE_FROM_DATE(1201, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.any {
            it.periodeFOMDato.toGregorianCalendar() > it.periodeTOMDato.toGregorianCalendar()
        }
    }),

    @Description("Hvis en eller flere perioder er overlappende avvises meldingen og hvilken periode det gjelder oppgis.")
    OVERLAPPING_PERIODS(1202, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.any { periodA ->
            healthInformation.aktivitet.periode
                    .filter { periodB ->
                        periodB != periodA
                    }
                    .any { periodB ->
                        periodA.periodeFOMDato.toZoned() in periodB.range() || periodA.periodeTOMDato.toZoned() in periodB.range()
                    }
        }
    }),

    @Description("Hvis det finnes opphold mellom perioder i sykmeldingen avvises meldingen.")
    GAP_BETWEEN_PERIODS(1203, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode
        val ranges = healthInformation.aktivitet.periode
                .sortedBy { it.periodeFOMDato.toGregorianCalendar() }
                .map { it.periodeFOMDato.toZoned() to it.periodeTOMDato.toZoned() }

        for (i in 1..(ranges.size - 1)) {
            if (workdaysBetween(ranges[i - 1].first, ranges[i].second) > 0) {
                // TODO: return true
            }
        }
        false
    }),

    // TODO: This is completely wrong
    // TODO: This is something the receiver system should do in the future.
    // TODO: Figure how we can check if this is a first time sykmelding
    @Description("Hvis førstegangs sykmelding er tilbakedatert mer enn 8 dager og dokumenterbar kontakt med pasient er mellom behandlet dato og signaturdato")
    FIRST_TIME_BACKDATED_MORE_THEN_8_DAYS(1204, Status.INVALID, { (healthInformation, ruleMetadata) ->
        if (healthInformation.kontaktMedPasient.behandletDato != null && healthInformation.kontaktMedPasient.behandletDato != null && healthInformation.kontaktMedPasient.kontaktDato != null) {
            healthInformation.kontaktMedPasient.behandletDato.toGregorianCalendar() == healthInformation.kontaktMedPasient.kontaktDato.toGregorianCalendar() &&
                    ruleMetadata.signatureDate.minusDays(8) < healthInformation.kontaktMedPasient.behandletDato.toZoned() ||
                    healthInformation.kontaktMedPasient.behandletDato.toZoned() in healthInformation.kontaktMedPasient.kontaktDato.toZoned()..ruleMetadata.signatureDate
        } else {
            false
        }
    }),

    @Description("Sykmeldinges fom-dato er mer enn 3 år tilbake i tid.")
    BACKDATED_MORE_THEN_3_YEARS(1206, Status.INVALID, { (healthInformation, ruleMetadata) ->
        ruleMetadata.signatureDate.minusYears(3).isAfter(healthInformation.kontaktMedPasient.behandletDato.toZoned())
    }),

    @Description("Sykmeldingens fom-dato er inntil 3 år tilbake i tid og årsak for tilbakedatering er angitt.")
    BACKDATED_WITH_REASON(1207, Status.MANUAL_PROCESSING, { (healthInformation, ruleMetadata) ->
        ruleMetadata.signatureDate.minusYears(3).isAfter(healthInformation.kontaktMedPasient.behandletDato.toZoned()) && (healthInformation.kontaktMedPasient.begrunnIkkeKontakt == null)
    }),

    @Description("Hvis sykmeldingen er fremdatert mer enn 30 dager etter konsultasjonsdato/signaturdato avvises meldingen.")
    PRE_DATED(1209, Status.INVALID, { (healthInformation, ruleMetadata) ->
        healthInformation.aktivitet.periode.sortedFOMDate().first().plusDays(30) > ruleMetadata.signatureDate
    }),

    @Description("Hvis sykmeldingens sluttdato er mer enn ett år frem i tid, avvises meldingen.")
    END_DATE(1211, Status.INVALID, { (healthInformation, ruleMetadata) ->
        healthInformation.aktivitet.periode.sortedTOMDate().last() > ruleMetadata.receivedDate.plusYears(1)
    }),

    // TODO: How should we handle this in paper sykemeldinger?
    @Description("Hvis behandletdato er etter dato for mottak av meldingen avvises meldingen")
    RECEIVED_DATE_BEFORE_PROCESSED_DATE(1123, Status.INVALID, { (healthInformation, ruleMetadata) ->
        healthInformation.kontaktMedPasient.behandletDato.toZoned() > ruleMetadata.receivedDate.plusHours(2)
    }),

    // TODO: Is this even supposed to be here?
    @Description("Hvis avventende sykmelding kombineres med andre typer sykmelding avvises meldingen.")
    PENDING_SICK_LEAVE_COMBINED(1240, Status.INVALID, { (healthInformation, _) ->
        val numberOfPendingPeriods = healthInformation.aktivitet.periode.count { it.avventendeSykmelding != null }
        numberOfPendingPeriods != 0 && healthInformation.aktivitet.periode.size == numberOfPendingPeriods
    }),

    @Description("Hvis innspill til arbeidsgiver om tilrettelegging i pkt 4.1.3 ikke er utfylt ved avventende sykmelding avvises meldingen")
    MISSING_INSPILL_TIL_ARBEIDSGIVER(1241, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode
                .filter { it.avventendeSykmelding != null }
                .any { (it.avventendeSykmelding.innspillTilArbeidsgiver == null) }
    }),

    // TODO: Figure if we need to check for the entire duration, not just whats specified in this sykmelding
    @Description("Hvis avventende sykmelding benyttes utover i arbeidsgiverperioden på 16 kalenderdager, avvises meldingen.")
    PENDING_PERIOD_OUTSIDE_OF_EMPLOYER_PAID(1242, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode
                .filter { it.avventendeSykmelding != null }
                .any { (it.periodeFOMDato.toZoned()..it.periodeTOMDato.toZoned()).daysBetween() > 16 }
    }),

    @Description("Hvis antall dager oppgitt for behandlingsdager periode er for høyt i forhold til periodens lengde avvises meldingen. Mer enn en dag per uke er for høyt. 1 dag per påbegynt uke.")
    TOO_MANY_TREATMENT_DAYS(1250, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.any {
            it.behandlingsdager != null && it.behandlingsdager.antallBehandlingsdagerUke > it.range().startedWeeksBetween()
        }
    }),

    @Description("Hvis sykmeldingsgrad er mindre enn 20% for gradert sykmelding, avvises meldingen")
    PARTIAL_SICK_LEAVE_PERCENTAGE_TO_LOW(1251, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.any {
            it.gradertSykmelding != null && it.gradertSykmelding.sykmeldingsgrad < 20
        }
    }),

    @Description("Hvis sykmeldingsgrad er høyere enn 99% for delvis sykmelding avvises meldingen")
    PARTIAL_SICK_LEAVE_TOO_HIGH_PERCENTAGE(1252, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.filter { it.gradertSykmelding != null }.any { it.gradertSykmelding.sykmeldingsgrad > 99 }
    }),

    // TODO: Check persisted sykmelding if there is a gap of less then 16 days from the previous one
    @Description("Fom-dato i ny sykmelding som er en forlengelse kan maks være tilbakedatert 1 mnd fra signaturdato. Skal telles.")
    BACKDATING_SYKMELDING_EXTENSION(null, Status.INVALID, { (healthInformation, ruleMetadata) ->
        healthInformation.aktivitet.periode.sortedFOMDate().first().minusMonths(1) > ruleMetadata.signatureDate
    }),
}

fun List<HelseOpplysningerArbeidsuforhet.Aktivitet.Periode>.sortedFOMDate(): List<ZonedDateTime> =
        map { it.periodeFOMDato.toZoned() }.sorted()
fun List<HelseOpplysningerArbeidsuforhet.Aktivitet.Periode>.sortedTOMDate(): List<ZonedDateTime> =
        map { it.periodeTOMDato.toZoned() }.sorted()

fun XMLGregorianCalendar.toZoned(): ZonedDateTime = toGregorianCalendar().toZonedDateTime()

fun workdaysBetween(a: ZonedDateTime, b: ZonedDateTime): Int = (1..(ChronoUnit.DAYS.between(a, b) - 1))
        .map { a.plusDays(it) }
        .filter { it.dayOfWeek !in arrayOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
        .count()

fun ClosedRange<ZonedDateTime>.daysBetween(): Long = ChronoUnit.DAYS.between(start, endInclusive)
fun ClosedRange<ZonedDateTime>.startedWeeksBetween(): Long = ChronoUnit.WEEKS.between(start, endInclusive) + 1
fun HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.range(): ClosedRange<ZonedDateTime> =
        periodeFOMDato.toZoned().rangeTo(periodeTOMDato.toZoned())

operator fun ClosedRange<ZonedDateTime>.contains(v: XMLGregorianCalendar) =
        v.toZoned() > start || v.toZoned() < endInclusive

fun CV.isICPC2(): Boolean = v == ICPC2.A01.oid

fun CV.toICPC2(): List<ICPC2>? = if (isICPC2()) { listOfNotNull(ICPC2.values().find { it.codeValue == v }) } else { ICD10.values().find { it.codeValue == v }?.icpc2 }

val diagnoseCodesSimplified = listOf(
        ICPC2.D70, ICPC2.D73, ICPC2.F70, ICPC2.F73, ICPC2.H71, ICPC2.R72, ICPC2.R74, ICPC2.R75, ICPC2.R76, ICPC2.R77, ICPC2.R78, ICPC2.R80, ICPC2.R81, ICPC2.U71
)
