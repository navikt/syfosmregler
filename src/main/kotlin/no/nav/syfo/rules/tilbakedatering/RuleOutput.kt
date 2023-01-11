package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.juridisk.JuridiskHenvisning

data class RuleOutput(
    val oldRuleName: String,
    val ruleId: String,
    val messageForSender: String,
    val messageForUser: String,
    val juridiskHenvisning: JuridiskHenvisning
)
