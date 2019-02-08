package no.nav.syfo.rules

import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.Description
import no.nav.syfo.Diagnosekode
import no.nav.syfo.ICD10
import no.nav.syfo.ICPC2
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.UtdypendeOpplysninger
import no.nav.syfo.contains
import no.nav.syfo.model.Status
import no.nav.syfo.validation.extractBornDate
import no.nav.syfo.validation.validatePersonAndDNumber
import no.nav.syfo.validation.validatePersonAndDNumber11Digits

enum class ValidationRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<RuleMetadata>) -> Boolean) : Rule<RuleData<RuleMetadata>> {
    // TODO: Use this ruleId for when the TPS SOAP call returns that the person is missing
    @Description("Pasienten sitt fødselsnummer eller D-nummer er ikke 11 tegn.")
    INVALID_FNR_SIZE(1002, Status.INVALID, { (healthInformation, _) ->
        !validatePersonAndDNumber11Digits(healthInformation.pasient.fodselsnummer.id)
    }),

    @Description("Fødselsnummer/D-nummer kan passerer ikke modulus 11")
    INVALID_FNR(1006, Status.INVALID, { (healthInformation, _) ->
        !validatePersonAndDNumber(healthInformation.pasient.fodselsnummer.id)
    }),

    @Description("Hele sykmeldingsperioden er før bruker har fylt 13 år. Pensjonsopptjening kan starte fra 13 år.")
    YOUNGER_THAN_13(1101, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.sortedTOMDate().last() < extractBornDate(healthInformation.pasient.fodselsnummer.id).plusYears(13)
    }),

    @Description("Hele sykmeldingsperioden er etter at bruker har fylt 70 år. Dersom bruker fyller 70 år i perioden skal sykmelding gå gjennom på vanlig måte.")
    PATIENT_OVER_70_YEARS(1102, Status.INVALID, { (healthInformation, _) ->
        healthInformation.aktivitet.periode.sortedFOMDate().first() > extractBornDate(healthInformation.pasient.fodselsnummer.id).plusYears(70)
    }),

    @Description("Ukjent diagnosekode type")
    UNKNOWN_DIAGNOSECODE_TYPE(1137, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose.diagnosekode.s !in Diagnosekode.values()
    }),

    @Description("Hvis hoveddiagnose er Z-diagnose (ICPC-2), avvises meldingen.")
    ICPC_2_Z_DIAGNOSE(1132, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering.hovedDiagnose.diagnosekode.toICPC2()?.first()?.codeValue?.startsWith("Z") == true
    }),

    @Description("Hvis hoveddiagnose mangler og det ikke er angitt annen lovfestet fraværsgrunn, avvises meldingen")
    MAIN_DIAGNOSE_MISSING_AND_MISSING_REASON(1133, Status.INVALID, { (healthInformation, _) ->
        healthInformation.medisinskVurdering?.annenFraversArsak == null &&
                healthInformation.medisinskVurdering?.hovedDiagnose.let { it == null || it.diagnosekode == null || it.diagnosekode.v == null }
    }),

    @Description("Hvis kodeverk ikke er angitt eller korrekt for hoveddiagnose, avvises meldingen.")
    INVALID_KODEVERK_FOR_MAIN_DIAGNOSE(1540, Status.INVALID, { (healthInformation, _) ->
        !healthInformation.medisinskVurdering.hovedDiagnose.diagnosekode.let { cv ->
            if (cv.isICPC2()) {
                ICPC2.values().any { it.codeValue == cv.v }
            } else {
                ICD10.values().any { it.codeValue == cv.v }
            }
        }
    }),

    // Revurder regel når IT ikkje lenger skal brukes
    @Description("Hvis kodeverk ikke er angitt eller korrekt for bidiagnose, avvises meldingen.")
    INVALID_KODEVERK_FOR_BI_DIAGNOSE(1541, Status.INVALID, { (healthInformation, _) ->
        !healthInformation.medisinskVurdering.biDiagnoser.diagnosekode.all { cv ->
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
                    healthInformation.aktivitet.periode
                    .mapNotNull { it.aktivitetIkkeMulig }
                    .any { it.arbeidsplassen.arsakskode == null && it.medisinskeArsaker.arsakskode == null }
    }),

    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 8.17, 39 uker før regelsettversjon \"2\" er innført skal sykmeldingen avvises")
    // TODO: Endre navn på denne etter diskusjon med fag og Diskutere med fag mtp hva vi skal gjøre med regelsettversjon
    MISSING_REQUIRED_DYNAMIC_QUESTIONS(1707, Status.INVALID, { (healthInformation, _) ->
        healthInformation.regelSettVersjon in arrayOf(null, "", "1") &&
                (healthInformation.utdypendeOpplysninger == null || !validateDynagruppe62(healthInformation.utdypendeOpplysninger.spmGruppe))
    }),

    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 8.17, 39 uker før regelsettversjon \"2\" er innført skal sykmeldingen avvises")
    // TODO: Endre navn på denne etter diskusjon med fag og Diskutere med fag mtp hva vi skal gjøre med regelsettversjon
    MISSING_REQUIRED_MEDICAL_REASON(1707, Status.INVALID, { (healthInformation, _) ->
        healthInformation.regelSettVersjon in arrayOf(null, "", "1") && (healthInformation.aktivitet?.periode ?: listOf())
                .filter { (it.periodeFOMDato..it.periodeTOMDato).daysBetween() > 56 }
                .none {
                    it.aktivitetIkkeMulig?.medisinskeArsaker?.beskriv.isNullOrBlank() && it.aktivitetIkkeMulig?.medisinskeArsaker?.arsakskode.isNullOrEmpty() ||
                            it.aktivitetIkkeMulig?.arbeidsplassen?.beskriv.isNullOrBlank() && it.aktivitetIkkeMulig?.arbeidsplassen?.arsakskode.isNullOrEmpty()
                }
    }),

    @Description("Hvis regelsettversjon som er angitt i fagmelding ikke eksisterer så skal meldingen returneres")
    INVALID_RULESET_VERSION(1708, Status.INVALID, { (healthInformation, _) ->
        healthInformation.regelSettVersjon !in arrayOf(null, "", "1", "2")
    }),

    @Description("Hvis utdypende opplysninger om medisinske eller arbeidsplassrelaterte årsaker ved 100% sykmelding ikke er oppgitt ved 7.17, 39 uker etter innføring av regelsettversjon \"2\" så skal sykmeldingen avvises")
    MISSING_REQUIRED_DYNAMIC_QUESTIONS_AFTER_RULE_SET_VERSION_2(1709, Status.INVALID, { (healthInformation, _) ->
        val arsakBeskrivelseAktivitetIkkeMulig = !healthInformation.aktivitet.periode.any {
            it.aktivitetIkkeMulig?.medisinskeArsaker?.beskriv.isNullOrBlank() && it.aktivitetIkkeMulig?.medisinskeArsaker?.arsakskode != null ||
                    it.aktivitetIkkeMulig?.arbeidsplassen?.beskriv.isNullOrBlank() && it.aktivitetIkkeMulig?.arbeidsplassen?.arsakskode != null
        }

        val timeGroup7Week = healthInformation.aktivitet.periode
                .any { (it.periodeFOMDato..it.periodeTOMDato).daysBetween() > 49 }
        val timeGroup17Week = healthInformation.aktivitet.periode
                .any { (it.periodeFOMDato..it.periodeTOMDato).daysBetween() > 119 }
        val timeGroup39Week = healthInformation.aktivitet.periode
                .any { (it.periodeFOMDato..it.periodeTOMDato).daysBetween() > 273 }

        val rulesettversion2 = healthInformation.regelSettVersjon == "2"

        when {
            timeGroup7Week -> arsakBeskrivelseAktivitetIkkeMulig && rulesettversion2 && !validateDynagruppe63(healthInformation.utdypendeOpplysninger.spmGruppe)
            timeGroup17Week -> arsakBeskrivelseAktivitetIkkeMulig && rulesettversion2 && !validateDynagruppe64(healthInformation.utdypendeOpplysninger.spmGruppe)
            timeGroup39Week -> arsakBeskrivelseAktivitetIkkeMulig && rulesettversion2 && !validateDynagruppe65(healthInformation.utdypendeOpplysninger.spmGruppe) || !validateDynagruppe66(healthInformation.utdypendeOpplysninger.spmGruppe)
            else -> false
        }
    })
}

