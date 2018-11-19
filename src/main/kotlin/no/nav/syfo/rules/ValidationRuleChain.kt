package no.nav.syfo.rules

import no.nav.syfo.Description
import no.nav.syfo.Diagnosekode
import no.nav.syfo.ICD10
import no.nav.syfo.ICPC2
import no.nav.syfo.Rule
import no.nav.syfo.contains
import no.nav.syfo.model.Status
import no.nav.syfo.validation.extractBornDate
import no.nav.syfo.validation.validatePersonAndDNumber

enum class ValidationRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<RuleMetadata>) -> Boolean) : Rule<RuleData<RuleMetadata>> {
    // TODO: Use this ruleId for when the TPS SOAP call returns that the person is missing
    @Description("Pasienten sitt fødselsnummer eller D-nummer er ikke 11 tegn.")
    INVALID_FNR_SIZE(1002, Status.INVALID, { (healthInformation, _) ->
        healthInformation.pasient.fodselsnummer.id.length != 11
    }),

    @Description("Fødselsnummer/D-nummer kan passerer ikke modulus 11")
    INVALID_FNR(1006, Status.INVALID, { (healthInformation, _) ->
        validatePersonAndDNumber(healthInformation.pasient.fodselsnummer.id)
    }),

    @Description("Hele sykmeldingsperioden er før bruker har fylt 13 år. Pensjonsopptjening kan starte fra 13 år.")
    YOUNGER_THAN_13(1101, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.sortedTOMDate().last().toLocalDate() < extractBornDate(healthInformation.pasient.fodselsnummer.id).plusYears(13)
    }),

    @Description("Hele sykmeldingsperioden er etter at bruker har fylt 70 år. Dersom bruker fyller 70 år i perioden skal sykmelding gå gjennom på vanlig måte.")
    PATIENT_OVER_70_YEARS(1102, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.sortedFOMDate().first().toLocalDate() > extractBornDate(healthInformation.pasient.fodselsnummer.id).plusYears(70)
    }),

    @Description("Ukjent diagnosekode type")
    UNKNOWN_DIAGNOSECODE_TYPE(1137, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose.diagnosekode.s !in Diagnosekode.values()
    }),

    @Description("Hvis hoveddiagnose er Z-diagnose (ICPC-2), avvises meldingen.")
    ICPC_2_Z_DIAGNOSE(1132, Status.INVALID, { (healthInformation, _) ->
        // To support not having a main diagnosis and avoid checking for invalid code here we allow null and compare
        // with the equality operator
        (healthInformation.medisinskVurdering.hovedDiagnose.diagnosekode.toICPC2()?.first()?.codeValue?.startsWith("Z") == true)
    }),

    @Description("Hvis hoveddiagnose mangler og det ikke er angitt annen lovfestet fraværsgrunn, avvises meldingen")
    MAIN_DIAGNOSE_MISSING_AND_MISSING_REASON(1133, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.annenFraversArsak == null &&
                healthInformation.medisinskVurdering.hovedDiagnose.let { it == null || it.diagnosekode == null || it.diagnosekode.v == null }
    }),

    @Description("Hvis ICPC prosessdiagnose er oppgitt skal meldingen avvises")
    ICPC_PROCESS_DIAGNOSIS(1142, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose.diagnosekode.isICPC2() &&
                healthInformation.medisinskVurdering.hovedDiagnose.diagnosekode.v.startsWith("-")
    }),

    @Description("Hvis kodeverk ikke er angitt eller korrekt for hoveddiagnose, avvises meldingen.")
    INVALID_KODEVERK_FOR_MAIN_DIAGNOSE(1540, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose.diagnosekode.let { cv ->
            if (cv.isICPC2()) {
                ICPC2.values().any { it.codeValue == cv.v }
            } else {
                ICD10.values().any { it.codeValue == cv.v }
            }
        }
    }),

    @Description("Hvis medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt og sykmeldingen ikke er \"forenklet\"")
    NO_MEDICAL_OR_WORKPLACE_RELATED_REASONS(1706, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose.diagnosekode.toICPC2()?.any { icpc2 -> icpc2 in diagnoseCodesSimplified } == true &&
                !healthInformation.aktivitet.periode.all {
                    (it.gradertSykmelding == null || it.gradertSykmelding.sykmeldingsgrad == 100) &&
                            it.aktivitetIkkeMulig?.arbeidsplassen?.arsakskode != null &&
                            it.aktivitetIkkeMulig.medisinskeArsaker?.arsakskode != null
                } == true
    }),

    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 8.17, 39 uker før regelsettversjon \"2\" er innført skal sykmeldingen avvises")
    // TODO: Endre navn på denne etter diskusjon med fag
    MISSING_REQUIRED_DYNAMIC_QUESTIONS(1707, Status.INVALID, { (healthInformation, _) ->
        false // TODO: Diskutere med fag mtp hva vi skal gjøre med regelsettversjon
    }),

    @Description("Hvis regelsettversjon som er angitt i fagmelding ikke eksisterer så skal meldingen returneres")
    INVALID_RULESET_VERSION(1708, Status.INVALID, { (healthInformation, _) ->
        false // TODO: Denne trenger også en diskusjon med fag
    }),

    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt  ved  7.17, 39 uker etter innføring av regelsettversjon \"2\" så skal sykmeldingen avvises")
    MISSING_REQUIRED_DYNAMIC_QUESTIONS_AFTER_RULE_SET_VERSION_2(1709, Status.INVALID, { (healthInformation, _) ->
        false // TODO: Er dette egentlig det samme som 1707? Ser ut som om de matcher på det samme, bare at de sjekker på regelsettversjoner
        // Mulig dette er kodeduplikat etter merging av regler
    })
}
