package no.nav.syfo.rules.validation

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.generateBehandler
import no.nav.syfo.generateMedisinskVurdering
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.rules.generatePersonNumber
import no.nav.syfo.sm.Diagnosekoder
import no.nav.syfo.toDiagnose
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate

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
                        tekst = "Blodig oppspytt/hemoptyse"
                    )
                  )
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
                pasientFodselsdato = person14Years
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.OK
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.PASIENT_ELDRE_ENN_70 to false,
                ValidationRules.UKJENT_DIAGNOSEKODETYPE to false,
                ValidationRules.ICPC_2_Z_DIAGNOSE to false,
                ValidationRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to false,
                ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 to false,
                ValidationRules.UGYLDIG_ORGNR_LENGDE to false,
                ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR to false,
                ValidationRules.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR to false
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "pasientOver70Aar" to false,
                "ukjentDiagnoseKodeType" to false,
                "icpc2ZDiagnose" to false,
                "houvedDiagnoseEllerFraversgrunnMangler" to false,
                "ugyldigKodeVerkHouvedDiagnose" to false,
                "ugyldigKodeVerkBiDiagnose" to false,
                "ugyldigRegelsettversjon" to false,
                "manglendeDynamiskesporsmaalversjon2uke39" to false,
                "ugyldingOrgNummerLengde" to false,
                "avsenderSammeSomPasient" to false,
                "behandlerSammeSomPasient" to false,

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
                pasientFodselsdato = person12Years
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to true
            )

            mapOf(
                "pasientUnder13Aar" to true,

            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.PASIENT_YNGRE_ENN_13
        }

        test("Pasient eldre enn 70 Aar, Status INVALID") {
            val person71Years = LocalDate.now().minusYears(71)

            val sykmelding = generateSykmelding()

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = generatePersonNumber(person71Years, false),
                rulesetVersion = null,
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person71Years
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.PASIENT_ELDRE_ENN_70 to true
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "pasientOver70Aar" to true

            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.PASIENT_ELDRE_ENN_70
        }
        test("Ukjent diagnoseKodeType, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val sykmelding = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.9999",
                        kode = "A09",
                        tekst = "Brudd legg/ankel"
                    )
                )
            )

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = generatePersonNumber(person31Years, false),
                rulesetVersion = null,
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person31Years
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.PASIENT_ELDRE_ENN_70 to false,
                ValidationRules.UKJENT_DIAGNOSEKODETYPE to true
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "pasientOver70Aar" to false,
                "ukjentDiagnoseKodeType" to true

            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.UKJENT_DIAGNOSEKODETYPE
        }
        test("Diagnosen er icpz 2 z diagnose, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val sykmelding = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnosekoder.icpc2["Z09"]!!.toDiagnose()
                )
            )

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = generatePersonNumber(person31Years, false),
                rulesetVersion = null,
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person31Years
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.PASIENT_ELDRE_ENN_70 to false,
                ValidationRules.UKJENT_DIAGNOSEKODETYPE to false,
                ValidationRules.ICPC_2_Z_DIAGNOSE to true
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "pasientOver70Aar" to false,
                "ukjentDiagnoseKodeType" to false,
                "icpc2ZDiagnose" to true

            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.ICPC_2_Z_DIAGNOSE
        }
        test("HouvedDiagnose eller fraversgrunn mangler, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val sykmelding = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = null,
                    annenFraversArsak = null
                )
            )

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = generatePersonNumber(person31Years, false),
                rulesetVersion = null,
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person31Years
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.PASIENT_ELDRE_ENN_70 to false,
                ValidationRules.UKJENT_DIAGNOSEKODETYPE to false,
                ValidationRules.ICPC_2_Z_DIAGNOSE to false,
                ValidationRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER to true
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "pasientOver70Aar" to false,
                "ukjentDiagnoseKodeType" to false,
                "icpc2ZDiagnose" to false,
                "houvedDiagnoseEllerFraversgrunnMangler" to true

            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER
        }
        test("Ugyldig KodeVerk for houvedDiagnose, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val sykmelding = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.7110",
                        kode = "Z09",
                        tekst = "Brudd legg/ankel"
                    )
                )
            )

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = generatePersonNumber(person31Years, false),
                rulesetVersion = null,
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person31Years
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.PASIENT_ELDRE_ENN_70 to false,
                ValidationRules.UKJENT_DIAGNOSEKODETYPE to false,
                ValidationRules.ICPC_2_Z_DIAGNOSE to false,
                ValidationRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to true
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "pasientOver70Aar" to false,
                "ukjentDiagnoseKodeType" to false,
                "icpc2ZDiagnose" to false,
                "houvedDiagnoseEllerFraversgrunnMangler" to false,
                "ugyldigKodeVerkHouvedDiagnose" to true

            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE
        }
        test("Ugyldig kodeVerk for biDiagnose, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val sykmelding = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    bidiagnoser = listOf(
                        Diagnose(
                            system = "2.16.578.1.12.4.1.1.7110",
                            kode = "S09",
                            tekst = "Brudd legg/ankel"
                        )
                    )
                )
            )

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = generatePersonNumber(person31Years, false),
                rulesetVersion = null,
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person31Years
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.PASIENT_ELDRE_ENN_70 to false,
                ValidationRules.UKJENT_DIAGNOSEKODETYPE to false,
                ValidationRules.ICPC_2_Z_DIAGNOSE to false,
                ValidationRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE to true,
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "pasientOver70Aar" to false,
                "ukjentDiagnoseKodeType" to false,
                "icpc2ZDiagnose" to false,
                "houvedDiagnoseEllerFraversgrunnMangler" to false,
                "ugyldigKodeVerkHouvedDiagnose" to false,
                "ugyldigKodeVerkBiDiagnose" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE
        }
        test("Ugyldig regelsettversjon, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val sykmelding = generateSykmelding()

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = generatePersonNumber(person31Years, false),
                rulesetVersion = "9999",
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person31Years
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.PASIENT_ELDRE_ENN_70 to false,
                ValidationRules.UKJENT_DIAGNOSEKODETYPE to false,
                ValidationRules.ICPC_2_Z_DIAGNOSE to false,
                ValidationRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to true
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "pasientOver70Aar" to false,
                "ukjentDiagnoseKodeType" to false,
                "icpc2ZDiagnose" to false,
                "houvedDiagnoseEllerFraversgrunnMangler" to false,
                "ugyldigKodeVerkHouvedDiagnose" to false,
                "ugyldigKodeVerkBiDiagnose" to false,
                "ugyldigRegelsettversjon" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.UGYLDIG_REGELSETTVERSJON
        }
        test("Mangelde dynamiske sporsmaal versjon 2 uke39, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now().minusDays(274),
                        tom = LocalDate.now()
                    )
                )
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
                pasientFodselsdato = person31Years
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.PASIENT_ELDRE_ENN_70 to false,
                ValidationRules.UKJENT_DIAGNOSEKODETYPE to false,
                ValidationRules.ICPC_2_Z_DIAGNOSE to false,
                ValidationRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to false,
                ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 to true
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "pasientOver70Aar" to false,
                "ukjentDiagnoseKodeType" to false,
                "icpc2ZDiagnose" to false,
                "houvedDiagnoseEllerFraversgrunnMangler" to false,
                "ugyldigKodeVerkHouvedDiagnose" to false,
                "ugyldigKodeVerkBiDiagnose" to false,
                "ugyldigRegelsettversjon" to false,
                "manglendeDynamiskesporsmaalversjon2uke39" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39
        }

        test("Ugyldig orgnummer lengede, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val sykmelding = generateSykmelding()

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = generatePersonNumber(person31Years, false),
                rulesetVersion = "2",
                legekontorOrgnr = "1232344",
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person31Years
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.PASIENT_ELDRE_ENN_70 to false,
                ValidationRules.UKJENT_DIAGNOSEKODETYPE to false,
                ValidationRules.ICPC_2_Z_DIAGNOSE to false,
                ValidationRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to false,
                ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 to false,
                ValidationRules.UGYLDIG_ORGNR_LENGDE to true
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "pasientOver70Aar" to false,
                "ukjentDiagnoseKodeType" to false,
                "icpc2ZDiagnose" to false,
                "houvedDiagnoseEllerFraversgrunnMangler" to false,
                "ugyldigKodeVerkHouvedDiagnose" to false,
                "ugyldigKodeVerkBiDiagnose" to false,
                "ugyldigRegelsettversjon" to false,
                "manglendeDynamiskesporsmaalversjon2uke39" to false,
                "ugyldingOrgNummerLengde" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.UGYLDIG_ORGNR_LENGDE
        }
        test("Avsender samme som pasient, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val patientPersonNumber = generatePersonNumber(person31Years, false)

            val sykmelding = generateSykmelding()

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = patientPersonNumber,
                rulesetVersion = null,
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = patientPersonNumber,
                pasientFodselsdato = person31Years
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.PASIENT_ELDRE_ENN_70 to false,
                ValidationRules.UKJENT_DIAGNOSEKODETYPE to false,
                ValidationRules.ICPC_2_Z_DIAGNOSE to false,
                ValidationRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to false,
                ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 to false,
                ValidationRules.UGYLDIG_ORGNR_LENGDE to false,
                ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR to true
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "pasientOver70Aar" to false,
                "ukjentDiagnoseKodeType" to false,
                "icpc2ZDiagnose" to false,
                "houvedDiagnoseEllerFraversgrunnMangler" to false,
                "ugyldigKodeVerkHouvedDiagnose" to false,
                "ugyldigKodeVerkBiDiagnose" to false,
                "ugyldigRegelsettversjon" to false,
                "manglendeDynamiskesporsmaalversjon2uke39" to false,
                "ugyldingOrgNummerLengde" to false,
                "avsenderSammeSomPasient" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR
        }

        test("Behandler samme som pasient, Status INVALID") {
            val person31Years = LocalDate.now().minusYears(31)

            val patientPersonNumber = generatePersonNumber(person31Years, false)

            val sykmelding = generateSykmelding(
                behandler = generateBehandler(
                    "Per", "", "Hansen", "134", "113", patientPersonNumber
                ),
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.7170",
                        kode = "R24",
                        tekst = "Blodig oppspytt/hemoptyse"
                    )
                )
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
                pasientFodselsdato = person31Years
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false,
                ValidationRules.PASIENT_ELDRE_ENN_70 to false,
                ValidationRules.UKJENT_DIAGNOSEKODETYPE to false,
                ValidationRules.ICPC_2_Z_DIAGNOSE to false,
                ValidationRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to false,
                ValidationRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE to false,
                ValidationRules.UGYLDIG_REGELSETTVERSJON to false,
                ValidationRules.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39 to false,
                ValidationRules.UGYLDIG_ORGNR_LENGDE to false,
                ValidationRules.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR to false,
                ValidationRules.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR to true
            )

            mapOf(
                "pasientUnder13Aar" to false,
                "pasientOver70Aar" to false,
                "ukjentDiagnoseKodeType" to false,
                "icpc2ZDiagnose" to false,
                "houvedDiagnoseEllerFraversgrunnMangler" to false,
                "ugyldigKodeVerkHouvedDiagnose" to false,
                "ugyldigKodeVerkBiDiagnose" to false,
                "ugyldigRegelsettversjon" to false,
                "manglendeDynamiskesporsmaalversjon2uke39" to false,
                "ugyldingOrgNummerLengde" to false,
                "avsenderSammeSomPasient" to false,
                "behandlerSammeSomPasient" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR
        }
    }
})
