package no.nav.syfo.rules

import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.model.sm2013.CV
import no.nav.model.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.Description
import no.nav.syfo.Diagnosekode
import no.nav.syfo.Rule
import no.nav.syfo.api.extractHelseopplysninger
import no.nav.syfo.contains
import no.nav.syfo.get
import no.nav.syfo.model.Status
import no.nav.syfo.validation.extractBornDate
import no.nav.syfo.validation.validatePersonAndDNumber
import no.nav.syfo.ICD10
import no.nav.syfo.ICPC2
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import no.trygdeetaten.xml.eiff._1.XMLMottakenhetBlokk
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.xml.datatype.XMLGregorianCalendar

data class RuleData(
    val healthInformation: HelseOpplysningerArbeidsuforhet,
    val ruleMetadata: RuleMetadata
) {
    companion object {
        fun fromFellesformat(fellesformat: XMLEIFellesformat): RuleData {
            val msgHead = fellesformat.get<XMLMsgHead>()
            val mottakEnhetBlokk = fellesformat.get<XMLMottakenhetBlokk>()
            return RuleData(
                    healthInformation = extractHelseopplysninger(msgHead),
                    ruleMetadata = RuleMetadata(
                            signatureDate = msgHead.msgInfo.genDate.atZone(ZoneId.systemDefault()),
                            receivedDate = mottakEnhetBlokk.mottattDatotid.toZoned()
                    )
            )
        }
    }
}
data class RuleMetadata(
    val signatureDate: ZonedDateTime,
    val receivedDate: ZonedDateTime
)

enum class ValidationRules(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData) -> Boolean) : Rule<RuleData> {
    // TODO: Use this ruleId for when the TPS SOAP call returns that the person is missing
    @Description("Fødselsnummer/D-nummer kan passerer ikke modulus 11")
    INVALID_FNR(1006, Status.INVALID, { (healthInformation, _) ->
        validatePersonAndDNumber(healthInformation.pasient.fodselsnummer.id)
    }),

    @Description("Hele sykmeldingsperioden er før bruker har fylt 13 år. Pensjonsopptjening kan starte fra 13 år.")
    YOUNGER_THAN_13(1101, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.sortedTOMDate().last().toLocalDate() < extractBornDate(healthInformation.pasient.fodselsnummer.id).plusYears(13)
    }),

    @Description("Hele sykmeldingsperioden er etter at bruker har fylt 70 år. Dersom bruker fyller 70 år i perioden skal sykmelding gå gjennom på vanlig måte.")
    PATIENT_OVER_70_YEARS(1102, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.sortedFOMDate().first().toLocalDate() > extractBornDate(healthInformation.pasient.fodselsnummer.id).plusYears(70)
    }),

    @Description("Ukjent diagnosekode type")
    UNKNOWN_DIAGNOSECODE_TYPE(1137, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose.diagnosekode.s !in Diagnosekode.values()
    }),

    @Description("Hvis hoveddiagnose er Z-diagnose (ICPC-2), avvises meldingen.")
    ICPC_2_Z_DIAGNOSE(null, Status.INVALID, { (healthInformation, _) ->
        // To support not having a main diagnosis and avoid checking for invalid code here we allow null and compare
        // with the equality operator
        (healthInformation.medisinskVurdering.hovedDiagnose.diagnosekode.toICPC2()?.first()?.codeValue?.startsWith("Z") == true)
    }),

    @Description("Hvis hoveddiagnose mangler og det ikke er angitt annen lovfestet fraværsgrunn, avvises meldingen")
    MAIN_DIAGNOSE_MISSING_AND_MISSING_REASON(null, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.annenFraversArsak == null &&
                healthInformation.medisinskVurdering.hovedDiagnose.let { it == null || it.diagnosekode == null || it.diagnosekode.v == null }
    }),

    @Description("Hvis kodeverk ikke er angitt eller korrekt for hoveddiagnose, avvises meldingen.")
    INVALID_KODEVERK_FOR_MAIN_DIAGNOSE(1540, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose.diagnosekode.let { cv ->
            if (cv.isICPC2()) {
                ICPC2.values().any { it.codeValue == cv.v }
            } else {
                ICD10.values().any { it.codeValue == cv.v }
            }
        }
    }),

    @Description("Hvis sykmeldingsgrad er mindre enn 20% for gradert sykmelding, avvises meldingen")
    PARTIAL_SICK_LEAVE_PERCENTAGE_TO_LOW(null, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.any {
            it.gradertSykmelding != null && it.gradertSykmelding.sykmeldingsgrad < 20
        }
    }),

    // EI@ tekst "Enkeltstående behandlingsdager er angitt."
    @Description("Hvis behandlingsdager er angitt sendes meldingen til manuell behandling.")
    NUMBER_OF_TREATMENT_DAYS_SET(1260, Status.MANUAL_PROCESSING, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.any { it.behandlingsdager != null }
    }),

    // TODO: '1608','Kun reisetilskudd er angitt. Melding sendt til oppfølging i Arena, skal ikke registreres i Infotrygd.',3,'1' ???
    @Description("Hvis sykmeldingen angir reisetilskudd går meldingen til manuell behandling.")
    TRAVEL_SUBSIDY_SPECIFIED(1608, Status.MANUAL_PROCESSING, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.any { it.isReisetilskudd == true } // Can be null, so use equality
    }),

    @Description("Hvis medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt og sykmeldingen ikke er \"forenklet\"")
    NO_MEDICAL_OR_WORKPLACE_RELATED_REASONS(null, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose.diagnosekode.toICPC2()?.any { icpc2 -> icpc2 in diagnoseCodesSimplified } == true && !healthInformation.aktivitet.periode.all {
            (it.gradertSykmelding == null || it.gradertSykmelding.sykmeldingsgrad == 100) &&
                    it.aktivitetIkkeMulig?.arbeidsplassen?.arsakskode != null &&
                    it.aktivitetIkkeMulig.medisinskeArsaker?.arsakskode != null
        } == true
    }),

    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt  ved  8, 17, 39 uker før regelsettversjon \"2\" er innført skal sykmeldingen avvises")
    WHAT_EVEN_IS_THIS_RULE(null, Status.INVALID, { (healthInformation, _) ->
        true // TODO
    })
}

