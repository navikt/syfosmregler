package no.nav.syfo.rules

import no.nav.syfo.QuestionGroup
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.SporsmalSvar
import no.nav.syfo.model.Status
import no.nav.syfo.sm.Diagnosekoder
import no.nav.syfo.sm.isICPC2
import no.nav.syfo.validation.extractBornDate

enum class ValidationRuleChain(
    override val ruleId: Int?,
    override val status: Status,
    override val messageForUser: String,
    override val messageForSender: String,
    override val predicate: (RuleData<RuleMetadata>) -> Boolean
) : Rule<RuleData<RuleMetadata>> {

    // Opptjening før 13 år er ikke mulig.
    @Description("Hele sykmeldingsperioden er før bruker har fylt 13 år. Pensjonsopptjening kan starte fra 13 år.")
    PASIENT_YNGRE_ENN_13(
        1101,
        Status.INVALID,
        "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
        "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
        { (sykmelding, metadata) ->
            sykmelding.perioder.sortedTOMDate().last() < extractBornDate(metadata.patientPersonNumber).plusYears(13)
        }
    ),

    // §8-3 Det ytes ikke sykepenger til medlem som er fylt 70 år.
    @Description("Hele sykmeldingsperioden er etter at bruker har fylt 70 år. Dersom bruker fyller 70 år i perioden skal sykmelding gå gjennom på vanlig måte.")
    PASIENT_ELDRE_ENN_70(
        1102,
        Status.INVALID,
        "Sykmelding kan ikke benyttes etter at du har fylt 70 år",
        "Pasienten er over 70 år. Sykmelding kan ikke benyttes. Pasienten har fått beskjed.",
        { (sykmelding, metadata) ->
            sykmelding.perioder.sortedFOMDate().first() > extractBornDate(metadata.patientPersonNumber).plusYears(70)
        }
    ),

    // Kodeverk må være satt i henhold til gyldige kodeverk som angitt av Helsedirektoratet (ICPC-2 og ICD-10).
    @Description("Ukjent houved diagnosekode type")
    UKJENT_DIAGNOSEKODETYPE(
        1137,
        Status.INVALID,
        "Den må ha en kjent diagnosekode.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Ukjent diagnosekode er benyttet.",
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
        { (sykmelding, _) ->
            sykmelding.medisinskVurdering.hovedDiagnose != null &&
                sykmelding.medisinskVurdering.hovedDiagnose!!.isICPC2() && sykmelding.medisinskVurdering.hovedDiagnose!!.kode?.startsWith("Z")
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
        { (sykmelding, _) ->
            sykmelding.medisinskVurdering.annenFraversArsak == null &&
                sykmelding.medisinskVurdering.hovedDiagnose == null
        }
    ),

    // Diagnose må være satt i henhold til angitt kodeverk.
    @Description("Hvis kodeverk ikke er angitt eller korrekt for hoveddiagnose, avvises meldingen.")
    UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE(
        1540,
        Status.INVALID,
        "Den må ha riktig kode for hoveddiagnose.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Kodeverk for hoveddiagnose er feil eller mangler.",
        { (sykmelding, _) ->
            (
                sykmelding.medisinskVurdering.hovedDiagnose?.system !in arrayOf(Diagnosekoder.ICPC2_CODE, Diagnosekoder.ICD10_CODE) ||
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

    // Diagnose må være satt i henhold til angitt kodeverk.
    @Description("Hvis kodeverk ikke er angitt eller korrekt for bidiagnose, avvises meldingen.")
    UGYLDIG_KODEVERK_FOR_BIDIAGNOSE(
        1541,
        Status.INVALID, "Det er brukt eit ukjent kodeverk for bidiagnosen.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Kodeverk for bidiagnose er ikke angitt eller korrekt",
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
        { (_, metadata) ->
            metadata.legekontorOrgnr != null && metadata.legekontorOrgnr.length != 9
        }
    ),

    // Den som signerer sykmeldingen kan ikke sykmelde seg selv
    @Description("Avsender fnr er det samme som pasient fnr")
    AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(
        9999,
        Status.INVALID,
        "Den som signert sykmeldingen er også pasient.",
        "Sykmeldingen kan ikke rettes, Pasienten har fått beskjed, den ble avvist grunnet følgende:" +
            "Avsender fnr er det samme som pasient fnr",
        { (_, metadata) ->
            metadata.avsenderFnr == metadata.patientPersonNumber
        }
    ),

    // Behandler kan ikke sykmelde seg selv
    @Description("Behandler fnr er det samme som pasient fnr")
    BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR(
        9999,
        Status.INVALID,
        "Den som er behandler av sykmeldingen er også pasient.",
        "Sykmeldingen kan ikke rettes. Pasienten har fått beskjed, den ble avvist grunnet følgende:" +
            "Behandler fnr er det samme som pasient fnr",
        { (sykmelding, metadata) ->
            sykmelding.behandler.fnr == metadata.patientPersonNumber
        }
    ),
}

fun Map<String, Map<String, SporsmalSvar>>.containsAnswersFor(questionGroup: QuestionGroup) =
    this[questionGroup.spmGruppeId]?.all { (spmId, _) ->
        spmId in questionGroup.spmsvar.map { it.spmId }
    }
