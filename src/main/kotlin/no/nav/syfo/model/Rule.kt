package no.nav.syfo.model

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.syfo.model.juridisk.JuridiskHenvisning
import no.nav.syfo.objectMapper

class Rule<RuleInput>(
    val name: String,
    val ruleId: Int?,
    val status: Status,
    val messageForUser: String,
    val messageForSender: String,
    val juridiskHenvisning: JuridiskHenvisning?,
    val input: RuleInput,
    val predicate: (input: RuleInput) -> Boolean,
) {
    fun executeRule(): RuleResult<RuleInput> {
        val result = predicate(input)
        return RuleResult(
            result = result,
            ruleInput = input,
            rule = this,
        )
    }

    fun toInputMap(): Map<String, Any> = objectMapper.convertValue(this.input as Any)
}

class RuleResult<RuleInput>(
    val result: Boolean,
    val ruleInput: RuleInput,
    val rule: Rule<RuleInput>,
)
