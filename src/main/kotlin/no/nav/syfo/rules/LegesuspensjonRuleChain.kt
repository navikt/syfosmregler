package no.nav.syfo.rules

import no.nav.syfo.model.Status
import no.nav.syfo.model.juridisk.JuridiskHenvisning

enum class LegesuspensjonRuleChain(
    override val ruleId: Int?,
    override val status: Status,
    override val messageForUser: String,
    override val messageForSender: String,
    override val juridiskHenvisning: JuridiskHenvisning?,
    override val predicate: (RuleData<Boolean>) -> Boolean
) : Rule<RuleData<Boolean>> {
    @Description("Behandler er suspendert av NAV på konsultasjonstidspunkt")
    BEHANDLER_SUSPENDERT(
        1414,
        Status.INVALID,
        "Den som sykmeldte deg har mistet retten til å skrive sykmeldinger.",
        "Behandler er suspendert av NAV på konsultasjonstidspunkt. Pasienten har fått beskjed.",
        null,
        { (_, suspended) ->
            suspended
        }
    ),
}
