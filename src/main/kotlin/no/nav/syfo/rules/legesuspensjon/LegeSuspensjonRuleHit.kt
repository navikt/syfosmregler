package no.nav.syfo.rules.legesuspensjon

import no.nav.syfo.model.Status
import no.nav.syfo.rules.common.RuleHit

enum class LegeSuspensjonRuleHit(
    val ruleHit: RuleHit
) {
    BEHANDLER_SUSPENDERT(
        ruleHit = RuleHit(
            rule = "BEHANDLER_SUSPENDERT",
            status = Status.INVALID,
            messageForSender = "Behandler er suspendert av NAV på konsultasjonstidspunkt. Pasienten har fått beskjed.",
            messageForUser = "Den som sykmeldte deg har mistet retten til å skrive sykmeldinger.",
        )
    )
}
