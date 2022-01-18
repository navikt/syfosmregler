package no.nav.syfo.rules

import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.services.erCoronaRelatert
import no.nav.syfo.services.kommerFraSpesialisthelsetjenesten

enum class SyketilfelleRuleChain(
    override val ruleId: Int?,
    override val status: Status,
    override val messageForUser: String,
    override val messageForSender: String,
    override val predicate: (RuleData<RuleMetadataSykmelding>) -> Boolean
) : Rule<RuleData<RuleMetadataSykmelding>> {
    
    
    // §8-7 Legeerklæring kan ikke godtas for tidsrom før medlemmet ble undersøkt av lege.
    // En legeerklæring for tidsrom før medlemmet søkte lege kan likevel godtas dersom medlemmet har vært 
    // forhidret fra å søke lege og det er godtgjort at han eller hun har vært arbeidsufør fra et tidligere tidspunkt.
    //
    // Dersom sykmeldingen er tilbakedatert mer enn 8 dager uten begrunnelse blir den avvist.
    //
    @Description("Første gangs sykmelding er tilbakedatert mer enn 8 dager.")
    TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING(
        1204,
        Status.INVALID,
        "Sykmeldingen er tilbakedatert uten begrunnelse fra den som sykmeldte deg.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Første sykmelding er tilbakedatert mer enn det som er tillatt, eller felt 11.2 er ikke utfylt",
        { (healthInformation, ruleMetadataSykmelding) ->
            ruleMetadataSykmelding.erNyttSyketilfelle &&
                (
                    ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt.toLocalDate() > healthInformation.perioder.sortedFOMDate().first().plusDays(8) &&
                        healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty()
                    ) &&
                !erCoronaRelatert(healthInformation)
        }
    ),
    
    // §8-7 Legeerklæring kan ikke godtas for tidsrom før medlemmet ble undersøkt av lege.
    // En legeerklæring for tidsrom før medlemmet søkte lege kan likevel godtas dersom medlemmet har vært 
    // forhidret fra å søke lege og det er godtgjort at han eller hun har vært arbeidsufør fra et tidligere tidspunkt.
    //    
    // Tilbakedatert sykmelding med begrunnelse sendes til manuell vurdering.
    // Unntak dersom sykmeldingen kommer fra spesialisthelsetjenesten, erstatter en tidligere sykmelding, kun gjelder arbeidsgiverperioden eller er koronarelatert.
    //
    @Description("Første gangs sykmelding er tilbakedatert mer enn 8 dager med begrunnelse.")
    TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE(
        1207,
        Status.MANUAL_PROCESSING,
        "Første sykmelding er tilbakedatert og årsak for tilbakedatering er angitt.",
        "Første sykmelding er tilbakedatert og felt 11.2 (begrunnelseIkkeKontakt) er utfylt",
        { (healthInformation, ruleMetadataSykmelding) ->
            ruleMetadataSykmelding.erNyttSyketilfelle && ruleMetadataSykmelding.erEttersendingAvTidligereSykmelding != true && !kommerFraSpesialisthelsetjenesten(healthInformation) &&
                ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt.toLocalDate() > healthInformation.perioder.sortedFOMDate().first().plusDays(8) &&
                !healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty() &&
                !erCoronaRelatert(healthInformation) &&
                (
                    healthInformation.perioder.sortedFOMDate().first().plusDays(14).isBefore(healthInformation.perioder.sortedTOMDate().last()) ||
                        healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt!!.length < 16
                    )
        }
    ),
    
    // §8-7 Legeerklæring kan ikke godtas for tidsrom før medlemmet ble undersøkt av lege.
    // En legeerklæring for tidsrom før medlemmet søkte lege kan likevel godtas dersom medlemmet har vært 
    // forhidret fra å søke lege og det er godtgjort at han eller hun har vært arbeidsufør fra et tidligere tidspunkt.
    //
    // Tilbakedateringer mellom 4 og 8 dager godtas ikke dersom sykmeldingen ikke inneholder dato for kontakt eller begrunnelse for tilbakedateringen.
    //
    @Description("Første gangs sykmelding er tilbakedatert mindre enn 8 dager uten begrunnelse og kontaktdato.")
    TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE(
        1204,
        Status.INVALID,
        "Sykmeldingen er tilbakedatert uten begrunnelse eller uten at det er opplyst når du kontaktet den som sykmeldte deg.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Første sykmelding er tilbakedatert uten at dato for kontakt (felt 11.1) eller at begrunnelse (felt 11.2) er utfylt",
        { (healthInformation, ruleMetadataSykmelding) ->
            ruleMetadataSykmelding.erNyttSyketilfelle &&
                ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt.toLocalDate() > healthInformation.perioder.sortedFOMDate().first().plusDays(4) &&
                ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt.toLocalDate() <= healthInformation.perioder.sortedFOMDate().first().plusDays(8) &&
                (healthInformation.kontaktMedPasient.kontaktDato == null && healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty()) &&
                !erCoronaRelatert(healthInformation)
        }
    ),
    
    // §8-7 Legeerklæring kan ikke godtas for tidsrom før medlemmet ble undersøkt av lege.
    // En legeerklæring for tidsrom før medlemmet søkte lege kan likevel godtas dersom medlemmet har vært 
    // forhidret fra å søke lege og det er godtgjort at han eller hun har vært arbeidsufør fra et tidligere tidspunkt.
    //    
    // KAN DENNE REGELEN SLETTES?? - ERSTATTES AV TILBAKEDATERT_MED_UTILSTREKKELIG_BEGRUNNELSE_FORLENGELSE ??
    // Ved forelengelser er det også behov for begrunnelse dersom sykmeldingen er tilbakedatert mer enn 1 mnd. Ved manglende begrunnelse avvises sykmeldingen.
    //
    @Description("Fom-dato i ny sykmelding som er en forlengelse kan maks være tilbakedatert 1 mnd fra behandlet-tidspunkt. Skal telles.")
    TILBAKEDATERT_FORLENGELSE_OVER_1_MND(
        null,
        Status.INVALID,
        "Sykmeldingen er tilbakedatert uten begrunnelse fra den som sykmeldte deg.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Fom-dato i ny sykmelding som er en forlengelse kan maks være tilbakedatert 1 mnd fra tidspunkt for behandling og felt 11.2 er ikke utfylt",
        { (healthInformation, ruleMetadataSykmelding) ->
            !ruleMetadataSykmelding.erNyttSyketilfelle &&
                healthInformation.perioder.sortedFOMDate().first() < ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt.toLocalDate().minusMonths(1) &&
                healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty() &&
                !erCoronaRelatert(healthInformation)
        }
    ),
    
    // §8-7 Legeerklæring kan ikke godtas for tidsrom før medlemmet ble undersøkt av lege.
    // En legeerklæring for tidsrom før medlemmet søkte lege kan likevel godtas dersom medlemmet har vært 
    // forhidret fra å søke lege og det er godtgjort at han eller hun har vært arbeidsufør fra et tidligere tidspunkt.
    // 
    // Mer enn 1mnd tilbakedatert. Samme sjekk som TILBAKEDATERT_FORLENGELSE_OVER_1_MND, men sjekker også om det er tekst i begrunnelsen. 
    //
    @Description("Sykmeldingens fom-dato er inntil 3 år tilbake i tid og årsak for tilbakedatering er utilstrekkelig.")
    TILBAKEDATERT_MED_UTILSTREKKELIG_BEGRUNNELSE_FORLENGELSE(
        1207,
        Status.INVALID,
        "Sykmeldingen er tilbakedatert uten at begrunnelsen for tilbakedatering er god nok",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Sykmeldingen er tilbakedatert uten at begrunnelsen for tilbakedatering (felt 11.2) er god nok",
        { (healthInformation, ruleMetadataSykmelding) ->
            !ruleMetadataSykmelding.erNyttSyketilfelle &&
                ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt.toLocalDate() > healthInformation.perioder.sortedFOMDate().first().plusDays(30) &&
                !healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty() &&
                !erCoronaRelatert(healthInformation) &&
                (
                    !healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt!!.contains("""[A-Za-z]""".toRegex()) && healthInformation.utdypendeOpplysninger.isEmpty() &&
                        healthInformation.meldingTilNAV?.beskrivBistand.isNullOrEmpty()
                    )
        }
    ),
    
    // §8-7 Legeerklæring kan ikke godtas for tidsrom før medlemmet ble undersøkt av lege.
    // En legeerklæring for tidsrom før medlemmet søkte lege kan likevel godtas dersom medlemmet har vært 
    // forhidret fra å søke lege og det er godtgjort at han eller hun har vært arbeidsufør fra et tidligere tidspunkt.
    //   
    // Dersom sykmeldingen er tilbakedatert mer enn 30 dager og begrunnelse er angitt går den til manuell behandling
    //
    @Description("Sykmeldingen er tilbakedatert mer enn 30 dager og årsak for tilbakedatering er angitt.")
    TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE(
        1207,
        Status.MANUAL_PROCESSING,
        "Sykmeldingen er tilbakedatert og årsak for tilbakedatering er angitt",
        "Sykmeldingen er tilbakedatert og felt 11.2 (begrunnelseIkkeKontakt) er utfylt",
        { (healthInformation, ruleMetadataSykmelding) ->
            !ruleMetadataSykmelding.erNyttSyketilfelle && ruleMetadataSykmelding.erEttersendingAvTidligereSykmelding != true && !kommerFraSpesialisthelsetjenesten(healthInformation) &&
                ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt.toLocalDate() > healthInformation.perioder.sortedFOMDate().first().plusDays(30) &&
                !healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty() &&
                !erCoronaRelatert(healthInformation) &&
                !(
                    !healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt!!.contains("""[A-Za-z]""".toRegex()) && healthInformation.utdypendeOpplysninger.isEmpty() &&
                        healthInformation.meldingTilNAV?.beskrivBistand.isNullOrEmpty()
                    )
        }
    ),
    
    // §8-7 Legeerklæring kan ikke godtas for tidsrom før medlemmet ble undersøkt av lege.
    // En legeerklæring for tidsrom før medlemmet søkte lege kan likevel godtas dersom medlemmet har vært 
    // forhidret fra å søke lege og det er godtgjort at han eller hun har vært arbeidsufør fra et tidligere tidspunkt.
    //    
    // En tilbakedatert sykmelding må inneholde dato for kontakt eller begrunnelse for at den skal godkjennes.
    //
    @Description("Sykmelding som er forlengelse er tilbakedatert mindre enn 30 dager uten begrunnelse og kontaktdato.")
    TILBAKEDATERT_FORLENGELSE_UNDER_1_MND(
        1207,
        Status.INVALID,
        "Sykmeldingen er tilbakedatert uten begrunnelse eller uten at det er opplyst når du kontaktet den som sykmeldte deg.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Sykmelding er tilbakedatert uten at dato for kontakt (felt 11.1) eller at begrunnelse (felt 11.2) er utfylt",
        { (healthInformation, ruleMetadataSykmelding) ->
            !ruleMetadataSykmelding.erNyttSyketilfelle &&
                ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt.toLocalDate() > healthInformation.perioder.sortedFOMDate().first().plusDays(4) &&
                ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt.toLocalDate() <= healthInformation.perioder.sortedFOMDate().first().plusDays(30) &&
                (
                    healthInformation.kontaktMedPasient.kontaktDato == null &&
                        (healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty() || !healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt!!.contains("""[A-Za-z]""".toRegex())) &&
                        !erCoronaRelatert(healthInformation)
                    )
        }
    ),
}

data class RuleMetadataSykmelding(
    val ruleMetadata: RuleMetadata,
    val erNyttSyketilfelle: Boolean,
    val erEttersendingAvTidligereSykmelding: Boolean?
)
