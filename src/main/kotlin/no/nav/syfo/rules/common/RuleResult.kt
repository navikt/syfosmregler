package no.nav.syfo.rules.common

import no.nav.syfo.model.Status
import no.nav.syfo.model.juridisk.JuridiskHenvisning

data class RuleHit(
    val status: Status,
    val rule: String,
    val messageForUser: String,
    val messageForSender: String,
    val juridiskHenvisning: JuridiskHenvisning?
)

data class RuleResult(
    val status: Status,
    val ruleHit: RuleHit?
) {
    override fun toString(): String {
        return status.name + (ruleHit?.let { "->${ruleHit.rule}" } ?: "")
    }
}