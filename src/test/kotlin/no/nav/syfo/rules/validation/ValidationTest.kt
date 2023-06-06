package no.nav.syfo.rules.validation

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.client.Behandler
import no.nav.syfo.generateBehandler
import no.nav.syfo.generateMedisinskVurdering
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.SporsmalSvar
import no.nav.syfo.model.Status
import no.nav.syfo.model.SvarRestriksjon
import no.nav.syfo.questions.QuestionGroup
import no.nav.syfo.questions.QuestionId
import no.nav.syfo.services.BehandlerOgStartdato
import no.nav.syfo.services.RuleMetadataSykmelding
import no.nav.syfo.services.SykmeldingMetadataInfo
import no.nav.syfo.validation.validatePersonAndDNumber
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val personNumberDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy")

class ValidationTest : FunSpec({

    val ruleTree = ValidationRulesExecution()

    context("Testing validation rules and checking the rule outcomes") {
        test("Alt ok, Status OK") {
            val person14Years = LocalDate.now().minusYears(14)

            val sykmelding = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.7170",
                        kode = "R24",
                        tekst = "Blodig oppspytt/hemoptyse",
                    ),
                ),
            )

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = generatePersonNumber(person14Years, false),
                rulesetVersion = null,
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person14Years,
            )

            val ruleMetadataSykmelding = ruleMetadataSykmelding(ruleMetadata)

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding).first

            status.treeResult.status shouldBeEqualTo Status.OK
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to false,
                ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 to false,
                ValidationRules.UGYLDIG_ORGNR_LENGDE to false,
                ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR to false,
                ValidationRules.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR to false,
                ValidationRules.UNDER_3_ORD_DYNAMISKE_SPORSMAAL_SVAR to false,
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "ugyldigRegelsettversjon" to false,
                "manglendeDynamiskesporsmaalversjon2uke39" to false,
                "ugyldingOrgNummerLengde" to false,
                "avsenderSammeSomPasient" to false,
                "behandlerSammeSomPasient" to false,
                "utdypendeOpplysninger" to emptyMap<String, Map<String, SporsmalSvar>>(),
                "group63" to false,
                "group64" to false,
                "group65" to false,
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo null
        }
        test("Pasient under 13 Aar, Status INVALID") {
            val person12Years = LocalDate.now().minusYears(12)

            val sykmelding = generateSykmelding()

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = generatePersonNumber(person12Years, false),
                rulesetVersion = null,
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person12Years,
            )
            val ruleMetadataSykmelding = ruleMetadataSykmelding(ruleMetadata)

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding).first

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to true,
            )

            mapOf(
                "pasientUnder13Aar" to true,

            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo ValidationRuleHit.PASIENT_YNGRE_ENN_13.ruleHit
        }

        test("Ugyldig regelsettversjon, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val sykmelding = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.7170",
                        kode = "R24",
                        tekst = "Blodig oppspytt/hemoptyse",
                    ),
                ),
            )

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = generatePersonNumber(person31Years, false),
                rulesetVersion = "9999",
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person31Years,
            )
            val ruleMetadataSykmelding = ruleMetadataSykmelding(ruleMetadata)

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding).first

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to true,
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "ugyldigRegelsettversjon" to true,
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo ValidationRuleHit.UGYLDIG_REGELSETTVERSJON.ruleHit
        }
        test("Mangelde dynamiske sporsmaal versjon 2 uke39, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now().minusDays(274),
                        tom = LocalDate.now(),
                    ),
                ),
            )

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = generatePersonNumber(person31Years, false),
                rulesetVersion = "2",
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person31Years,
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to false,
                ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 to true,
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "ugyldigRegelsettversjon" to false,
                "manglendeDynamiskesporsmaalversjon2uke39" to true,
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo ValidationRuleHit.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39.ruleHit
        }

        test("Ugyldig orgnummer lengede, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val sykmelding = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.7170",
                        kode = "R24",
                        tekst = "Blodig oppspytt/hemoptyse",
                    ),
                ),
            )

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = generatePersonNumber(person31Years, false),
                rulesetVersion = "2",
                legekontorOrgnr = "1232344",
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person31Years,
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to false,
                ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 to false,
                ValidationRules.UGYLDIG_ORGNR_LENGDE to true,
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "ugyldigRegelsettversjon" to false,
                "manglendeDynamiskesporsmaalversjon2uke39" to false,
                "ugyldingOrgNummerLengde" to true,
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo ValidationRuleHit.UGYLDIG_ORGNR_LENGDE.ruleHit
        }
        test("Avsender samme som pasient, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val patientPersonNumber = generatePersonNumber(person31Years, false)

            val sykmelding = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.7170",
                        kode = "R24",
                        tekst = "Blodig oppspytt/hemoptyse",
                    ),
                ),
            )

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = patientPersonNumber,
                rulesetVersion = null,
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = patientPersonNumber,
                pasientFodselsdato = person31Years,
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to false,
                ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 to false,
                ValidationRules.UGYLDIG_ORGNR_LENGDE to false,
                ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR to true,
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "ugyldigRegelsettversjon" to false,
                "manglendeDynamiskesporsmaalversjon2uke39" to false,
                "ugyldingOrgNummerLengde" to false,
                "avsenderSammeSomPasient" to true,
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo ValidationRuleHit.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR.ruleHit
        }

        test("Behandler samme som pasient, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val patientPersonNumber = generatePersonNumber(person31Years, false)

            val sykmelding = generateSykmelding(
                behandler = generateBehandler(
                    "Per",
                    "",
                    "Hansen",
                    "134",
                    "113",
                    patientPersonNumber,
                ),
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.7170",
                        kode = "R24",
                        tekst = "Blodig oppspytt/hemoptyse",
                    ),
                ),
            )

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = patientPersonNumber,
                rulesetVersion = null,
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person31Years,
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to false,
                ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 to false,
                ValidationRules.UGYLDIG_ORGNR_LENGDE to false,
                ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR to false,
                ValidationRules.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR to true,
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "ugyldigRegelsettversjon" to false,
                "manglendeDynamiskesporsmaalversjon2uke39" to false,
                "ugyldingOrgNummerLengde" to false,
                "avsenderSammeSomPasient" to false,
                "behandlerSammeSomPasient" to true,
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo ValidationRuleHit.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR.ruleHit
        }

        test("Utdypendeopplysinger under 3 ord sporsmal gruppe 6.3, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val patientPersonNumber = generatePersonNumber(person31Years, false)

            val utdypendeOpplysningerMap = mapOf(
                QuestionGroup.GROUP_6_3.spmGruppeId to mapOf(
                    QuestionId.ID_6_3_1.spmId to SporsmalSvar(
                        QuestionId.ID_6_3_1.spmTekst,
                        "to ord",
                        listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                    ),
                    QuestionId.ID_6_3_2.spmId to SporsmalSvar(
                        QuestionId.ID_6_3_2.spmTekst,
                        "tre ord ord",
                        listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                    ),
                ),
            )

            val sykmelding = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.7170",
                        kode = "R24",
                        tekst = "Blodig oppspytt/hemoptyse",
                    ),
                ),
                utdypendeOpplysninger = utdypendeOpplysningerMap,
            )

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = patientPersonNumber,
                rulesetVersion = "3",
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person31Years,
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to false,
                ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 to false,
                ValidationRules.UGYLDIG_ORGNR_LENGDE to false,
                ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR to false,
                ValidationRules.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR to false,
                ValidationRules.UNDER_3_ORD_DYNAMISKE_SPORSMAAL_SVAR to true,
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "ugyldigRegelsettversjon" to false,
                "manglendeDynamiskesporsmaalversjon2uke39" to false,
                "ugyldingOrgNummerLengde" to false,
                "avsenderSammeSomPasient" to false,
                "behandlerSammeSomPasient" to false,
                "utdypendeOpplysninger" to utdypendeOpplysningerMap,
                "group63" to true,
                "group64" to false,
                "group65" to false,
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo ValidationRuleHit.UNDER_3_ORD_DYNAMISKE_SPORSMAAL_SVAR.ruleHit
        }

        test("Utdypendeopplysinger under 3 ord sporsmal gruppe 6.4, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val patientPersonNumber = generatePersonNumber(person31Years, false)

            val utdypendeOpplysningerMap = mapOf(
                QuestionGroup.GROUP_6_4.spmGruppeId to mapOf(
                    QuestionId.ID_6_4_1.spmId to SporsmalSvar(
                        QuestionId.ID_6_4_1.spmTekst,
                        "to ord",
                        listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                    ),
                    QuestionId.ID_6_4_2.spmId to SporsmalSvar(
                        QuestionId.ID_6_4_2.spmTekst,
                        "tre ord ord",
                        listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                    ),
                    QuestionId.ID_6_4_3.spmId to SporsmalSvar(
                        QuestionId.ID_6_4_3.spmTekst,
                        "tre ord ord",
                        listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                    ),
                ),
            )

            val sykmelding = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.7170",
                        kode = "R24",
                        tekst = "Blodig oppspytt/hemoptyse",
                    ),
                ),
                utdypendeOpplysninger = utdypendeOpplysningerMap,
            )

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = patientPersonNumber,
                rulesetVersion = "3",
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person31Years,
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to false,
                ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 to false,
                ValidationRules.UGYLDIG_ORGNR_LENGDE to false,
                ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR to false,
                ValidationRules.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR to false,
                ValidationRules.UNDER_3_ORD_DYNAMISKE_SPORSMAAL_SVAR to true,
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "ugyldigRegelsettversjon" to false,
                "manglendeDynamiskesporsmaalversjon2uke39" to false,
                "ugyldingOrgNummerLengde" to false,
                "avsenderSammeSomPasient" to false,
                "behandlerSammeSomPasient" to false,
                "utdypendeOpplysninger" to utdypendeOpplysningerMap,
                "group63" to false,
                "group64" to true,
                "group65" to false,
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo ValidationRuleHit.UNDER_3_ORD_DYNAMISKE_SPORSMAAL_SVAR.ruleHit
        }

        test("Utdypendeopplysinger under 3 ord sporsmal gruppe 6.5, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val patientPersonNumber = generatePersonNumber(person31Years, false)

            val utdypendeOpplysningerMap = mapOf(
                QuestionGroup.GROUP_6_5.spmGruppeId to mapOf(
                    QuestionId.ID_6_5_1.spmId to SporsmalSvar(
                        QuestionId.ID_6_5_1.spmTekst,
                        "to ord",
                        listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                    ),
                    QuestionId.ID_6_5_2.spmId to SporsmalSvar(
                        QuestionId.ID_6_5_2.spmTekst,
                        "tre ord ord",
                        listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                    ),
                    QuestionId.ID_6_5_3.spmId to SporsmalSvar(
                        QuestionId.ID_6_5_3.spmTekst,
                        "tre ord ord",
                        listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                    ),
                    QuestionId.ID_6_5_4.spmId to SporsmalSvar(
                        QuestionId.ID_6_5_4.spmTekst,
                        "tre ord ord",
                        listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                    ),
                ),
            )

            val sykmelding = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.7170",
                        kode = "R24",
                        tekst = "Blodig oppspytt/hemoptyse",
                    ),
                ),
                utdypendeOpplysninger = utdypendeOpplysningerMap,
            )

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = patientPersonNumber,
                rulesetVersion = "3",
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person31Years,
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to false,
                ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 to false,
                ValidationRules.UGYLDIG_ORGNR_LENGDE to false,
                ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR to false,
                ValidationRules.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR to false,
                ValidationRules.UNDER_3_ORD_DYNAMISKE_SPORSMAAL_SVAR to true,
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "ugyldigRegelsettversjon" to false,
                "manglendeDynamiskesporsmaalversjon2uke39" to false,
                "ugyldingOrgNummerLengde" to false,
                "avsenderSammeSomPasient" to false,
                "behandlerSammeSomPasient" to false,
                "utdypendeOpplysninger" to utdypendeOpplysningerMap,
                "group63" to false,
                "group64" to false,
                "group65" to true,
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo ValidationRuleHit.UNDER_3_ORD_DYNAMISKE_SPORSMAAL_SVAR.ruleHit
        }
    }
})

fun ruleMetadataSykmelding(ruleMetadata: RuleMetadata) = RuleMetadataSykmelding(
    ruleMetadata = ruleMetadata,
    sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
    doctorSuspensjon = false,
    behandlerOgStartdato = BehandlerOgStartdato(Behandler(emptyList(), null), null),
)

fun generatePersonNumber(bornDate: LocalDate, useDNumber: Boolean = false): String {
    val personDate = bornDate.format(personNumberDateFormat).let {
        if (useDNumber) "${it[0] + 4}${it.substring(1)}" else it
    }
    return (if (bornDate.year >= 2000) (75011..99999) else (11111..50099))
        .map { "$personDate$it" }
        .first {
            validatePersonAndDNumber(it)
        }
}
