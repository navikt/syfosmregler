package no.nav.syfo

import no.nav.syfo.model.Status

data class Rule<in T>(val name: String, val outcomeType: OutcomeType, val description: String, val predicate: (T) -> Boolean)
data class Outcome(val outcomeType: OutcomeType, val description: String)

data class RuleChain<in T>(val name: String, val description: String, val rules: List<Rule<T>>) {
    fun executeFlow(input: T): List<Outcome> = rules
            .filter { RULE_HIT_SUMMARY.labels(it.outcomeType.name).startTimer().use { _ -> it.predicate(input) } }
            .onEach { RULE_HIT_COUNTER.labels(it.outcomeType.name) }
            .map { Outcome(it.outcomeType, it.description) }
}

enum class OutcomeType(val ruleId: Int, val status: Status) {
    PATIENT_YOUNGER_THAN_13(1101, Status.INVALID),
    PATIENT_OLDER_THAN_70(1102, Status.INVALID),
    SIGNATURE_DATE_TOO_OLD(1110, Status.INVALID),
    INVALID_CODE_SYSTEM(1137, Status.INVALID),
    TEST_RULE(1001, Status.INVALID),
    TEST_RULE1(1002, Status.INVALID)
}
