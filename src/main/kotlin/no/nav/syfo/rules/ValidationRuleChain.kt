package no.nav.syfo.rules

import no.nav.syfo.Description
import no.nav.syfo.Diagnosekode
import no.nav.syfo.ICD10
import no.nav.syfo.ICPC2
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.QuestionGroup
import no.nav.syfo.contains
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.SporsmalSvar
import no.nav.syfo.model.Status
import no.nav.syfo.validation.extractBornDate
import no.nav.syfo.validation.validatePersonAndDNumber
import no.nav.syfo.validation.validatePersonAndDNumber11Digits

enum class ValidationRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<RuleMetadata>) -> Boolean) : Rule<RuleData<RuleMetadata>> {
    // TODO: Use this ruleId for when the TPS SOAP call returns that the person is missing
    @Description("Pasienten sitt fødselsnummer eller D-nummer er ikke 11 tegn.")
    INVALID_FNR_SIZE(1002, Status.INVALID, { (_, metadata) ->
        !validatePersonAndDNumber11Digits(metadata.patientPersonNumber)
    }),

    @Description("Fødselsnummer/D-nummer kan passerer ikke modulus 11")
    INVALID_FNR(1006, Status.INVALID, { (_, metadata) ->
        !validatePersonAndDNumber(metadata.patientPersonNumber)
    }),

    @Description("Hele sykmeldingsperioden er før bruker har fylt 13 år. Pensjonsopptjening kan starte fra 13 år.")
    YOUNGER_THAN_13(1101, Status.INVALID, { (healthInformation, metadata) ->
        healthInformation.perioder.sortedTOMDate().last() < extractBornDate(metadata.patientPersonNumber).plusYears(13)
    }),

    @Description("Hele sykmeldingsperioden er etter at bruker har fylt 70 år. Dersom bruker fyller 70 år i perioden skal sykmelding gå gjennom på vanlig måte.")
    PATIENT_OVER_70_YEARS(1102, Status.INVALID, { (healthInformation, metadata) ->
        healthInformation.perioder.sortedFOMDate().first() > extractBornDate(metadata.patientPersonNumber).plusYears(70)
    }),

    @Description("Ukjent diagnosekode type")
    UNKNOWN_DIAGNOSECODE_TYPE(1137, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose != null &&
            healthInformation.medisinskVurdering.hovedDiagnose.system !in Diagnosekode.values()
    }),

    @Description("Hvis hoveddiagnose er Z-diagnose (ICPC-2), avvises meldingen.")
    ICPC_2_Z_DIAGNOSE(1132, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose?.toICPC2()?.firstOrNull()?.codeValue?.startsWith("Z") == true
    }),

    @Description("Hvis hoveddiagnose mangler og det ikke er angitt annen lovfestet fraværsgrunn, avvises meldingen")
    MAIN_DIAGNOSE_MISSING_AND_MISSING_REASON(1133, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.annenFraversArsak == null &&
                healthInformation.medisinskVurdering.hovedDiagnose == null
    }),

    @Description("Hvis kodeverk ikke er angitt eller korrekt for hoveddiagnose, avvises meldingen.")
    INVALID_KODEVERK_FOR_MAIN_DIAGNOSE(1540, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose?.let { cv ->
            if (cv.isICPC2()) {
                ICPC2.values().any { it.codeValue == cv.kode }
            } else {
                ICD10.values().any { it.codeValue == cv.kode }
            }
        } != true
    }),

    // Revurder regel når IT ikkje lenger skal brukes
    @Description("Hvis kodeverk ikke er angitt eller korrekt for bidiagnose, avvises meldingen.")
    INVALID_KODEVERK_FOR_BI_DIAGNOSE(1541, Status.INVALID, { (healthInformation, _) ->
        !healthInformation.medisinskVurdering.bidiagnoser.all { diagnose ->
            if (diagnose.isICPC2()) {
                ICPC2.values().any { it.codeValue == diagnose.kode }
            } else {
                ICD10.values().any { it.codeValue == diagnose.kode }
            }
        }
    }),

    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 8.17, 39 uker før regelsettversjon \"2\" er innført skal sykmeldingen avvises")
    // TODO: Endre navn på denne etter diskusjon med fag og Diskutere med fag mtp hva vi skal gjøre med regelsettversjon
    MISSING_REQUIRED_DYNAMIC_QUESTIONS(1707, Status.INVALID, { (healthInformation, ruleMetadata) ->
        ruleMetadata.rulesetVersion in arrayOf(null, "", "1") &&
                healthInformation.perioder.any { (it.fom..it.tom).daysBetween() > 56 } &&
                healthInformation.utdypendeOpplysninger.containsAnswersFor(QuestionGroup.GROUP_6_2) != true
    }),

    @Description("Hvis regelsettversjon som er angitt i fagmelding ikke eksisterer så skal meldingen returneres")
    INVALID_RULESET_VERSION(1708, Status.INVALID, { (_, ruleMetadata) ->
        ruleMetadata.rulesetVersion !in arrayOf(null, "", "1", "2")
    }),

    // Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 7.17, 39 uker etter innføring av regelsettversjon "2" så skal sykmeldingen avvises
    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 7 uker etter innføring av regelsettversjon \"2\" så skal sykmeldingen avvises")
    MISSING_DYNAMIC_QUESTION_VERSION2_WEEK_7(1709, Status.INVALID, { (healthInformation, ruleMetadata) ->
        ruleMetadata.rulesetVersion in arrayOf("2") &&
                healthInformation.perioder.any { (it.fom..it.tom).daysBetween() > 49 } &&
                healthInformation.utdypendeOpplysninger.containsAnswersFor(QuestionGroup.GROUP_6_3) != true
    }),

    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 17 uker etter innføring av regelsettversjon \"2\" så skal sykmeldingen avvises")
    MISSING_DYNAMIC_QUESTION_VERSION2_WEEK_17(1709, Status.INVALID, { (healthInformation, ruleMetadata) ->
        ruleMetadata.rulesetVersion in arrayOf("2") &&
                healthInformation.perioder.any { (it.fom..it.tom).daysBetween() > 119 } &&
                healthInformation.utdypendeOpplysninger.containsAnswersFor(QuestionGroup.GROUP_6_4) != true
    }),

    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 39 uker etter innføring av regelsettversjon \"2\" så skal sykmeldingen avvises")
    MISSING_DYNAMIC_QUESTION_VERSION2_WEEK_39(1709, Status.INVALID, { (healthInformation, ruleMetadata) ->
        ruleMetadata.rulesetVersion in arrayOf("2") &&
                healthInformation.perioder.any { (it.fom..it.tom).daysBetween() > 273 } &&
                healthInformation.utdypendeOpplysninger.containsAnswersFor(QuestionGroup.GROUP_6_5) != true
    }),
}

fun Map<String, Map<String, SporsmalSvar>>.containsAnswersFor(questionGroup: QuestionGroup) =
        this[questionGroup.spmGruppeId]?.all { (spmId, _) ->
            spmId in questionGroup.spmsvar.map { it.spmId }
        }

// TODO Figure out what to do about group 6.6

fun Diagnose.isICPC2(): Boolean = system == ICPC2.A01.oid

fun Diagnose.toICPC2(): List<ICPC2>? = if (isICPC2()) { listOfNotNull(ICPC2.values().find { it.codeValue == kode }) } else { ICD10.values().find { it.codeValue == kode }?.icpc2 }