fun validateDynagruppe62(spmgruppe: List<HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger.SpmGruppe>): Boolean {
        val spmGruppe62 = spmgruppe.find {
                    it.spmGruppeId == UtdypendeOpplysninger.DYNAGRUPPE6_2.spmGruppeId &&
                    it.spmGruppeTekst == UtdypendeOpplysninger.DYNAGRUPPE6_2.spmGruppeTekst
    }

    return spmGruppe62?.spmSvar?.size == UtdypendeOpplysninger.DYNAGRUPPE6_2.spmsvar.size &&
        UtdypendeOpplysninger.DYNAGRUPPE6_2.spmsvar.map { it.spmId } == spmGruppe62.spmSvar.map { it.spmId } &&
        UtdypendeOpplysninger.DYNAGRUPPE6_2.spmsvar.map { it.restriksjon.codeValue } == spmGruppe62.spmSvar.map { it.restriksjon.restriksjonskode.first().v } &&
        UtdypendeOpplysninger.DYNAGRUPPE6_2.spmsvar.map { it.spmTekst } == spmGruppe62.spmSvar.map { it.spmTekst } &&
        !spmGruppe62.spmSvar.any { it.svarTekst.isNullOrEmpty() }
}

fun validateDynagruppe63(spmgruppe: List<HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger.SpmGruppe>): Boolean {
    val spmGruppe63 = spmgruppe.find {
        it.spmGruppeId == UtdypendeOpplysninger.DYNAGRUPPE6_3.spmGruppeId &&
                it.spmGruppeTekst == UtdypendeOpplysninger.DYNAGRUPPE6_3.spmGruppeTekst
    }

    return spmGruppe63?.spmSvar?.size == UtdypendeOpplysninger.DYNAGRUPPE6_3.spmsvar.size &&
            UtdypendeOpplysninger.DYNAGRUPPE6_3.spmsvar.map { it.spmId } == spmGruppe63.spmSvar.map { it.spmId } &&
            UtdypendeOpplysninger.DYNAGRUPPE6_3.spmsvar.map { it.restriksjon.codeValue } == spmGruppe63.spmSvar.map { it.restriksjon.restriksjonskode.first().v } &&
            UtdypendeOpplysninger.DYNAGRUPPE6_3.spmsvar.map { it.spmTekst } == spmGruppe63.spmSvar.map { it.spmTekst } &&
            !spmGruppe63.spmSvar.any { it.svarTekst.isNullOrEmpty() }
}

