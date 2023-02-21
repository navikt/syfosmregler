package no.nav.syfo.rules.legesuspensjon

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.model.Status
import org.amshove.kluent.shouldBeEqualTo
import java.util.UUID

class LegeSuspensjonTest : FunSpec({
    val ruleTree = LegeSuspensjonRulesExecution()

    context("Testing legesuspensjon rules and checking the rule outcomes") {
        test("Er ikkje suspendert, Status OK") {
            val sykmeldingid = UUID.randomUUID().toString()

            val suspendert = false

            val status = ruleTree.runRules(sykmeldingid, suspendert)

            status.treeResult.status shouldBeEqualTo Status.OK
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                LegeSuspensjonRules.BEHANDLER_SUSPENDERT to false
            )

            mapOf(
                "suspendert" to false
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo null
        }

        test("Er suspendert, Status INVALID") {
            val sykmeldingid = UUID.randomUUID().toString()

            val suspendert = true

            val status = ruleTree.runRules(sykmeldingid, suspendert)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                LegeSuspensjonRules.BEHANDLER_SUSPENDERT to true
            )
            mapOf(
                "suspendert" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo LegeSuspensjonRuleHit.BEHANDLER_SUSPENDERT.ruleHit
        }
    }
})
