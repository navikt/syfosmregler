package no.nav.syfo.rules.validation

import no.nav.syfo.model.Status
import no.nav.syfo.model.juridisk.JuridiskHenvisning

enum class RuleHit(
    val messageForSender: String,
    val messageForUser: String,
    val juridiskHenvisning: JuridiskHenvisning?,
    status: Status
) {
    PASIENT_YNGRE_ENN_13(
        status = Status.INVALID,
        messageForSender = "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
        messageForUser = "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
        juridiskHenvisning = null
    )
}
