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
    UGYLDIG_FNR_LENGDE(
            1002,
            Status.INVALID,
            "Pasienten sitt fødselsnummer eller D-nummer er ikke 11 tegn.",
            "Pasienten sitt fødselsnummer eller D-nummer er ikke 11 tegn.", { (_, metadata) ->
        !validatePersonAndDNumber11Digits(metadata.patientPersonNumber)
    }),

    @Description("Fødselsnummer/D-nummer kan passerer ikke modulus 11")
    UGYLDIG_FNR(
            1006,
            Status.INVALID,
            "Fødselsnummer/D-nummer kan passerer ikke modulus 11",
            "Fødselsnummer/D-nummer kan passerer ikke modulus 11", { (_, metadata) ->
        !validatePersonAndDNumber(metadata.patientPersonNumber)
    }),

    @Description("Hele sykmeldingsperioden er før bruker har fylt 13 år. Pensjonsopptjening kan starte fra 13 år.")
    PASIENT_YNGRE_ENN_13(
            1101,
            Status.INVALID,
            "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
            "Pasienten er under 13 år. Sykmelding kan ikke benyttes.", { (healthInformation, metadata) ->
        healthInformation.perioder.sortedTOMDate().last() < extractBornDate(metadata.patientPersonNumber).plusYears(13)
    }),

    @Description("Hele sykmeldingsperioden er etter at bruker har fylt 70 år. Dersom bruker fyller 70 år i perioden skal sykmelding gå gjennom på vanlig måte.")
    PASIENT_ELDRE_ENN_70(
            1102,
            Status.INVALID,
            "Sykmelding kan ikke benyttes etter at du har fylt 70 år",
            "Pasienten er over 70 år. Sykmelding kan ikke benyttes.", { (healthInformation, metadata) ->
        healthInformation.perioder.sortedFOMDate().first() > extractBornDate(metadata.patientPersonNumber).plusYears(70)
    }),

    @Description("Ukjent houved diagnosekode type")
    UKJENT_DIAGNOSEKODETYPE(
            1137,
            Status.INVALID,
            "Den må ha en kjent diagnosekode.",
            "Ukjent diagnosekode er benyttet. ", { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose != null &&
            healthInformation.medisinskVurdering.hovedDiagnose?.system !in Diagnosekoder
    }),

    @Description("Hvis hoveddiagnose er Z-diagnose (ICPC-2), avvises meldingen.")
    ICPC_2_Z_DIAGNOSE(
            1132,
            Status.INVALID,
            "Den må ha en gyldig diagnosekode som gir rett til sykepenger.",
            "Angitt hoveddiagnose (z-diagnose) gir ikke rett til sykepenger.", { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose?.toICPC2()?.firstOrNull()?.code?.startsWith("Z") == true
    }),

    @Description("Hvis hoveddiagnose mangler og det ikke er angitt annen lovfestet fraværsgrunn, avvises meldingen")
    HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER(
            1133,
            Status.INVALID,
            "Den må ha en hoveddiagnose eller en annen gyldig fraværsgrunn.",
            "Hoveddiagnose eller annen lovfestet fraværsgrunn mangler. ",
            { (healthInformation, _) ->
        healthInformation.medisinskVurdering.annenFraversArsak == null &&
                healthInformation.medisinskVurdering.hovedDiagnose == null
    }),

    @Description("Hvis kodeverk ikke er angitt eller korrekt for hoveddiagnose, avvises meldingen.")
    UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE(
            1540,
            Status.INVALID,
            "Den må ha riktig kode for hoveddiagnose.",
            "Kodeverk for hoveddiagnose er feil eller mangler.", { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose?.system !in arrayOf(Diagnosekoder.ICPC2_CODE, Diagnosekoder.ICD10_CODE) ||
                healthInformation.medisinskVurdering.hovedDiagnose?.let { diagnose ->
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
            "Hvis kodeverk ikke er angitt eller korrekt for bidiagnose, avvises meldingen.", { (healthInformation, _) ->
        !healthInformation.medisinskVurdering.biDiagnoser.all { diagnose ->
            if (diagnose.isICPC2()) {
                Diagnosekoder.icpc2.containsKey(diagnose.kode)
            } else {
                Diagnosekoder.icd10.containsKey(diagnose.kode)
            }
        }
    }),

    // TODO: Vi trenger å kunne finne syketilfelle start dato for å gjøre disse beregningene. Fom i beregningen skal være syketilfelle start dato.
//    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 8.17, 39 uker før regelsettversjon \"2\" er innført skal sykmeldingen avvises")
//    MANGLENDE_PAKREVDE_DYNAMISKE_SPORSMAL(
//            1707,
//            Status.INVALID,
//            "Den må inneholde utdypende opplysninger når du har vært sykmeldt lenge",
//            "Utdypende opplysninger mangler. ", { (healthInformation, ruleMetadata) ->
//        ruleMetadata.rulesetVersion in arrayOf(null, "", "1") &&
//                healthInformation.perioder.any { (it.fom..it.tom).daysBetween() > 56 } &&
//                healthInformation.utdypendeOpplysninger.containsAnswersFor(QuestionGroup.GROUP_6_2) != true
//    }),
//
    @Description("Hvis regelsettversjon som er angitt i fagmelding ikke eksisterer så skal meldingen returneres")
    UGYLDIG_REGELSETTVERSJON(
            1708,
            Status.INVALID,
            "Det er brukt en versjon av sykmeldingen som ikke lenger er gyldig.",
            "Feil regelsett er brukt i sykmeldingen.", { (_, ruleMetadata) ->
        ruleMetadata.rulesetVersion !in arrayOf(null, "", "1", "2")
    }),
//
//    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 7 uker etter innføring av regelsettversjon \"2\" så skal sykmeldingen avvises")
//    MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_7(
//            1709,
//            Status.INVALID,
//            "Sykmeldingen mangler utdypende opplysninger som kreves når sykefraværet er lengre enn 7 uker til sammen.",
//            "Utdypende opplysninger som kreves ved uke 7 mangler. ", { (healthInformation, ruleMetadata) ->
//        ruleMetadata.rulesetVersion in arrayOf("2") &&
//                healthInformation.perioder.any { (it.fom..it.tom).daysBetween() > 49 } &&
//                healthInformation.utdypendeOpplysninger.containsAnswersFor(QuestionGroup.GROUP_6_3) != true
//    }),
//
//    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 17 uker etter innføring av regelsettversjon \"2\" så skal sykmeldingen avvises")
//    MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_17(
//            1709,
//            Status.INVALID,
//            "Sykmeldingen mangler utdypende opplysninger som kreves når sykefraværet er lengre enn 17 uker til sammen.",
//            "Utdypende opplysninger som kreves ved uke 17 mangler.", { (healthInformation, ruleMetadata) ->
//        ruleMetadata.rulesetVersion in arrayOf("2") &&
//                healthInformation.perioder.any { (it.fom..it.tom).daysBetween() > 119 } &&
//                healthInformation.utdypendeOpplysninger.containsAnswersFor(QuestionGroup.GROUP_6_4) != true
//    }),

    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 39 uker etter innføring av regelsettversjon \"2\" så skal sykmeldingen avvises")
    MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39(
            1709,
            Status.INVALID,
            "Sykmeldingen mangler utdypende opplysninger som kreves når sykefraværet er lengre enn 39 uker til sammen.",
            "Utdypende opplysninger som kreves ved uke 39 mangler. ", { (healthInformation, ruleMetadata) ->
        ruleMetadata.rulesetVersion in arrayOf("2") &&
                healthInformation.perioder.any { (it.fom..it.tom).daysBetween() > 273 } &&
                healthInformation.utdypendeOpplysninger.containsAnswersFor(QuestionGroup.GROUP_6_5) != true
    }),

    @Description("Organisasjonsnummeret som er oppgitt er ikke 9 tegn.")
    UGYLDIG_ORGNR_LENGDE(
            9999,
            Status.INVALID,
            "Den må ha riktig organisasjonsnummer.",
            "Feil format på organisasjonsnummer. Dette skal være 9 sifre..", { (_, metadata) ->
        metadata.legekontorOrgnr != null && metadata.legekontorOrgnr.length != 9
    }),
}

fun Map<String, Map<String, SporsmalSvar>>.containsAnswersFor(questionGroup: QuestionGroup) =
        this[questionGroup.spmGruppeId]?.all { (spmId, _) ->
            spmId in questionGroup.spmsvar.map { it.spmId }
        }
