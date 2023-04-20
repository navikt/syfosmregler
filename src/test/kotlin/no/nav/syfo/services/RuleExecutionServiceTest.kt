package no.nav.syfo.services

import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.common.RuleExecution
import no.nav.syfo.rules.common.RuleHit
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.common.UtenJuridisk
import no.nav.syfo.rules.dsl.TreeOutput
import org.amshove.kluent.shouldBeEqualTo

enum class TestRules {
    RULE1, RULE2, RUlE3
}

class RuleExecutionServiceTest : FunSpec({

    val sykmeldnig = mockk<Sykmelding>(relaxed = true)
    val ruleMetadataSykmelding = mockk<RuleMetadataSykmelding>(relaxed = true)
    val rulesExecution = mockk<RuleExecution<TestRules>>(relaxed = true)
    val ruleExecutionService = RuleExecutionService()

    test("Run ruleTrees") {

        every {
            rulesExecution.runRules(
                any(),
                any(),
            )
        } returns (
            TreeOutput<TestRules, RuleResult>(
                treeResult = RuleResult(
                    status = Status.OK,
                    ruleHit = null,
                ),
            ) to UtenJuridisk
            )

        val (rule, juridisk) = ruleExecutionService.runRules(sykmeldnig, ruleMetadataSykmelding, sequenceOf(rulesExecution)).first()
        rule.treeResult.status shouldBeEqualTo Status.OK
        juridisk shouldBeEqualTo UtenJuridisk
    }

    test("should not run all rules if first no OK") {

        val okRule = mockk<RuleExecution<TestRules>>().also {
            every { it.runRules(any(), any()) } returns (
                TreeOutput<TestRules, RuleResult>(
                    treeResult = RuleResult(
                        status = Status.OK,
                        ruleHit = null,
                    ),
                ) to UtenJuridisk
                )
        }
        val invalidRuleExecution = mockk<RuleExecution<TestRules>>().also {
            every { it.runRules(any(), any()) } returns (
                TreeOutput<TestRules, RuleResult>(
                    treeResult = RuleResult(
                        status = Status.INVALID,
                        ruleHit = RuleHit(Status.INVALID, TestRules.RULE2.name, "message", "message"),
                    ),
                ) to UtenJuridisk
                )
        }
        val results = ruleExecutionService.runRules(sykmeldnig, ruleMetadataSykmelding, sequenceOf(invalidRuleExecution, okRule))
        results.size shouldBeEqualTo 1
        results.first().first.treeResult.status shouldBeEqualTo Status.INVALID
    }
})
