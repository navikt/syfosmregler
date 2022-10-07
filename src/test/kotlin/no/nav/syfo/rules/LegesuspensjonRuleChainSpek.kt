package no.nav.syfo.rules

import io.kotest.core.spec.style.FunSpec
import org.amshove.kluent.shouldBeEqualTo

object LegesuspensjonRuleChainSpek : FunSpec({
    context("Testing validation rules and checking the rule outcomes") {
        test("Should check rule BEHANDLER_SUSPENDERT, should trigger rule") {
            val suspended = true

            LegesuspensjonRuleChain(suspended).getRuleByName("BEHANDLER_SUSPENDERT")
                .executeRule()
                .result shouldBeEqualTo true
        }

        test("Should check rule BEHANDLER_SUSPENDERT, should NOT trigger rule") {
            val suspended = false

            LegesuspensjonRuleChain(suspended).getRuleByName("BEHANDLER_SUSPENDERT")
                .executeRule().result shouldBeEqualTo false
        }
    }
})
