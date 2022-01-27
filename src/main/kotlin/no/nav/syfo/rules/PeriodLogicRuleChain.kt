package no.nav.syfo.rules

import no.nav.syfo.model.Periode
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
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

    // Sykmeldingen må inneholden en fom-dato og en tom-dato
    @Description("Hvis ingen perioder er oppgitt skal sykmeldingen avvises.")
    PERIODER_MANGLER(
        1200,
        Status.INVALID,
        "Det er ikke oppgitt hvilken periode sykmeldingen gjelder for.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis ingen perioder er oppgitt skal sykmeldingen avvises.",
        { (healthInformation, _) ->
            healthInformation.perioder.isNullOrEmpty()
        }
    ),

    // fom-dato må være før tom-dato
    @Description("Hvis tildato for en periode ligger før fradato avvises meldingen og hvilken periode det gjelder oppgis.")
    FRADATO_ETTER_TILDATO(
        1201,
        Status.INVALID,
        "Det er lagt inn datoer som ikke stemmer innbyrdes.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis tildato for en periode ligger før fradato avvises meldingen og hvilken periode det gjelder oppgis.",
        { (healthInformation, _) ->
            healthInformation.perioder.any { it.fom.isAfter(it.tom) }
        }
    ),

    // Hvis sykmeldingen inneholder flere perioden kan ikke periodene overlappe hverandre
    @Description("Hvis en eller flere perioder er overlappende avvises meldingen og hvilken periode det gjelder oppgis.")
    OVERLAPPENDE_PERIODER(
        1202,
        Status.INVALID,
        "Periodene må ikke overlappe hverandre.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis en eller flere perioder er overlappende avvises meldingen og hvilken periode det gjelder oppgis.",
        { (healthInformation, _) ->
            healthInformation.perioder.any { periodA ->
                healthInformation.perioder
                    .filter { periodB -> periodB != periodA }
                    .any { periodB ->
                        periodA.fom in periodB.range() || periodA.tom in periodB.range()
                    }
            }
        }
    ),

    // Det kan ikke være opphold mellom perioder i sykmeldingen
    @Description("Hvis det finnes opphold mellom perioder i sykmeldingen avvises meldingen.")
    OPPHOLD_MELLOM_PERIODER(
        1203,
        Status.INVALID,
        "Det er opphold mellom sykmeldingsperiodene.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis det finnes opphold mellom perioder i sykmeldingen avvises meldingen.",
        { (healthInformation, _) ->
            val ranges = healthInformation.perioder
                .sortedBy { it.fom }
                .map { it.fom to it.tom }

            var gapBetweenPeriods = false
            for (i in 1..(ranges.size - 1)) {
                gapBetweenPeriods = workdaysBetween(ranges[i - 1].second, ranges[i].first) > 0
                if (gapBetweenPeriods == true) {
                    break
                }
            }
            gapBetweenPeriods
        }
    ),

    // For hver perioden må det angis om det er 100% sykmelding, gradert, reisetilskudd, behandlingsdager eller avventende sykmelding
    @Description("Hvis det ikke er oppgitt type for perioden skal sykmeldingen avvises.")
    IKKE_DEFINERT_PERIODE(
        1204,
        Status.INVALID,
        "Det er ikke oppgitt type for sykmeldingen (den må være enten 100 prosent, gradert, avventende, reisetilskudd eller behandlingsdager).",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Det er ikke oppgitt type for sykmeldingen (den må være enten 100 prosent, gradert, avventende, reisetilskudd eller behandlingsdager).",
        { (healthInformation, _) ->
            healthInformation.perioder.any { it.aktivitetIkkeMulig == null && it.gradert == null && it.avventendeInnspillTilArbeidsgiver.isNullOrEmpty() && !it.reisetilskudd && (it.behandlingsdager == null || it.behandlingsdager == 0) }
        }
    ),

    // Vi tar ikke imot sykmeldinger som ligger mer enn 3 år tilbake i tid
    @Description("Sykmeldinges fom-dato er mer enn 3 år tilbake i tid.")
    TILBAKEDATERT_MER_ENN_3_AR(
        1206,
        Status.INVALID,
        "Startdatoen er mer enn tre år tilbake.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Sykmeldinges fom-dato er mer enn 3 år tilbake i tid.",
        { (healthInformation, _) ->
            healthInformation.perioder.sortedFOMDate().first().atStartOfDay().isBefore(LocalDate.now().minusYears(3).atStartOfDay())
        }
    ),

    // En sykmelding kan ikke fremdateres mer enn 30 dager
    @Description("Hvis sykmeldingen er fremdatert mer enn 30 dager etter konsultasjonsdato/signaturdato avvises meldingen.")
    FREMDATERT(
        1209,
        Status.INVALID,
        "Sykmeldingen er datert mer enn 30 dager fram i tid.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis sykmeldingen er fremdatert mer enn 30 dager etter behandletDato avvises meldingen.",
        { (sykmelding, ruleMetadata) ->
            sykmelding.perioder.sortedFOMDate().first() > ruleMetadata.behandletTidspunkt.plusDays(30).toLocalDate()
        }
    ),

    // En sykmelding kan ikke vare mer enn 1 år
    @Description("Hvis sykmeldingen første fom og siste tom har ein varighet som er over 1 år. avvises meldingen.")
    TOTAL_VARIGHET_OVER_ETT_AAR(
        1211,
        Status.INVALID,
        "Den kan ikke ha en varighet på over ett år.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Sykmeldingen første fom og siste tom har ein varighet som er over 1 år",
        { (healthInformation, _) ->
            val firstFomDate = healthInformation.perioder.sortedFOMDate().first().atStartOfDay().toLocalDate()
            val lastTomDate = healthInformation.perioder.sortedTOMDate().last().atStartOfDay().toLocalDate()
            (firstFomDate..lastTomDate).daysBetween() > 365
        }
    ),

    // Behandlet dato skal angi tidspunktet da pasienten oppsøkte legen. Den kan ikke være frem i tid.
    @Description("Hvis behandletdato er etter dato for mottak av meldingen avvises meldingen")
    BEHANDLINGSDATO_ETTER_MOTTATTDATO(
        1123,
        Status.INVALID,
        "Behandlingsdatoen må rettes.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Behandlingsdatoen er etter dato for når NAV mottok meldingen",
        { (healthInformation, ruleMetadata) ->
            healthInformation.behandletTidspunkt > ruleMetadata.receivedDate.plusDays(1)
        }
    ),

    // Avventende sykmelding kan ikke kombineres med noe annet
    @Description("Hvis avventende sykmelding er funnet og det finnes flere perioder")
    AVVENTENDE_SYKMELDING_KOMBINERT(
        9999,
        Status.INVALID,
        "En avventende sykmelding kan bare inneholde én periode.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Avventende sykmelding kan ikke inneholde flere perioder.",
        { (healthInformation, _) ->
            healthInformation.perioder.count { it.avventendeInnspillTilArbeidsgiver != null } != 0 &&
                healthInformation.perioder.size > 1
        }
    ),

    // Avventende sykmelding må inneholde melding til arbeidsgiver om tilrettelegging
    @Description("Hvis innspill til arbeidsgiver om tilrettelegging i pkt 4.1.3 ikke er utfylt ved avventende sykmelding avvises meldingen")
    MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER(
        1241,
        Status.INVALID,
        "En avventende sykmelding forutsetter at du kan jobbe hvis arbeidsgiveren din legger til rette for det. Den som har sykmeldt deg har ikke foreslått hva arbeidsgiveren kan gjøre, noe som kreves for denne typen sykmelding.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis innspill til arbeidsgiver om tilrettelegging i pkt 4.1.3 ikke er utfylt ved avventende sykmelding avvises meldingen",
        { (healthInformation, _) ->
            healthInformation.perioder
                .any { it.avventendeInnspillTilArbeidsgiver != null && it.avventendeInnspillTilArbeidsgiver?.trim().isNullOrEmpty() }
        }
    ),

    // En avventende sykmelding kan ikke vare mer enn 16 dager
    @Description("Hvis avventende sykmelding benyttes utover arbeidsgiverperioden på 16 kalenderdager, avvises meldingen.")
    AVVENTENDE_SYKMELDING_OVER_16_DAGER(
        1242,
        Status.INVALID,
        "En avventende sykmelding kan bare gis for 16 dager.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis avventende sykmelding benyttes utover arbeidsgiverperioden på 16 kalenderdager, avvises meldingen.",
        { (healthInformation, _) ->
            healthInformation.perioder
                .filter { it.avventendeInnspillTilArbeidsgiver != null }
                .any { (it.fom..it.tom).daysBetween() > 16 }
        }
    ),

    // Sykmelding med behandlingsdager kan ha maks 1 behandlingsdag per uke
    @Description("Hvis antall dager oppgitt for behandlingsdager periode er for høyt i forhold til periodens lengde avvises meldingen. Mer enn en dag per uke er for høyt. 1 dag per påbegynt uke.")
    FOR_MANGE_BEHANDLINGSDAGER_PER_UKE(
        1250,
        Status.INVALID,
        "Det er angitt for mange behandlingsdager. Det kan bare angis én behandlingsdag per uke.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis antall dager oppgitt for behandlingsdager periode er for høyt i forhold til periodens lengde avvises meldingen. Mer enn en dag per uke er for høyt. 1 dag per påbegynt uke.",
        { (healthInformation, _) ->
            healthInformation.perioder.any {
                it.behandlingsdager != null && it.behandlingsdager!! > it.range().startedWeeksBetween()
            }
        }
    ),

    // §8-13, 1. ledd: Dersom medlemmet er delvis arbeidsufør, kan det ytes graderte sykepenger.
    // Det er et vilkår at evnen til å utføre inntektsgivende arbeid er nedsatt med minst 20 prosent.
    @Description("Hvis sykmeldingsgrad er mindre enn 20% for gradert sykmelding, avvises meldingen")
    GRADERT_SYKMELDING_UNDER_20_PROSENT(
        1251,
        Status.INVALID,
        "Sykmeldingsgraden kan ikke være mindre enn 20 %.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis sykmeldingsgrad er mindre enn 20% for gradert sykmelding, avvises meldingen",
        { (healthInformation, _) ->
            healthInformation.perioder.any {
                it.gradert != null && it.gradert!!.grad < 20
            }
        }
    ),

    // Gradering kan ikke være høyere enn 99 prosent
    @Description("Hvis sykmeldingsgrad er høyere enn 99% for delvis sykmelding avvises meldingen")
    GRADERT_SYKMELDING_OVER_99_PROSENT(
        1252,
        Status.INVALID,
        "Sykmeldingsgraden kan ikke være mer enn 99% fordi det er en gradert sykmelding.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis sykmeldingsgrad er høyere enn 99% for delvis sykmelding avvises meldingen",
        { (healthInformation, _) ->
            healthInformation.perioder.mapNotNull { it.gradert }.any { it.grad > 99 }
        }
    ),

    // Sykmelding med behandlingsdager skal alltid til manuell behandling
    @Description("Sykmelding inneholder behandlingsdager")
    SYKMELDING_MED_BEHANDLINGSDAGER(
        1253,
        Status.MANUAL_PROCESSING,
        "Sykmelding inneholder behandlingsdager.",
        "Sykmelding inneholder behandlingsdager (felt 4.4).",
        { (healthInformation, _) ->
            healthInformation.perioder.any {
                it.behandlingsdager != null
            }
        }
    )
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
