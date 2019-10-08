package no.nav.syfo.rules

import no.nav.syfo.QuestionGroup
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.SporsmalSvar
import no.nav.syfo.model.Status
import no.nav.syfo.sm.Diagnosekoder
import no.nav.syfo.sm.isICPC2
import no.nav.syfo.sm.toICPC2
import no.nav.syfo.validation.extractBornDate
import no.nav.syfo.validation.validatePersonAndDNumber
import no.nav.syfo.validation.validatePersonAndDNumber11Digits

enum class ValidationRuleChain(
    override val ruleId: Int?,
    override val status: Status,
    override val messageForUser: String,
    override val messageForSender: String,
    override val predicate: (RuleData<RuleMetadata>) -> Boolean
) : Rule<RuleData<RuleMetadata>> {

    @Description("Pasienten sitt fødselsnummer eller D-nummer er ikke 11 tegn.")
    UGYLDIG_FNR_LENGDE_PASIENT(
            1002,
            Status.INVALID,
            "Fødselsnummer eller D-nummer den sykmeldt er ikke 11 tegn.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Pasienten sitt fødselsnummer eller D-nummer er ikke 11 tegn.", { (_, metadata) ->
        !validatePersonAndDNumber11Digits(metadata.patientPersonNumber)
    }),

    @Description("Behandler sitt fødselsnummer eller D-nummer er ikke 11 tegn.")
    UGYLDIG_FNR_LENGDE_BEHANDLER(
            1002,
            Status.INVALID,
            "Fødselsnummer for den som sykmeldte deg, er ikke 11 tegn.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Behandler sitt fødselsnummer eller D-nummer er ikke 11 tegn.", { (sykmelding, _) ->
        !validatePersonAndDNumber11Digits(sykmelding.behandler.fnr)
    }),

    @Description("Pasientens fødselsnummer/D-nummer kan passerer ikke modulus 11")
    UGYLDIG_FNR_PASIENT(
            1006,
            Status.INVALID,
            "Fødselsnummer for den sykmeldte er ikke gyldig",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Pasientens fødselsnummer/D-nummer er ikke gyldig", { (_, metadata) ->
        !validatePersonAndDNumber(metadata.patientPersonNumber)
    }),

    @Description("Behandlers fødselsnummer/D-nummer kan passerer ikke modulus 11")
    UGYLDIG_FNR_BEHANDLER(
            1006,
            Status.INVALID,
            "Fødselsnummer for den sykmeldte deg, er ikke gyldig",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Behandlers fødselsnummer/D-nummer kan passerer ikke modulus 11", { (sykmelding, _) ->
        !validatePersonAndDNumber(sykmelding.behandler.fnr)
    }),

    @Description("Hele sykmeldingsperioden er før bruker har fylt 13 år. Pensjonsopptjening kan starte fra 13 år.")
    PASIENT_YNGRE_ENN_13(
            1101,
            Status.INVALID,
            "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Pasienten er under 13 år. Sykmelding kan ikke benyttes.", { (sykmelding, metadata) ->
        sykmelding.perioder.sortedTOMDate().last() < extractBornDate(metadata.patientPersonNumber).plusYears(13)
    }),

    @Description("Hele sykmeldingsperioden er etter at bruker har fylt 70 år. Dersom bruker fyller 70 år i perioden skal sykmelding gå gjennom på vanlig måte.")
    PASIENT_ELDRE_ENN_70(
            1102,
            Status.INVALID,
            "Sykmelding kan ikke benyttes etter at du har fylt 70 år",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Pasienten er over 70 år. Sykmelding kan ikke benyttes.", { (sykmelding, metadata) ->
        sykmelding.perioder.sortedFOMDate().first() > extractBornDate(metadata.patientPersonNumber).plusYears(70)
    }),

    @Description("Ukjent houved diagnosekode type")
    UKJENT_DIAGNOSEKODETYPE(
            1137,
            Status.INVALID,
            "Den må ha en kjent diagnosekode.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Ukjent diagnosekode er benyttet.", { (sykmelding, _) ->
        sykmelding.medisinskVurdering.hovedDiagnose != null &&
                sykmelding.medisinskVurdering.hovedDiagnose?.system !in Diagnosekoder
    }),

    @Description("Hvis hoveddiagnose er Z-diagnose (ICPC-2), avvises meldingen.")
    ICPC_2_Z_DIAGNOSE(
            1132,
            Status.INVALID,
            "Den må ha en gyldig diagnosekode som gir rett til sykepenger.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Angitt hoveddiagnose (z-diagnose) gir ikke rett til sykepenger.", { (sykmelding, _) ->
        sykmelding.medisinskVurdering.hovedDiagnose?.toICPC2()?.firstOrNull()?.code?.startsWith("Z") == true
    }),

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
    }),

    @Description("Hvis kodeverk ikke er angitt eller korrekt for hoveddiagnose, avvises meldingen.")
    UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE(
            1540,
            Status.INVALID,
            "Den må ha riktig kode for hoveddiagnose.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Kodeverk for hoveddiagnose er feil eller mangler.", { (sykmelding, _) ->
        sykmelding.medisinskVurdering.hovedDiagnose?.system !in arrayOf(Diagnosekoder.ICPC2_CODE, Diagnosekoder.ICD10_CODE) ||
                sykmelding.medisinskVurdering.hovedDiagnose?.let { diagnose ->
            if (diagnose.isICPC2()) {
                Diagnosekoder.icpc2.containsKey(diagnose.kode)
            } else {
                Diagnosekoder.icd10.containsKey(diagnose.kode)
            }
        } != true
    }),

    // Revurder regel når IT ikkje lenger skal brukes
    // Her mener jeg fremdeles at vi skal nulle ut bidiagnosen dersom den er feil - ikke avvise sykmeldingen!!
    @Description("Hvis kodeverk ikke er angitt eller korrekt for bidiagnose, avvises meldingen.")
    UGYLDIG_KODEVERK_FOR_BIDIAGNOSE(
            1541,
            Status.MANUAL_PROCESSING, "Det er feil i koden for bidiagnosen.",
            "Kodeverk ikke er angitt eller korrekt for bidiagnose, avvises meldingen.", { (sykmelding, _) ->
        !sykmelding.medisinskVurdering.biDiagnoser.all { diagnose ->
            if (diagnose.isICPC2()) {
                Diagnosekoder.icpc2.containsKey(diagnose.kode)
            } else {
                Diagnosekoder.icd10.containsKey(diagnose.kode)
            }
        }
    }),

    @Description("Hvis regelsettversjon som er angitt i fagmelding ikke eksisterer så skal meldingen returneres")
    UGYLDIG_REGELSETTVERSJON(
            1708,
            Status.INVALID,
            "Det er brukt en versjon av sykmeldingen som ikke lenger er gyldig.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Feil regelsett er brukt i sykmeldingen.", { (_, ruleMetadata) ->
        ruleMetadata.rulesetVersion !in arrayOf(null, "", "1", "2")
    }),

    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 39 uker etter innføring av regelsettversjon \"2\" så skal sykmeldingen avvises")
    MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39(
            1709,
            Status.INVALID,
            "Sykmeldingen mangler utdypende opplysninger som kreves når sykefraværet er lengre enn 39 uker til sammen.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Utdypende opplysninger som kreves ved uke 39 mangler. ", { (sykmelding, ruleMetadata) ->
        ruleMetadata.rulesetVersion in arrayOf("2") &&
                sykmelding.perioder.any { (it.fom..it.tom).daysBetween() > 273 } &&
                sykmelding.utdypendeOpplysninger.containsAnswersFor(QuestionGroup.GROUP_6_5) != true
    }),

    @Description("Organisasjonsnummeret som er oppgitt er ikke 9 tegn.")
    UGYLDIG_ORGNR_LENGDE(
            9999,
            Status.INVALID,
            "Den må ha riktig organisasjonsnummer.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Feil format på organisasjonsnummer. Dette skal være 9 sifre..", { (_, metadata) ->
        metadata.legekontorOrgnr != null && metadata.legekontorOrgnr.length != 9
    }),
}

fun Map<String, Map<String, SporsmalSvar>>.containsAnswersFor(questionGroup: QuestionGroup) =
        this[questionGroup.spmGruppeId]?.all { (spmId, _) ->
            spmId in questionGroup.spmsvar.map { it.spmId }
        }
