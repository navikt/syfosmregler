package no.nav.syfo.rules.patientageover70

import no.nav.syfo.model.Status
import no.nav.syfo.rules.common.RuleHit

enum class PatientAgeOver70RuleHit(
    val ruleHit: RuleHit
) {
    PASIENT_ELDRE_ENN_70(
        ruleHit = RuleHit(
            rule = "PASIENT_ELDRE_ENN_70",
            status = Status.INVALID,
            messageForSender = "Pasienten er over 70 år. Sykmelding kan ikke benyttes. Pasienten har fått beskjed.",
            messageForUser = "Sykmelding kan ikke benyttes etter at du har fylt 70 år"
        )
    )
}
