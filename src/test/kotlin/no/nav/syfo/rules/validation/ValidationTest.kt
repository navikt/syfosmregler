package no.nav.syfo.rules.validation

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.rules.generatePersonNumber
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate

class ValidationTest : FunSpec({

    val ruleTree = ValidationRulesExecution()

    context("Testing validation rules and checking the rule outcomes") {
        test("Alt ok, Status OK") {
            val person13Years = LocalDate.now().minusYears(13)

            val sykmelding = generateSykmelding()

            val ruleMetadata = RuleMetadata(
                signatureDate = LocalDate.now().atStartOfDay(),
                receivedDate = LocalDate.now().atStartOfDay(),
                behandletTidspunkt = LocalDate.now().atStartOfDay(),
                patientPersonNumber = generatePersonNumber(person13Years, false),
                rulesetVersion = null,
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = LocalDate.now().minusYears(13)
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
                pasientFodselsdato = LocalDate.now().minusYears(12)
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
        // TODO create tests for all ValidationRules
    }
})
