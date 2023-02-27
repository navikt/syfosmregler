package no.nav.syfo.rules.gradert

import no.nav.syfo.model.Status
import no.nav.syfo.rules.common.RuleHit

enum class GradertRuleHit(
    val ruleHit: RuleHit
) {
    GRADERT_SYKMELDING_UNDER_20_PROSENT(
        ruleHit = RuleHit(
            rule = "GRADERT_SYKMELDING_UNDER_20_PROSENT",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                "Hvis sykmeldingsgrad er mindre enn 20% for gradert sykmelding, avvises meldingen",
            messageForUser = "Sykmeldingsgraden kan ikke være mindre enn 20 %."
        )
    )
}
