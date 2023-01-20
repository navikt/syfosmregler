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
                pasientFodselsdato = LocalDate.now().minusYears(14)
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.OK
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                ValidationRules.PASIENT_YNGRE_ENN_13 to false
            )

            mapOf(
                "pasientUnder13Aar" to false
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo null
        }
    }
})
