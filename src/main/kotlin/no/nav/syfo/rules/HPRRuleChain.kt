package no.nav.syfo.rules

import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.model.Status

enum class HPRRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<RuleMetadata>) -> Boolean) : Rule<RuleData<RuleMetadata>> {
    // TODO: 1401, 1402, 1403, 1404: See spreadsheet
    // 1401: Behandler ikke registrert i HPR
    // Kommentar fra Camilla: Denne vil endres. I ny løsning skal vi slå opp mot HPR. Dersom behandler ikke ligger i HPR vil meldingen avvises.
    // Dersom det kommer soapfault, som har følgende faultstring: ArgumentException: Personnummer ikke funnet
    @Description("Behandler er ikke registrert i HPR.")
    BEHANDLER_NOT_IN_HPR(1401, Status.MANUAL_PROCESSING, { (_, _, _, _, doctor) ->
        doctor.fødselsdato.isNil
    }),

    // 1402: Behandler har ikke vært en gyldig behandler under konsultasjonstidspunktet. Ser ut som om dette gjelder behandlers autorisasjon
    // Kommentar fra Camilla: 13 i 2017. Denne må sees i sammenheng med 1317 - eller er dette knyttet mot behandlers autorisasjon?
    @Description("Behandler er ikke gyldig i HPR på konsultasjonstidspunkt..")
    BEHANDLER_NOT_VALDIG_IN_HPR(1402, Status.MANUAL_PROCESSING, { (_, _, _, _, doctor) ->
        doctor.godkjenninger.value.godkjenning.any{
            it.autorisasjon.value.isAktiv
        }
    }),
    // 1403: Behandler er registrert med status kode OPPH, forkortelse for opphørt
    // Kommentar fra Camilla: Antar at denne skal være knyttet opp mot "trygdens rett til å sykmelde" + autoriasjon i HPR. Må sjekke hvilke status som ligger i HPR + samarbeide med Team registre for å finne ut hvordan vi ivaretar informasjon om egne data.
    @Description("Behandler er ikke gyldig i HPR på konsultasjonstidspunkt..")
    BEHANDLER_NOT_AUTH_IN_HPR(1403, Status.MANUAL_PROCESSING, { (_, _, _, _, doctor) ->
        doctor.godkjenninger.isNil
    })
    // 1404: Hvis behandler er suspendert i TSS.
    // Kommentar fra Camilla: 13 i 2017. Sees sammen med 1403.

    // 1405: Hvis behandler har andre statuskoder enn Opphør,suspendert eller Død skal meldingen gå til manuell behandling.
    // Kommentar fra Camilla: Hva kan disse være? Målet må være å få til automatisert behandling slik at vi enten avviser eller godtar. Unngå manuell behandling.

    // 1407: Hvis behandler ikke har en samhandlertype som kan sykmelde skal meldingen gå til manuell behandling
    // Kommentar fra Camilla: 596 i 2017. Er dette fysioterapeuter (også annet helsepersonell)? Bør sjekkes litt hva som kommer inn. I fremtiden sjekke mot HPR.

    // 1413: Hvis behanlder mangler autorisasjon for obligatorisk opplæring  avvises meldingen
    // Kommentar fra Camilla: Gjelder legene - har aldri vært i drift. Må ha lederforankring dersom denne skal fjernes (pga forskriften). Må være sjekk for kiropraktorer og manuell terapeuter

    // 1414: Hvis behandler har autorisasjonskode Suspendert av NAV  sendes meldingen til manuell behandling.
    // Kommentar fra Camilla: Denne bør vel heller avvises? Pasienten må få beskjed: Sykmeldingen kan ikke godtas. Oppfølgingsoppgave til NAV Kontroll?
}
