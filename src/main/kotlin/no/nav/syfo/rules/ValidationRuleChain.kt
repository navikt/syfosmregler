package no.nav.syfo.rules

import no.nav.syfo.QuestionGroup
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.RuleThingy
import no.nav.syfo.model.SporsmalSvar
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.model.juridisk.JuridiskHenvisning
import no.nav.syfo.model.juridisk.Lovverk
import no.nav.syfo.sm.Diagnosekoder
import no.nav.syfo.sm.isICPC2

class ValidationRuleChain(
    private val sykmelding: Sykmelding,
    private val metadata: RuleMetadata,
) {
    val rules: List<RuleThingy<*>>

    fun getByName(name: String): RuleThingy<*> {
        return rules.find { name == it.name } ?: throw IllegalArgumentException("Unable to find with name $name")
    }

    init {
        rules = listOf(
            // Opptjening før 13 år er ikke mulig.
            // Hele sykmeldingsperioden er før bruker har fylt 13 år. Pensjonsopptjening kan starte fra 13 år.
            RuleThingy(
                name = "PASIENT_YNGRE_ENN_13",
                ruleId = 1101,
                status = Status.INVALID,
                messageForUser = "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
                messageForSender = "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
                null,
                object {
                    val sisteTomDato = sykmelding.perioder.sortedTOMDate().last()
                    val pasientFodselsdato = metadata.pasientFodselsdato
                },
            ) { it.sisteTomDato < it.pasientFodselsdato.plusYears(13) },

            // §8-3 Det ytes ikke sykepenger til medlem som er fylt 70 år.
            // Hele sykmeldingsperioden er etter at bruker har fylt 70 år. Dersom bruker fyller 70 år i perioden skal sykmelding gå gjennom på vanlig måte.
            RuleThingy(
                name = "PASIENT_ELDRE_ENN_70",
                ruleId = 1102,
                status = Status.INVALID,
                messageForUser = "Sykmelding kan ikke benyttes etter at du har fylt 70 år",
                messageForSender = "Pasienten er over 70 år. Sykmelding kan ikke benyttes. Pasienten har fått beskjed.",
                JuridiskHenvisning(
                    lovverk = Lovverk.FOLKETRYGDLOVEN,
                    paragraf = "8-3",
                    ledd = 1,
                    punktum = 2,
                    bokstav = null
                ),
                object {
                    val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
                    val pasientFodselsdato = metadata.pasientFodselsdato
                }
            ) {
                it.forsteFomDato > it.pasientFodselsdato.plusYears(70)
            },

            // §8-4 Sykmeldingen må angi sykdom eller skade eller annen gyldig fraværsgrunn som angitt i loven.
            // Kodeverk må være satt i henhold til gyldige kodeverk som angitt av Helsedirektoratet (ICPC-2 og ICD-10).
            // Ukjent houved diagnosekode type
            RuleThingy(
                name = "UKJENT_DIAGNOSEKODETYPE",
                ruleId = 1137,
                status = Status.INVALID,
                messageForUser = "Sykmeldingen må ha et kjent kodeverk for diagnosen.",
                messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Ukjent kodeverk er benyttet for diagnosen.",
                JuridiskHenvisning(
                    lovverk = Lovverk.FOLKETRYGDLOVEN,
                    paragraf = "8-4",
                    ledd = 1,
                    punktum = 1,
                    bokstav = null
                ),
                object {
                    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose
                }
            ) {
                it.hoveddiagnose != null && it.hoveddiagnose.system !in Diagnosekoder
            },

            // §8-4 Arbeidsuførhet som skyldes sosiale eller økomoniske problemer o.l. gir ikke rett til sykepenger.
            // Hvis hoveddiagnose er Z-diagnose (ICPC-2), avvises meldingen.
            RuleThingy(
                name = "ICPC_2_Z_DIAGNOSE",
                ruleId = 1132,
                status = Status.INVALID,
                messageForUser = "Den må ha en gyldig diagnosekode som gir rett til sykepenger.",
                messageForSender = "Angitt hoveddiagnose (z-diagnose) gir ikke rett til sykepenger. Pasienten har fått beskjed.",
                JuridiskHenvisning(
                    lovverk = Lovverk.FOLKETRYGDLOVEN,
                    paragraf = "8-4",
                    ledd = 1,
                    punktum = 2,
                    bokstav = null
                ),
                object {
                    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose
                }
            ) {
                it.hoveddiagnose != null && it.hoveddiagnose.isICPC2() && it.hoveddiagnose.kode.startsWith("Z")
            },

            // §8-4 Sykmeldingen må angi sykdom eller skade eller annen gyldig fraværsgrunn som angitt i loven.
            // Hvis hoveddiagnose mangler og det ikke er angitt annen lovfestet fraværsgrunn, avvises meldingen
            RuleThingy(
                name = "HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER",
                ruleId = 1133,
                status = Status.INVALID,
                messageForUser = "Den må ha en hoveddiagnose eller en annen gyldig fraværsgrunn.",
                messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Hoveddiagnose eller annen lovfestet fraværsgrunn mangler. ",
                JuridiskHenvisning(
                    lovverk = Lovverk.FOLKETRYGDLOVEN,
                    paragraf = "8-4",
                    ledd = 1,
                    punktum = 1,
                    bokstav = null
                ),
                object {
                    val annenFraversArsak = sykmelding.medisinskVurdering.annenFraversArsak
                    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose
                }
            ) {
                it.annenFraversArsak == null && it.hoveddiagnose == null
            },

            // §8-4 Sykmeldingen må angi sykdom eller skade eller annen gyldig fraværsgrunn som angitt i loven.
            // Diagnose må være satt i henhold til angitt kodeverk.
            // Hvis kodeverk ikke er angitt eller korrekt for hoveddiagnose, avvises meldingen.
            RuleThingy(
                name = "UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE",
                ruleId = 1540,
                status = Status.INVALID,
                messageForUser = "Den må ha riktig kode for hoveddiagnose.",
                messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Kodeverk for hoveddiagnose er feil eller mangler.",
                JuridiskHenvisning(
                    lovverk = Lovverk.FOLKETRYGDLOVEN,
                    paragraf = "8-4",
                    ledd = 1,
                    punktum = 1,
                    bokstav = null
                ),
                object {
                    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose
                    val annenFravarsArsak = sykmelding.medisinskVurdering.annenFraversArsak
                }
            ) {
                (it.hoveddiagnose?.system !in arrayOf(Diagnosekoder.ICPC2_CODE, Diagnosekoder.ICD10_CODE) ||
                        it.hoveddiagnose?.let { diagnose ->
                            if (diagnose.isICPC2()) {
                                Diagnosekoder.icpc2.containsKey(diagnose.kode)
                            } else {
                                Diagnosekoder.icd10.containsKey(diagnose.kode)
                            }
                        } != true
                        ) && it.annenFravarsArsak == null
            },


            // §8-4 Sykmeldingen må angi sykdom eller skade eller annen gyldig fraværsgrunn som angitt i loven.
            // Diagnose må være satt i henhold til angitt kodeverk.
            // Hvis kodeverk ikke er angitt eller korrekt for bidiagnose, avvises meldingen.
            RuleThingy(
                name = "UGYLDIG_KODEVERK_FOR_BIDIAGNOSE",
                ruleId = 1541,
                status = Status.INVALID,
                messageForUser = "Det er brukt eit ukjent kodeverk for bidiagnosen.",
                messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Kodeverk for bidiagnose er ikke angitt eller korrekt",
                JuridiskHenvisning(
                    lovverk = Lovverk.FOLKETRYGDLOVEN,
                    paragraf = "8-4",
                    ledd = 1,
                    punktum = 1,
                    bokstav = null
                ),
                object {
                    val biDiagnoser = sykmelding.medisinskVurdering.biDiagnoser
                }
            ) {
                !it.biDiagnoser.all { diagnose ->
                    if (diagnose.isICPC2()) {
                        Diagnosekoder.icpc2.containsKey(diagnose.kode)
                    } else {
                        Diagnosekoder.icd10.containsKey(diagnose.kode)
                    }
                }
            },

            // Sjekker om korrekt versjon av sykmeldingen er benyttet
            // Hvis regelsettversjon som er angitt i fagmelding ikke eksisterer så skal meldingen returneres
            RuleThingy(
                name = "UGYLDIG_REGELSETTVERSJON",
                ruleId = 1708,
                status = Status.INVALID,
                messageForUser = "Det er brukt en versjon av sykmeldingen som ikke lenger er gyldig.",
                messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Feil regelsett er brukt i sykmeldingen.",
                null,
                object {
                    val rulesetVersion = metadata.rulesetVersion
                }
            ) {
                it.rulesetVersion !in arrayOf(null, "", "1", "2")
            },

            // Krav om utdypende opplysninger ved uke 39
            // Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 39 uker etter innføring av regelsettversjon \"2\" så skal sykmeldingen avvises
            RuleThingy(
                name = "MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39",
                ruleId = 1709,
                status = Status.INVALID,
                messageForUser = "Sykmeldingen mangler utdypende opplysninger som kreves når sykefraværet er lengre enn 39 uker til sammen.",
                messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Utdypende opplysninger som kreves ved uke 39 mangler. ",
                null,
                object {
                    val rulesetVersion = metadata.rulesetVersion
                    val sykmeldingPerioder = sykmelding.perioder
                    val utdypendeOpplysinger = sykmelding.utdypendeOpplysninger
                }
            ) { input ->
                input.rulesetVersion in arrayOf("2") &&
                        input.sykmeldingPerioder.any { (it.fom..it.tom).daysBetween() > 273 } &&
                        input.utdypendeOpplysinger.containsAnswersFor(QuestionGroup.GROUP_6_5) != true
            },

            // Orgnr må være korrekt angitt
            // Organisasjonsnummeret som er oppgitt er ikke 9 tegn.
            RuleThingy(
                name = "UGYLDIG_ORGNR_LENGDE",
                ruleId = 9999,
                status = Status.INVALID,
                messageForUser = "Den må ha riktig organisasjonsnummer.",
                messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Feil format på organisasjonsnummer. Dette skal være 9 sifre.",
                null,
                object {
                    val legekontorOrgnr = metadata.legekontorOrgnr
                }
            ) {
                it.legekontorOrgnr != null && it.legekontorOrgnr.length != 9
            },

            // Forvaltningsloven §6 1. ledd bokstav a
            // Den som signerer sykmeldingen kan ikke sykmelde seg selv
            // Avsender fnr er det samme som pasient fnr
            RuleThingy(
                name = "AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR",
                ruleId = 9999,
                status = Status.INVALID,
                messageForUser = "Den som signert sykmeldingen er også pasient.",
                messageForSender = "Sykmeldingen kan ikke rettes, Pasienten har fått beskjed, den ble avvist grunnet følgende:" +
                        "Avsender fnr er det samme som pasient fnr",
                JuridiskHenvisning(
                    lovverk = Lovverk.FORVALTNINGSLOVEN,
                    paragraf = "6",
                    ledd = 1,
                    punktum = 1,
                    bokstav = "a"
                ),
                object {
                    val avsenderFnr = metadata.avsenderFnr
                    val patientPersonNumber = metadata.patientPersonNumber
                }
            ) {
                it.avsenderFnr == it.patientPersonNumber
            },

            // Forvaltningsloven §6 1. ledd bokstav a
            // Behandler kan ikke sykmelde seg selv
            // Behandler fnr er det samme som pasient fnr
            RuleThingy(
                name = "BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR",
                ruleId = 9999,
                status = Status.INVALID,
                messageForUser = "Den som er behandler av sykmeldingen er også pasient.",
                messageForSender = "Sykmeldingen kan ikke rettes. Pasienten har fått beskjed, den ble avvist grunnet følgende:" +
                        "Behandler fnr er det samme som pasient fnr",
                juridiskHenvisning = JuridiskHenvisning(
                    lovverk = Lovverk.FORVALTNINGSLOVEN,
                    paragraf = "6",
                    ledd = 1,
                    punktum = 1,
                    bokstav = "a"
                ),
                object {
                    val behandlerFnr = sykmelding.behandler.fnr
                    val pasientFodselsNummer = metadata.patientPersonNumber
                },
            ) { it.behandlerFnr == it.pasientFodselsNummer },
        )
    }
}

