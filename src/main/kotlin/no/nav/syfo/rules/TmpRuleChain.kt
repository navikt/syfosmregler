package no.nav.syfo.rules

import no.nav.syfo.OutcomeType
import no.nav.syfo.Rule
import no.nav.syfo.RuleChain

val tmpRuleChain = RuleChain<String>("Temporary rule chain", "Test rule chain", listOf(
        Rule("Test rule", OutcomeType.TEST_RULE, "this is a simple test rule") {
            it == "true"
        },
        Rule("Test rule", OutcomeType.TEST_RULE1, "this is another simple test rule") {
            it == "test"
        }
))
