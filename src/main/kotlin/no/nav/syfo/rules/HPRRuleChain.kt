package no.nav.syfo.rules

import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.model.Status
import no.nhn.schemas.reg.hprv2.Person as HPRPerson

enum class HPRRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<HPRPerson>) -> Boolean) : Rule<RuleData<HPRPerson>> {
    // TODO: 1401, 1402, 1403, 1404: See spreadsheet
    // 1401: Behandler ikke registrert i HPR
    // Kommentar fra Camilla: Denne vil endres. I ny løsning skal vi slå opp mot HPR. Dersom behandler ikke ligger i HPR vil meldingen avvises.
    // Dersom det kommer soapfault, som har følgende faultstring: ArgumentException: Personnummer ikke funnet

    // 1402: Behandler har ikke vært en gyldig behandler under konsultasjonstidspunktet. Ser ut som om dette gjelder behandlers autorisasjon
    // Kommentar fra Camilla: 13 i 2017. Denne må sees i sammenheng med 1317 - eller er dette knyttet mot behandlers autorisasjon?
    // Sjekk OID og Verdi
    @Description("Behandler er ikke gyldig i HPR på konsultasjonstidspunkt..")
    BEHANDLER_NOT_VALDIG_IN_HPR(1402, Status.MANUAL_PROCESSING, { (_, doctor) ->
        if (!doctor.godkjenninger.godkjenning.isNullOrEmpty()) {
            !doctor.godkjenninger.godkjenning.any {
                it.autorisasjon.isAktiv
            }
        } else {
            false
        }
    }),
    // 1403: Behandler er registrert med status kode OPPH, forkortelse for opphørt
    // Kommentar fra Camilla: Antar at denne skal være knyttet opp mot "trygdens rett til å sykmelde" + autoriasjon i HPR. Må sjekke hvilke status som ligger i HPR + samarbeide med Team registre for å finne ut hvordan vi ivaretar informasjon om egne data.
    @Description("Behandler registrert med status Opphørt i TSS")
    BEHANDLER_OPP_IN_HPR(1403, Status.MANUAL_PROCESSING, { (_, doctor) ->
        !doctor.godkjenninger.godkjenning.any {
            it.autorisasjon.isAktiv && it.autorisasjon.oid == 7704 && it.autorisasjon.verdi == "1"
        }
    }),
    // 1404: Hvis behandler er suspendert i TSS.
    // Kommentar fra Camilla: 13 i 2017. Sees sammen med 1403.
    // Burde bli sjekket i TSS da dette er nav spesifikk behandler informasjon, sjekk ut registeret: https://jira.adeo.no/browse/REG-1397
    @Description("Behandler registrert med status Suspendert i TSS")
    BEHANDLER_SUSP_IN_HPR(1404, Status.MANUAL_PROCESSING, { (_, doctor) ->
        !doctor.godkjenninger.godkjenning.any {
            it.autorisasjon.isAktiv && it.autorisasjon.oid == 7704 && it.autorisasjon.verdi == "1"
        }
    }),

    // 1405: Hvis behandler har andre statuskoder enn Opphør,suspendert eller Død skal meldingen gå til manuell behandling.
    // Kommentar fra Camilla: Hva kan disse være? Målet må være å få til automatisert behandling slik at vi enten avviser eller godtar. Unngå manuell behandling.
    @Description("Behandler registrert med status Andre årsaker i TSS")
    BEHANDLER_ANDRE_IN_HPR(1405, Status.MANUAL_PROCESSING, { (_, doctor) ->
        !doctor.godkjenninger.godkjenning.any {
            it.autorisasjon.isAktiv && it.autorisasjon.oid == 7704 && it.autorisasjon.verdi == "1"
        }
    }),

    // 1407: Hvis behandler ikke har en samhandlertype som kan sykmelde skal meldingen gå til manuell behandling
    // Kommentar fra Camilla: 596 i 2017. Er dette fysioterapeuter (også annet helsepersonell)? Bør sjekkes litt hva som kommer inn. I fremtiden sjekke mot HPR.
    @Description("Behandler finnes i TSS men er ikke lege, kiropraktor, manuellterapeut eller tannlege")
    BEHANDLER_NOT_LE_KI_MT_TL_IN_HPR(1407, Status.MANUAL_PROCESSING, { (_, doctor) ->
        if (!doctor.godkjenninger?.godkjenning.isNullOrEmpty()) {
        !doctor.godkjenninger.godkjenning.any {
            if (it?.helsepersonellkategori?.isAktiv != null && it.autorisasjon != null && !it.helsepersonellkategori.verdi.isNullOrEmpty()) {
            it.autorisasjon.isAktiv && it.helsepersonellkategori.let { it.isAktiv && it.verdi in listOf("LE", "KI", "MT", "TL") }
            } else {
                false
            }
        }
        } else {
            false
        }
        }),
    // 1413: Hvis behanlder mangler autorisasjon for obligatorisk opplæring  avvises meldingen
    // Kommentar fra Camilla: Gjelder legene - har aldri vært i drift. Må ha lederforankring dersom denne skal fjernes (pga forskriften). Må være sjekk for kiropraktorer og manuell terapeuter

    // 1414: Hvis behandler har autorisasjonskode Suspendert av NAV  sendes meldingen til manuell behandling.
    // Kommentar fra Camilla: Denne bør vel heller avvises? Pasienten må få beskjed: Sykmeldingen kan ikke godtas. Oppfølgingsoppgave til NAV Kontroll?
    // Burde bli sjekket i TSS da dette er nav spesifikk behandler informasjon, sjekk ut registeret: https://jira.adeo.no/browse/REG-1397
}
