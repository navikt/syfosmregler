package no.nav.syfo.rules

import no.nav.syfo.OutcomeType
import no.nav.syfo.Rule
import no.nav.syfo.RuleChain

val tmpRuleChain = RuleChain<String>("Test rule chain", listOf(
        Rule(OutcomeType.TEST_RULE, "this is a simple test rule") {
            it == "true"
        },
        Rule(OutcomeType.TEST_RULE1, "this is another simple test rule") {
            it == "test"
        }
))
