package no.nav.syfo.rules

import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object LegesuspensjonRuleChainSpek : Spek({
    describe("Testing validation rules and checking the rule outcomes") {
        it("Should check rule BEHANDLER_SUSPENDERT, should trigger rule") {
            val suspended = true

            LegesuspensjonRuleChain(suspended).getRuleByName("BEHANDLER_SUSPENDERT")
                .executeRule()
                .result shouldBeEqualTo true
        }

        it("Should check rule BEHANDLER_SUSPENDERT, should NOT trigger rule") {
            val suspended = false

            LegesuspensjonRuleChain(suspended).getRuleByName("BEHANDLER_SUSPENDERT")
                .executeRule().result shouldBeEqualTo false
        }
    }
})
