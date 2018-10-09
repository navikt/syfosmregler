package no.nav.syfo

import no.nav.syfo.model.Status

data class Rule<in T>(val name: String, val ruleId: Int, val status: Status, val description: String, val predicate: (T) -> Boolean)
data class Outcome(val outcomeName: String, val status: Status, val description: String)

data class RuleChain<in T>(val name: String, val description: String, val rules: List<Rule<T>>) {
    fun executeFlow(input: T): List<Outcome> = rules
            .filter { RULE_HIT_SUMMARY.labels(it.name).startTimer().use { _ -> it.predicate(input) } }
            .onEach { RULE_HIT_COUNTER.labels(it.name) }
            .map { Outcome(it.name, it.status, it.description) }
}

