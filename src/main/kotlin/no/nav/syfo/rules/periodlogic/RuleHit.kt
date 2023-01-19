package no.nav.syfo.rules.periodlogic

import no.nav.syfo.model.Status
import no.nav.syfo.model.juridisk.JuridiskHenvisning
import no.nav.syfo.model.juridisk.Lovverk

enum class RuleHit(
    val messageForSender: String,
    val messageForUser: String,
    val juridiskHenvisning: JuridiskHenvisning?,
    status: Status
) {
    PERIODER_MANGLER(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å " +
            "vente på ny sykmelding fra deg. Grunnet følgende: " +
            "Hvis ingen perioder er oppgitt skal sykmeldingen avvises.",
        messageForUser = "Det er ikke oppgitt hvilken periode sykmeldingen gjelder for.",
        juridiskHenvisning = null
    ),
    FRADATO_ETTER_TILDATO(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente " +
            "på ny sykmelding fra deg. Grunnet følgende: " +
            "Hvis tildato for en periode ligger før fradato avvises meldingen og hvilken periode det gjelder oppgis.",
        messageForUser = "Det er lagt inn datoer som ikke stemmer innbyrdes.",
        juridiskHenvisning = null
    ),
    OVERLAPPENDE_PERIODER(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på" +
            " ny sykmelding fra deg. Grunnet følgende: " +
            "Hvis en eller flere perioder er overlappende avvises meldingen og hvilken periode det gjelder oppgis.",
        messageForUser = "Periodene må ikke overlappe hverandre.",
        juridiskHenvisning = null
    ),
    OPPHOLD_MELLOM_PERIODER(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å " +
            "vente på ny sykmelding fra deg. Grunnet følgende: " +
            "Hvis det finnes opphold mellom perioder i sykmeldingen avvises meldingen.",
        messageForUser = "Det er opphold mellom sykmeldingsperiodene.",
        juridiskHenvisning = null
    ),
    IKKE_DEFINERT_PERIODE(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å" +
            " vente på ny sykmelding fra deg. Grunnet følgende: " +
            "Det er ikke oppgitt type for sykmeldingen " +
            "(den må være enten 100 prosent, gradert, avventende, reisetilskudd eller behandlingsdager).",
        messageForUser = "Det er ikke oppgitt type for sykmeldingen " +
            "(den må være enten 100 prosent, gradert, avventende, reisetilskudd eller behandlingsdager).",
        juridiskHenvisning = null
    ),
    TILBAKEDATERT_MER_ENN_3_AR(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
            "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende: " +
            "Sykmeldinges fom-dato er mer enn 3 år tilbake i tid.",
        messageForUser = "Startdatoen er mer enn tre år tilbake.",
        juridiskHenvisning = null
    ),
    FREMDATERT(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
            "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis sykmeldingen er fremdatert mer enn 30 dager etter behandletDato avvises meldingen.",
        messageForUser = "Sykmeldingen er datert mer enn 30 dager fram i tid.",
        juridiskHenvisning = null
    ),
    TOTAL_VARIGHET_OVER_ETT_AAR(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
            "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Sykmeldingen første fom og siste tom har ein varighet som er over 1 år",
        messageForUser = "Den kan ikke ha en varighet på over ett år.",
        juridiskHenvisning = null
    ),
    BEHANDLINGSDATO_ETTER_MOTTATTDATO(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
            "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Behandlingsdatoen er etter dato for når NAV mottok meldingen",
        messageForUser = "Behandlingsdatoen må rettes.",
        juridiskHenvisning = null
    ),
    AVVENTENDE_SYKMELDING_KOMBINERT(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
            "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Avventende sykmelding kan ikke inneholde flere perioder.",
        messageForUser = "En avventende sykmelding kan bare inneholde én periode.",
        juridiskHenvisning = null
    ),
    MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis innspill til arbeidsgiver om tilrettelegging i pkt 4.1.3 ikke er utfylt ved avventende sykmelding avvises meldingen",
        messageForUser = "En avventende sykmelding forutsetter at du kan jobbe hvis arbeidsgiveren din legger til " +
            "rette for det. Den som har sykmeldt deg har ikke foreslått hva arbeidsgiveren kan gjøre, " +
            "noe som kreves for denne typen sykmelding.",
        juridiskHenvisning = null
    ),
    AVVENTENDE_SYKMELDING_OVER_16_DAGER(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
            "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis avventende sykmelding benyttes utover arbeidsgiverperioden på 16 kalenderdager," +
            " avvises meldingen.",
        messageForUser = "En avventende sykmelding kan bare gis for 16 dager.",
        juridiskHenvisning = null
    ),
    FOR_MANGE_BEHANDLINGSDAGER_PER_UKE(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
            "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis antall dager oppgitt for behandlingsdager periode er for høyt" +
            " i forhold til periodens lengde avvises meldingen. Mer enn en dag per uke er for høyt." +
            " 1 dag per påbegynt uke.",
        messageForUser = "Det er angitt for mange behandlingsdager. Det kan bare angis én behandlingsdag per uke.",
        juridiskHenvisning = null
    ),
    GRADERT_SYKMELDING_UNDER_20_PROSENT(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
            "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis sykmeldingsgrad er mindre enn 20% for gradert sykmelding, avvises meldingen",
        messageForUser = "Sykmeldingsgraden kan ikke være mindre enn 20 %.",
        juridiskHenvisning = JuridiskHenvisning(
            lovverk = Lovverk.FOLKETRYGDLOVEN,
            paragraf = "8-13",
            ledd = 1,
            punktum = null,
            bokstav = null
        )
    ),
    GRADERT_SYKMELDING_OVER_99_PROSENT(
        status = Status.INVALID,
        messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
            "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Hvis sykmeldingsgrad er høyere enn 99% for delvis sykmelding avvises meldingen",
        messageForUser = "Sykmeldingsgraden kan ikke være mer enn 99% fordi det er en gradert sykmelding.",
        juridiskHenvisning = null
    ),
    SYKMELDING_MED_BEHANDLINGSDAGER(
        status = Status.MANUAL_PROCESSING,
        messageForSender = "Sykmelding inneholder behandlingsdager (felt 4.4).",
        messageForUser = "Sykmelding inneholder behandlingsdager.",
        juridiskHenvisning = null
    )
}
