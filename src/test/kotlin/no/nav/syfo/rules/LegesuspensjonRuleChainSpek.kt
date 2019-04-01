package no.nav.syfo.rules

import no.nav.syfo.RuleData
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Sykmelding
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object LegesuspensjonRuleChainSpek : Spek({

    fun ruleData(
        healthInformation: Sykmelding,
        suspended: Boolean
    ): RuleData<Boolean> = RuleData(healthInformation, suspended)

    describe("Testing validation rules and checking the rule outcomes") {
        it("Should check rule BEHANDLER_SUSPENDED, should trigger rule") {
            val healthInformation = generateSykmelding()
            val suspended = true

            LegesuspensjonRuleChain.BEHANDLER_SUSPENDED(ruleData(healthInformation, suspended)) shouldEqual true
        }

        it("Should check rule BEHANDLER_SUSPENDED, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val suspended = false

            LegesuspensjonRuleChain.BEHANDLER_SUSPENDED(ruleData(healthInformation, suspended)) shouldEqual false
        }
    }
})
