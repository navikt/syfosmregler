package no.nav.syfo.rules.legesuspensjon

import no.nav.syfo.model.Status
import no.nav.syfo.model.juridisk.JuridiskHenvisning

enum class RuleHit(
    val messageForSender: String,
    val messageForUser: String,
    val juridiskHenvisning: JuridiskHenvisning?,
    val status: Status
) {
    BEHANDLER_SUSPENDERT(
        status = Status.INVALID,
        messageForSender = "Behandler er suspendert av NAV på konsultasjonstidspunkt. Pasienten har fått beskjed.",
        messageForUser = "Den som sykmeldte deg har mistet retten til å skrive sykmeldinger.",
        juridiskHenvisning = null
    )
}
