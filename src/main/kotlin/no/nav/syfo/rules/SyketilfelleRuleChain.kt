package no.nav.syfo.rules

import no.nav.syfo.model.Rule
import no.nav.syfo.model.RuleChain
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.model.juridisk.JuridiskHenvisning
import no.nav.syfo.model.juridisk.Lovverk
import no.nav.syfo.services.erCoronaRelatert
import no.nav.syfo.services.kommerFraSpesialisthelsetjenesten

class SyketilfelleRuleChain(
    private val sykmelding: Sykmelding,
    private val ruleMetadataSykmelding: RuleMetadataSykmelding,
) : RuleChain {
    override val rules: List<Rule<*>> = listOf(
        // §8-7, 2. ledd: Legeerklæring kan ikke godtas for tidsrom før medlemmet ble undersøkt av lege.
        // En legeerklæring for tidsrom før medlemmet søkte lege kan likevel godtas dersom medlemmet har vært
        // forhidret fra å søke lege og det er godtgjort at han eller hun har vært arbeidsufør fra et tidligere tidspunkt.
        //
        // Dersom sykmeldingen er tilbakedatert mer enn 8 dager uten begrunnelse blir den avvist.
        //
        // Første gangs sykmelding er tilbakedatert mer enn 8 dager.
        Rule(
            name = "TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING",
            ruleId = 1204,
            status = Status.INVALID,
            messageForUser = "Sykmeldingen er tilbakedatert uten begrunnelse fra den som sykmeldte deg.",
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                "Første sykmelding er tilbakedatert mer enn det som er tillatt, eller felt 11.2 er ikke utfylt",
            juridiskHenvisning = JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-7",
                ledd = 2,
                punktum = null,
                bokstav = null
            ),
            input = object {
                val erNyttSyketilfelle = ruleMetadataSykmelding.erNyttSyketilfelle
                val behandletTidspunkt = ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt
                val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
                val begrunnelseIkkeKontakt = sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt
                val erKoronaRelatert = erCoronaRelatert(sykmelding)
            },
            predicate = {
                it.erNyttSyketilfelle &&
                    (it.behandletTidspunkt.toLocalDate() > it.forsteFomDato.plusDays(8) && it.begrunnelseIkkeKontakt.isNullOrEmpty()) &&
                    !it.erKoronaRelatert
            }
        ),

        // §8-7, 2. ledd: Legeerklæring kan ikke godtas for tidsrom før medlemmet ble undersøkt av lege.
        // En legeerklæring for tidsrom før medlemmet søkte lege kan likevel godtas dersom medlemmet har vært
        // forhidret fra å søke lege og det er godtgjort at han eller hun har vært arbeidsufør fra et tidligere tidspunkt.
        //
        // Tilbakedatert sykmelding med begrunnelse sendes til manuell vurdering.
        // Unntak dersom sykmeldingen kommer fra spesialisthelsetjenesten, erstatter en tidligere sykmelding, kun gjelder arbeidsgiverperioden eller er koronarelatert.
        //
        // Første gangs sykmelding er tilbakedatert mer enn 8 dager med begrunnelse.
        Rule(
            name = "TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE",
            ruleId = 1207,
            status = Status.MANUAL_PROCESSING,
            messageForUser = "Første sykmelding er tilbakedatert og årsak for tilbakedatering er angitt.",
            messageForSender = "Første sykmelding er tilbakedatert og felt 11.2 (begrunnelseIkkeKontakt) er utfylt",
            juridiskHenvisning = JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-7",
                ledd = 2,
                punktum = null,
                bokstav = null
            ),
            input = object {
                val erNyttSyketilfelle = ruleMetadataSykmelding.erNyttSyketilfelle
                val erEttersendingAvTidligereSykmelding = ruleMetadataSykmelding.erEttersendingAvTidligereSykmelding
                val erFraSpesialisthelsetjenesten = kommerFraSpesialisthelsetjenesten(sykmelding)
                val behandletTidspunkt = ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt
                val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
                val sisteTomDato = sykmelding.perioder.sortedTOMDate().last()
                val begrunnelseIkkeKontakt = sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt
                val erCoronaRelatert = erCoronaRelatert(sykmelding)
            },
            predicate = {
                it.erNyttSyketilfelle &&
                    it.erEttersendingAvTidligereSykmelding != true &&
                    !it.erFraSpesialisthelsetjenesten &&
                    it.behandletTidspunkt.toLocalDate() > it.forsteFomDato.plusDays(8) &&
                    !it.begrunnelseIkkeKontakt.isNullOrEmpty() &&
                    !it.erCoronaRelatert &&
                    (
                        it.forsteFomDato
                            .plusDays(14)
                            .isBefore(it.sisteTomDato) || it.begrunnelseIkkeKontakt.length < 16
                        )
            }
        ),

        // §8-7 Legeerklæring kan ikke godtas for tidsrom før medlemmet ble undersøkt av lege.
        // En legeerklæring for tidsrom før medlemmet søkte lege kan likevel godtas dersom medlemmet har vært
        // forhidret fra å søke lege og det er godtgjort at han eller hun har vært arbeidsufør fra et tidligere tidspunkt.
        //
        // Tilbakedateringer mellom 4 og 8 dager godtas ikke dersom sykmeldingen ikke inneholder dato for kontakt eller begrunnelse for tilbakedateringen.
        //
        // Første gangs sykmelding er tilbakedatert mindre enn 8 dager uten begrunnelse og kontaktdato.
        Rule(
            name = "TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE",
            ruleId = 1204,
            status = Status.INVALID,
            messageForUser = "Sykmeldingen er tilbakedatert uten begrunnelse eller uten at det er opplyst når du kontaktet den som sykmeldte deg.",
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                "Første sykmelding er tilbakedatert uten at dato for kontakt (felt 11.1) eller at begrunnelse (felt 11.2) er utfylt",
            juridiskHenvisning = JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-7",
                ledd = 2,
                punktum = null,
                bokstav = null
            ),
            input = object {
                val erNyttSyketilfelle = ruleMetadataSykmelding.erNyttSyketilfelle
                val behandletTidspunkt = ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt
                val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
                val sisteTomDato = sykmelding.perioder.sortedTOMDate().last()
                val begrunnelseIkkeKontakt = sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt
                val erCoronaRelatert = erCoronaRelatert(sykmelding)
                val kontaktMedPasientDato = sykmelding.kontaktMedPasient.kontaktDato
            },
            predicate = {
                it.erNyttSyketilfelle &&
                    it.behandletTidspunkt.toLocalDate() > it.forsteFomDato.plusDays(4) &&
                    it.behandletTidspunkt.toLocalDate() <= it.sisteTomDato.plusDays(8) &&
                    (it.kontaktMedPasientDato == null && it.begrunnelseIkkeKontakt.isNullOrEmpty()) &&
                    !it.erCoronaRelatert
            }
        ),

        // §8-7 Legeerklæring kan ikke godtas for tidsrom før medlemmet ble undersøkt av lege.
        // En legeerklæring for tidsrom før medlemmet søkte lege kan likevel godtas dersom medlemmet har vært
        // forhidret fra å søke lege og det er godtgjort at han eller hun har vært arbeidsufør fra et tidligere tidspunkt.
        //
        // Ved forelengelser er det også behov for begrunnelse dersom sykmeldingen er tilbakedatert mer enn 1 mnd. Ved manglende begrunnelse avvises sykmeldingen.
        //
        // Fom-dato i ny sykmelding som er en forlengelse kan maks være tilbakedatert 1 mnd fra behandlet-tidspunkt. Skal telles.
        Rule(
            name = "TILBAKEDATERT_FORLENGELSE_OVER_1_MND",
            ruleId = null,
            status = Status.INVALID,
            messageForUser = "Sykmeldingen er tilbakedatert uten begrunnelse fra den som sykmeldte deg.",
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende: Første sykmelding er tilbakedatert mer enn det som er tillatt, eller felt 11.2 er ikke utfylt.",
            juridiskHenvisning = JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-7",
                ledd = 2,
                punktum = null,
                bokstav = null
            ),
            input = object {
                val erNyttSyketilfelle = ruleMetadataSykmelding.erNyttSyketilfelle
                val behandletTidspunkt = ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt
                val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
                val begrunnelseIkkeKontakt = sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt
                val erCoronaRelatert = erCoronaRelatert(sykmelding)
            },
            predicate = {
                !it.erNyttSyketilfelle &&
                    it.forsteFomDato < it.behandletTidspunkt.toLocalDate().minusMonths(1) &&
                    it.begrunnelseIkkeKontakt.isNullOrEmpty() &&
                    !it.erCoronaRelatert
            }
        ),

        // §8-7 Legeerklæring kan ikke godtas for tidsrom før medlemmet ble undersøkt av lege.
        // En legeerklæring for tidsrom før medlemmet søkte lege kan likevel godtas dersom medlemmet har vært
        // forhidret fra å søke lege og det er godtgjort at han eller hun har vært arbeidsufør fra et tidligere tidspunkt.
        //
        // Mer enn 1mnd tilbakedatert. Samme sjekk som TILBAKEDATERT_FORLENGELSE_OVER_1_MND, men sjekker også om det er tekst i begrunnelsen.
        //
        // Sykmeldingens fom-dato er inntil 3 år tilbake i tid og årsak for tilbakedatering er utilstrekkelig.
        Rule(
            name = "TILBAKEDATERT_MED_UTILSTREKKELIG_BEGRUNNELSE_FORLENGELSE",
            ruleId = 1207,
            status = Status.INVALID,
            messageForUser = "Sykmeldingen er tilbakedatert uten at begrunnelsen for tilbakedatering er god nok",
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                "Fom-dato i ny sykmelding som er en forlengelse kan maks være tilbakedatert 1 mnd fra tidspunkt for behandling uten begrunnelse. Her må årsak i pkt. 11.2 fylles ut.",
            juridiskHenvisning = JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-7",
                ledd = 2,
                punktum = null,
                bokstav = null
            ),
            input = object {
                val erNyttSyketilfelle = ruleMetadataSykmelding.erNyttSyketilfelle
                val behandletTidspunkt = ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt
                val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
                val begrunnelseIkkeKontakt = sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt
                val erCoronaRelatert = erCoronaRelatert(sykmelding)
                val utdypendeOpplysninger = sykmelding.utdypendeOpplysninger
                val meldingTilNav = sykmelding.meldingTilNAV
            },
            predicate = {
                !it.erNyttSyketilfelle &&
                    it.behandletTidspunkt.toLocalDate() > it.forsteFomDato.plusDays(30) &&
                    !it.begrunnelseIkkeKontakt.isNullOrEmpty() &&
                    !it.erCoronaRelatert &&
                    (
                        !it.begrunnelseIkkeKontakt.contains("""[A-Za-z]""".toRegex()) &&
                            it.utdypendeOpplysninger.isEmpty() &&
                            it.meldingTilNav?.beskrivBistand.isNullOrEmpty()
                        )
            }
        ),

        // §8-7 Legeerklæring kan ikke godtas for tidsrom før medlemmet ble undersøkt av lege.
        // En legeerklæring for tidsrom før medlemmet søkte lege kan likevel godtas dersom medlemmet har vært
        // forhidret fra å søke lege og det er godtgjort at han eller hun har vært arbeidsufør fra et tidligere tidspunkt.
        //
        // Dersom sykmeldingen er tilbakedatert mer enn 30 dager og begrunnelse er angitt går den til manuell behandling
        //
        // Sykmeldingen er tilbakedatert mer enn 30 dager og årsak for tilbakedatering er angitt.
        Rule(
            name = "TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE",
            ruleId = 1207,
            status = Status.MANUAL_PROCESSING,
            messageForUser = "Sykmeldingen er tilbakedatert og årsak for tilbakedatering er angitt",
            messageForSender = "Sykmeldingen er tilbakedatert og felt 11.2 (begrunnelseIkkeKontakt) er utfylt",
            juridiskHenvisning = JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-7",
                ledd = 2,
                punktum = null,
                bokstav = null
            ),
            input = object {
                val erNyttSyketilfelle = ruleMetadataSykmelding.erNyttSyketilfelle
                val erEttersendingAvTidligereSykmelding = ruleMetadataSykmelding.erEttersendingAvTidligereSykmelding
                val erFraSpesialisthelsetjenesten = kommerFraSpesialisthelsetjenesten(sykmelding)
                val behandletTidspunkt = ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt
                val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
                val begrunnelseIkkeKontakt = sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt
                val erCoronaRelatert = erCoronaRelatert(sykmelding)
                val utdypendeOpplysninger = sykmelding.utdypendeOpplysninger
                val meldingTilNav = sykmelding.meldingTilNAV
            },
            predicate = {
                !it.erNyttSyketilfelle && it.erEttersendingAvTidligereSykmelding != true &&
                    !it.erFraSpesialisthelsetjenesten &&
                    it.behandletTidspunkt.toLocalDate() > it.forsteFomDato.plusDays(30) &&
                    !it.begrunnelseIkkeKontakt.isNullOrEmpty() &&
                    !it.erCoronaRelatert &&
                    !(
                        !it.begrunnelseIkkeKontakt.contains("""[A-Za-z]""".toRegex()) &&
                            it.utdypendeOpplysninger.isEmpty() &&
                            it.meldingTilNav?.beskrivBistand.isNullOrEmpty()
                        )
            }
        ),

        // §8-7 Legeerklæring kan ikke godtas for tidsrom før medlemmet ble undersøkt av lege.
        // En legeerklæring for tidsrom før medlemmet søkte lege kan likevel godtas dersom medlemmet har vært
        // forhidret fra å søke lege og det er godtgjort at han eller hun har vært arbeidsufør fra et tidligere tidspunkt.
        //
        // En tilbakedatert sykmelding må inneholde dato for kontakt eller begrunnelse for at den skal godkjennes.
        //
        // Sykmelding som er forlengelse er tilbakedatert mindre enn 30 dager uten begrunnelse og kontaktdato.
        Rule(
            name = "TILBAKEDATERT_FORLENGELSE_UNDER_1_MND",
            ruleId = 1207,
            status = Status.INVALID,
            messageForUser = "Sykmeldingen er tilbakedatert uten begrunnelse eller uten at det er opplyst når du kontaktet den som sykmeldte deg.",
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                "Sykmelding er tilbakedatert uten at dato for kontakt (felt 11.1) eller at begrunnelse (felt 11.2) er utfylt",
            juridiskHenvisning = JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-7",
                ledd = 2,
                punktum = null,
                bokstav = null
            ),
            input = object {
                val erNyttSyketilfelle = ruleMetadataSykmelding.erNyttSyketilfelle
                val behandletDato = ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt.toLocalDate()
                val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
                val kontaktDato = sykmelding.kontaktMedPasient.kontaktDato
                val begrunnelseIkkeKontakt = sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt
                val erCoronaRelatert = erCoronaRelatert(sykmelding)
            },
            predicate = { input ->
                !input.erNyttSyketilfelle &&
                    input.behandletDato > input.forsteFomDato.plusDays(4) &&
                    input.behandletDato <= input.forsteFomDato.plusDays(30) &&
                    (
                        input.kontaktDato == null &&
                            (input.begrunnelseIkkeKontakt.isNullOrEmpty() || !input.begrunnelseIkkeKontakt.contains("""[A-Za-z]""".toRegex())) &&
                            !input.erCoronaRelatert
                        )
            }
        )
    )
}

data class RuleMetadataSykmelding(
    val ruleMetadata: RuleMetadata,
    val erNyttSyketilfelle: Boolean,
    val erEttersendingAvTidligereSykmelding: Boolean?,
)
