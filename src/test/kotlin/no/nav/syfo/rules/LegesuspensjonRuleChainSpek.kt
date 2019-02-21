package no.nav.syfo.rules

import no.nav.syfo.executeFlow
import no.nav.syfo.generateSykmelding
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object LegesuspensjonRuleChainSpek : Spek({

    describe("Testing validation rules and checking the rule outcomes") {
        it("Should check rule BEHANDLER_SUSPENDED, should trigger rule") {
            val healthInformation = generateSykmelding()
            val suspended = true

            val legesuspensjonRuleChainResults = LegesuspensjonRuleChain.values().toList().executeFlow(healthInformation, suspended)

            legesuspensjonRuleChainResults.any { it == LegesuspensjonRuleChain.BEHANDLER_SUSPENDED } shouldEqual true
        }

        it("Should check rule BEHANDLER_SUSPENDED, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val suspended = false

            val legesuspensjonRuleChainResults = LegesuspensjonRuleChain.values().toList().executeFlow(healthInformation, suspended)

            legesuspensjonRuleChainResults.any { it == LegesuspensjonRuleChain.BEHANDLER_SUSPENDED } shouldEqual false
        }
    }
})