/*
    enum class ValidationRuleChainOld(
        override val ruleId: Int?,
        override val status: Status,
        override val messageForUser: String,
        override val messageForSender: String,
        override val juridiskHenvisning: JuridiskHenvisning?,
        override val predicate: (RuleData<RuleMetadata>) -> Boolean
    ) : Rule<RuleData<RuleMetadata>> {

        // Opptjening før 13 år er ikke mulig.
        @Description("Hele sykmeldingsperioden er før bruker har fylt 13 år. Pensjonsopptjening kan starte fra 13 år.")
        PASIENT_YNGRE_ENN_13(
            1101,
            Status.INVALID,
            "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
            "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
            null,
            { (sykmelding, metadata) ->
                sykmelding.perioder.sortedTOMDate().last() < metadata.pasientFodselsdato.plusYears(13)
            }
        ),

        // §8-3 Det ytes ikke sykepenger til medlem som er fylt 70 år.
        @Description("Hele sykmeldingsperioden er etter at bruker har fylt 70 år. Dersom bruker fyller 70 år i perioden skal sykmelding gå gjennom på vanlig måte.")
        PASIENT_ELDRE_ENN_70(
            1102,
            Status.INVALID,
            "Sykmelding kan ikke benyttes etter at du har fylt 70 år",
            "Pasienten er over 70 år. Sykmelding kan ikke benyttes. Pasienten har fått beskjed.",
            JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-3",
                ledd = 1,
                punktum = 2,
                bokstav = null
            ),
            { (sykmelding, metadata) ->
                sykmelding.perioder.sortedFOMDate().first() > metadata.pasientFodselsdato.plusYears(70)
            }
        ),

        // §8-4 Sykmeldingen må angi sykdom eller skade eller annen gyldig fraværsgrunn som angitt i loven.
        // Kodeverk må være satt i henhold til gyldige kodeverk som angitt av Helsedirektoratet (ICPC-2 og ICD-10).
        @Description("Ukjent houved diagnosekode type")
        UKJENT_DIAGNOSEKODETYPE(
            1137,
            Status.INVALID,
            "Sykmeldingen må ha et kjent kodeverk for diagnosen.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Ukjent kodeverk er benyttet for diagnosen.",
            JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-4",
                ledd = 1,
                punktum = 1,
                bokstav = null
            ),
            { (sykmelding, _) ->
                sykmelding.medisinskVurdering.hovedDiagnose != null &&
                        sykmelding.medisinskVurdering.hovedDiagnose?.system !in Diagnosekoder
            }
        ),

        // §8-4 Arbeidsuførhet som skyldes sosiale eller økomoniske problemer o.l. gir ikke rett til sykepenger.
        @Description("Hvis hoveddiagnose er Z-diagnose (ICPC-2), avvises meldingen.")
        ICPC_2_Z_DIAGNOSE(
            1132,
            Status.INVALID,
            "Den må ha en gyldig diagnosekode som gir rett til sykepenger.",
            "Angitt hoveddiagnose (z-diagnose) gir ikke rett til sykepenger. Pasienten har fått beskjed.",
            JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-4",
                ledd = 1,
                punktum = 2,
                bokstav = null
            ),
            { (sykmelding, _) ->
                sykmelding.medisinskVurdering.hovedDiagnose != null &&
                        sykmelding.medisinskVurdering.hovedDiagnose!!.isICPC2() && sykmelding.medisinskVurdering.hovedDiagnose!!.kode?.startsWith(
                    "Z"
                )
            }
        ),

        // §8-4 Sykmeldingen må angi sykdom eller skade eller annen gyldig fraværsgrunn som angitt i loven.
        @Description("Hvis hoveddiagnose mangler og det ikke er angitt annen lovfestet fraværsgrunn, avvises meldingen")
        HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER(
            1133,
            Status.INVALID,
            "Den må ha en hoveddiagnose eller en annen gyldig fraværsgrunn.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Hoveddiagnose eller annen lovfestet fraværsgrunn mangler. ",
            JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-4",
                ledd = 1,
                punktum = 1,
                bokstav = null
            ),
            { (sykmelding, _) ->
                sykmelding.medisinskVurdering.annenFraversArsak == null &&
                        sykmelding.medisinskVurdering.hovedDiagnose == null
            }
        ),

        // §8-4 Sykmeldingen må angi sykdom eller skade eller annen gyldig fraværsgrunn som angitt i loven.
        // Diagnose må være satt i henhold til angitt kodeverk.
        @Description("Hvis kodeverk ikke er angitt eller korrekt for hoveddiagnose, avvises meldingen.")
        UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE(
            1540,
            Status.INVALID,
            "Den må ha riktig kode for hoveddiagnose.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Kodeverk for hoveddiagnose er feil eller mangler.",
            JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-4",
                ledd = 1,
                punktum = 1,
                bokstav = null
            ),
            { (sykmelding, _) ->
                (
                        sykmelding.medisinskVurdering.hovedDiagnose?.system !in arrayOf(
                            Diagnosekoder.ICPC2_CODE,
                            Diagnosekoder.ICD10_CODE
                        ) ||
                                sykmelding.medisinskVurdering.hovedDiagnose?.let { diagnose ->
                                    if (diagnose.isICPC2()) {
                                        Diagnosekoder.icpc2.containsKey(diagnose.kode)
                                    } else {
                                        Diagnosekoder.icd10.containsKey(diagnose.kode)
                                    }
                                } != true
                        ) && sykmelding.medisinskVurdering.annenFraversArsak == null
            }
        ),

        // §8-4 Sykmeldingen må angi sykdom eller skade eller annen gyldig fraværsgrunn som angitt i loven.
        // Diagnose må være satt i henhold til angitt kodeverk.
        @Description("Hvis kodeverk ikke er angitt eller korrekt for bidiagnose, avvises meldingen.")
        UGYLDIG_KODEVERK_FOR_BIDIAGNOSE(
            1541,
            Status.INVALID, "Det er brukt eit ukjent kodeverk for bidiagnosen.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Kodeverk for bidiagnose er ikke angitt eller korrekt",
            JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-4",
                ledd = 1,
                punktum = 1,
                bokstav = null
            ),
            { (sykmelding, _) ->
                !sykmelding.medisinskVurdering.biDiagnoser.all { diagnose ->
                    if (diagnose.isICPC2()) {
                        Diagnosekoder.icpc2.containsKey(diagnose.kode)
                    } else {
                        Diagnosekoder.icd10.containsKey(diagnose.kode)
                    }
                }
            }
        ),

        // Sjekker om korrekt versjon av sykmeldingen er benyttet
        @Description("Hvis regelsettversjon som er angitt i fagmelding ikke eksisterer så skal meldingen returneres")
        UGYLDIG_REGELSETTVERSJON(
            1708,
            Status.INVALID,
            "Det er brukt en versjon av sykmeldingen som ikke lenger er gyldig.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Feil regelsett er brukt i sykmeldingen.",
            null,
            { (_, ruleMetadata) ->
                ruleMetadata.rulesetVersion !in arrayOf(null, "", "1", "2")
            }
        ),

        // Krav om utdypende opplysninger ved uke 39
        @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 39 uker etter innføring av regelsettversjon \"2\" så skal sykmeldingen avvises")
        MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39(
            1709,
            Status.INVALID,
            "Sykmeldingen mangler utdypende opplysninger som kreves når sykefraværet er lengre enn 39 uker til sammen.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Utdypende opplysninger som kreves ved uke 39 mangler. ",
            null,
            { (sykmelding, ruleMetadata) ->
                ruleMetadata.rulesetVersion in arrayOf("2") &&
                        sykmelding.perioder.any { (it.fom..it.tom).daysBetween() > 273 } &&
                        sykmelding.utdypendeOpplysninger.containsAnswersFor(QuestionGroup.GROUP_6_5) != true
            }
        ),

        // Orgnr må være korrekt angitt
        @Description("Organisasjonsnummeret som er oppgitt er ikke 9 tegn.")
        UGYLDIG_ORGNR_LENGDE(
            9999,
            Status.INVALID,
            "Den må ha riktig organisasjonsnummer.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Feil format på organisasjonsnummer. Dette skal være 9 sifre.",
            null,
            { (_, metadata) ->
                metadata.legekontorOrgnr != null && metadata.legekontorOrgnr.length != 9
            }
        ),

        // Forvaltningsloven §6 1. ledd bokstav a
        // Den som signerer sykmeldingen kan ikke sykmelde seg selv
        @Description("Avsender fnr er det samme som pasient fnr")
        AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(
            9999,
            Status.INVALID,
            "Den som signert sykmeldingen er også pasient.",
            "Sykmeldingen kan ikke rettes, Pasienten har fått beskjed, den ble avvist grunnet følgende:" +
                    "Avsender fnr er det samme som pasient fnr",
            JuridiskHenvisning(
                lovverk = Lovverk.FORVALTNINGSLOVEN,
                paragraf = "6",
                ledd = 1,
                punktum = 1,
                bokstav = "a"
            ),
            { (_, metadata) ->
                metadata.avsenderFnr == metadata.patientPersonNumber
            }
        ),

        // Forvaltningsloven §6 1. ledd bokstav a
        // Behandler kan ikke sykmelde seg selv
        @Description("Behandler fnr er det samme som pasient fnr")
        BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR(
            9999,
            Status.INVALID,
            "Den som er behandler av sykmeldingen er også pasient.",
            "Sykmeldingen kan ikke rettes. Pasienten har fått beskjed, den ble avvist grunnet følgende:" +
                    "Behandler fnr er det samme som pasient fnr",
            JuridiskHenvisning(
                lovverk = Lovverk.FORVALTNINGSLOVEN,
                paragraf = "6",
                ledd = 1,
                punktum = 1,
                bokstav = "a"
            ),
            { (sykmelding, metadata) ->
                sykmelding.behandler.fnr == metadata.patientPersonNumber
            }
        ),
    }
*/

fun Map<String, Map<String, SporsmalSvar>>.containsAnswersFor(questionGroup: QuestionGroup) =
    this[questionGroup.spmGruppeId]?.all { (spmId, _) ->
        spmId in questionGroup.spmsvar.map { it.spmId }
    }
