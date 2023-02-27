package no.nav.syfo.rules.patientageover70

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.generateMedisinskVurdering
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.rules.validation.ruleMetadataSykmelding
import no.nav.syfo.validation.validatePersonAndDNumber
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PatientAgeOver70Test : FunSpec({

    val ruleTree = PatientAgeOver70RulesExecution()

    context("Testing patient age over 70 rule and checking the rule outcomes") {

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

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

            status.treeResult.status shouldBeEqualTo Status.OK
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PatientAgeOver70Rules.PASIENT_ELDRE_ENN_70 to false,
            )

            mapOf("pasientOver70Aar" to false) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo null
        }

        test("Pasient over 70, Status OK") {
            val person70Years = LocalDate.now().minusYears(71)

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
                patientPersonNumber = generatePersonNumber(person70Years, false),
                rulesetVersion = null,
                legekontorOrgnr = null,
                tssid = null,
                avsenderFnr = "2",
                pasientFodselsdato = person70Years
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PatientAgeOver70Rules.PASIENT_ELDRE_ENN_70 to true
            )

            mapOf("pasientOver70Aar" to true) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo PatientAgeOver70RuleHit.PASIENT_ELDRE_ENN_70.ruleHit
        }
    }
})

fun generatePersonNumber(bornDate: LocalDate, useDNumber: Boolean = false): String {
    val personNumberDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy")
    val personDate = bornDate.format(personNumberDateFormat).let {
        if (useDNumber) "${it[0] + 4}${it.substring(1)}" else it
    }
    return (if (bornDate.year >= 2000) (75011..99999) else (11111..50099))
        .map { "$personDate$it" }
        .first {
            validatePersonAndDNumber(it)
        }
}