enum class PeriodLogicRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData) -> Boolean) : Rule<RuleData> {
    @Description("Behandlet dato (felt 12.1) er etter dato for mottak av sykmeldingen.")
    SIGNATURE_DATE_AFTER_RECEIVED_DATE(1110, Status.INVALID, { (_, ruleMetadata) ->
        ruleMetadata.receivedDate < ruleMetadata.signatureDate
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

    @Description("Sykmeldinges fom-dato er mer enn 3 år tilbake i tid.")
    BACKDATED_MORE_THEN_3_YEARS(1206, Status.INVALID, { (healthInformation, ruleMetadata) ->
        ruleMetadata.signatureDate.minusYears(3) < healthInformation.kontaktMedPasient.behandletDato.toZoned()
    }),

    @Description("Sykmeldingens fom-dato er inntil 3 år tilbake i tid og årsak for tilbakedatering er angitt.")
    BACKDATED_WITH_REASON(1207, Status.MANUAL_PROCESSING, { (healthInformation, _) ->
        (healthInformation.kontaktMedPasient.begrunnIkkeKontakt == null)
    }),

    // TODO: This is something the receiver system should do in the future.
    @Description("Hvis førstegangs sykmelding er tilbakedatert mer enn 8 dager og dokumenterbar kontakt med pasient er mellom behandlet dato og signaturdato")
    FIRST_TIME_BACKDATED_MORE_THEN_8_DAYS(null, Status.INVALID, { (healthInformation, ruleMetadata) ->
        (TODO("How can we figure out if its a first-time sykmelding?") &&
                ruleMetadata.signatureDate.minusDays(8) < healthInformation.kontaktMedPasient.behandletDato.toZoned()) ||
                healthInformation.kontaktMedPasient.behandletDato.toZoned() in healthInformation.kontaktMedPasient.kontaktDato.toZoned()..ruleMetadata.signatureDate
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

    @Description("Hvis avventende sykmelding benyttes utover i arbeidsgiverperioden på 16 kalenderdager, avvises meldingen.")
    PENDING_PERIOD_OUTSIDE_OF_EMPLOYER_PAID(1242, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode
                .filter { it.avventendeSykmelding != null }
                .any { (it.periodeFOMDato.toZoned()..it.periodeTOMDato.toZoned()).daysBetween() > 16 }
    }),

    @Description("Hvis antall dager oppgitt for behandlingsdager periode er for høyt i forhold til periodens lengde avvises meldingen. Mer enn en dag per uke er for høyt. 1 dag per påbegynt uke.")
    TOO_MANY_TREATMENT_DAYS(1250, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.any {
            it.behandlingsdager.antallBehandlingsdagerUke > it.range().startedWeeksBetween()
        }
    }),

    @Description("Hvis gradert sykmelding og reisetilskudd er oppgitt for samme periode sendes meldingen til manuell behandling.")
    GRADUAL_SYKMELDING_COMBINED_WITH_TRAVEL(1250, Status.MANUAL_PROCESSING, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.any { it.gradertSykmelding != null && it.isReisetilskudd }
    }),

    @Description("Fom-dato i ny sykmelding som er en forlengelse kan maks være tilbakedatert 1 mnd fra signaturdato. Skal telles.")
    BACKDATING_SYKMELDING_EXTENSION(null, Status.INVALID, { (healthInformation, ruleMetadata) ->
        healthInformation.aktivitet.periode.sortedFOMDate().first().minusMonths(1) < ruleMetadata.signatureDate
    }),

    @Description("Hvis sykmeldingen er fremdatert mer enn 30 dager etter konsultasjonsdato/signaturdato avvises meldingen.")
    PRE_DATED(null, Status.INVALID, { (healthInformation, ruleMetadata) ->
        healthInformation.aktivitet.periode.sortedFOMDate().first() > ruleMetadata.signatureDate.plusDays(30)
    }),

    @Description("Hvis sykmeldingens sluttdato er mer enn ett år frem i tid, avvises meldingen.")
    END_DATE(null, Status.INVALID, { (healthInformation, ruleMetadata) ->
        healthInformation.aktivitet.periode.sortedTOMDate().last() > ruleMetadata.receivedDate.plusYears(1)
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
