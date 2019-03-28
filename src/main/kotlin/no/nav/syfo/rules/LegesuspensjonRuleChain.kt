package no.nav.syfo.rules

import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.model.Status

enum class LegesuspensjonRuleChain(
    override val ruleId: Int?,
    override val status: Status,
    override val textToUser: String,
    override val textToTreater: String,
    override val predicate: (RuleData<Boolean>) -> Boolean
) : Rule<RuleData<Boolean>> {
    // 1414: Hvis behandler har autorisasjonskode Suspendert av NAV  sendes meldingen til manuell behandling.
    // Kommentar fra Camilla: Denne bør vel heller avvises? Pasienten må få beskjed: Sykmeldingen kan ikke godtas. Oppfølgingsoppgave til NAV Kontroll?
    @Description("Behandler er suspendert av NAV på konsultasjonstidspunkt")
    BEHANDLER_SUSPENDED(
            1414,
            Status.INVALID,
            "Behandler er suspendert av NAV på konsultasjonstidspunkt",
            "Behandler er suspendert av NAV på konsultasjonstidspunkt", { (_, suspended) ->
        suspended
    }),
}
