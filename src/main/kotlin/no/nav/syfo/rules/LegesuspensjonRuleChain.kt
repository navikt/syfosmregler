package no.nav.syfo.rules

import no.nav.syfo.model.Rule
import no.nav.syfo.model.RuleChain
import no.nav.syfo.model.Status

class LegesuspensjonRuleChain(
    private val behandlerSuspendert: Boolean,
) : RuleChain {
    override val rules: List<Rule<*>> = listOf(
        // Behandler er suspendert av NAV på konsultasjonstidspunkt
        Rule(
            name = "BEHANDLER_SUSPENDERT",
            ruleId = 1414,
            status = Status.INVALID,
            messageForUser = "Den som sykmeldte deg har mistet retten til å skrive sykmeldinger.",
            messageForSender = "Behandler er suspendert av NAV på konsultasjonstidspunkt. Pasienten har fått beskjed.",
            juridiskHenvisning = null,
            input = object {
                val suspendert = behandlerSuspendert
            },
            predicate = { it.suspendert }
        ),
    )
}
