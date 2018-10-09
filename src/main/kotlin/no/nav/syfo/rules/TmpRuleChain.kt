package no.nav.syfo.rules

import no.nav.syfo.Rule
import no.nav.syfo.RuleChain
import no.nav.syfo.model.Status

val tmpRuleChain = RuleChain<String>("Temporary rule chain", "Test rule chain", listOf(
        Rule("Test rule", -1, Status.INVALID, "this is a simple test rule") {
            it == "true"
        },
        Rule("Test rule", -2, Status.INVALID, "this is another simple test rule") {
            it == "test"
        }
))
