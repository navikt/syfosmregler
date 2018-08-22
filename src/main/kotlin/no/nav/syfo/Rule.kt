package no.nav.syfo

data class Rule<in T>(val outcomeType: OutcomeType, val description: String, val predicate: (T) -> Boolean)
data class Outcome(val outcomeType: OutcomeType, val description: String)

data class RuleChain<in T>(val description: String, val rules: List<Rule<T>>) {
    fun executeFlow(input: T): List<Outcome> = rules
            .filter { RULE_HIT_SUMMARY.labels(it.outcomeType.name).startTimer().use { _ -> it.predicate(input) } }
            .onEach { RULE_HIT_COUNTER.labels(it.outcomeType.name) }
            .map { Outcome(it.outcomeType, it.description) }
}

enum class OutcomeType(val ruleId: Int) {
    TEST_RULE(1001),
    TEST_RULE1(1002)
}