fun validateDynagruppe64(spmgruppe: List<HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger.SpmGruppe>): Boolean {
    val spmGruppe64 = spmgruppe.find {
        it.spmGruppeId == UtdypendeOpplysninger.DYNAGRUPPE6_3.spmGruppeId &&
                it.spmGruppeTekst == UtdypendeOpplysninger.DYNAGRUPPE6_3.spmGruppeTekst
    }

    return spmGruppe64?.spmSvar?.size == UtdypendeOpplysninger.DYNAGRUPPE6_4.spmsvar.size &&
            UtdypendeOpplysninger.DYNAGRUPPE6_4.spmsvar.map { it.spmId } == spmGruppe64.spmSvar.map { it.spmId } &&
            UtdypendeOpplysninger.DYNAGRUPPE6_4.spmsvar.map { it.restriksjon.codeValue } == spmGruppe64.spmSvar.map { it.restriksjon.restriksjonskode.first().v } &&
            UtdypendeOpplysninger.DYNAGRUPPE6_4.spmsvar.map { it.spmTekst } == spmGruppe64.spmSvar.map { it.spmTekst } &&
            !spmGruppe64.spmSvar.any { it.svarTekst.isNullOrEmpty() }
}

fun validateDynagruppe65(spmgruppe: List<HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger.SpmGruppe>): Boolean {
    val spmGruppe65 = spmgruppe.find {
        it.spmGruppeId == UtdypendeOpplysninger.DYNAGRUPPE6_5.spmGruppeId &&
                it.spmGruppeTekst == UtdypendeOpplysninger.DYNAGRUPPE6_5.spmGruppeTekst
    }

    return spmGruppe65?.spmSvar?.size == UtdypendeOpplysninger.DYNAGRUPPE6_5.spmsvar.size &&
            UtdypendeOpplysninger.DYNAGRUPPE6_5.spmsvar.map { it.spmId } == spmGruppe65.spmSvar.map { it.spmId } &&
            UtdypendeOpplysninger.DYNAGRUPPE6_5.spmsvar.map { it.restriksjon.codeValue } == spmGruppe65.spmSvar.map { it.restriksjon.restriksjonskode.first().v } &&
            UtdypendeOpplysninger.DYNAGRUPPE6_5.spmsvar.map { it.spmTekst } == spmGruppe65.spmSvar.map { it.spmTekst } &&
            !spmGruppe65.spmSvar.any { it.svarTekst.isNullOrEmpty() }
}

// TODO merker at denne ikkje er obligatorisk, slik som dei andre
fun validateDynagruppe66(spmgruppe: List<HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger.SpmGruppe>): Boolean {
    val spmGruppe66 = spmgruppe.find {
        it.spmGruppeId == UtdypendeOpplysninger.DYNAGRUPPE6_6.spmGruppeId &&
                it.spmGruppeTekst == UtdypendeOpplysninger.DYNAGRUPPE6_6.spmGruppeTekst
    }

    return spmGruppe66?.spmSvar?.size == UtdypendeOpplysninger.DYNAGRUPPE6_6.spmsvar.size &&
            UtdypendeOpplysninger.DYNAGRUPPE6_6.spmsvar.map { it.spmId } == spmGruppe66.spmSvar.map { it.spmId } &&
            UtdypendeOpplysninger.DYNAGRUPPE6_6.spmsvar.map { it.restriksjon.codeValue } == spmGruppe66.spmSvar.map { it.restriksjon.restriksjonskode.first().v } &&
            UtdypendeOpplysninger.DYNAGRUPPE6_6.spmsvar.map { it.spmTekst } == spmGruppe66.spmSvar.map { it.spmTekst } &&
            !spmGruppe66.spmSvar.any { it.svarTekst.isNullOrEmpty() }
}
