package no.nav.syfo.rules.periode

import no.nav.syfo.model.Status
import no.nav.syfo.rules.common.RuleHit

enum class PeriodeRuleHit(val ruleHit: RuleHit) {
    FREMDATERT_MER_ENN_30_DAGER(
        ruleHit =
            RuleHit(
                rule = "FREMDATERT",
                status = Status.INVALID,
                messageForSender =
                    "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                        "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Hvis sykmeldingen er fremdatert mer enn 30 dager etter behandletDato avvises meldingen.",
                messageForUser = "Sykmeldingen er datert mer enn 30 dager fram i tid.",
            ),
    ),
    TILBAKEDATERT_MER_ENN_3_AR(
        ruleHit =
            RuleHit(
                rule = "TILBAKEDATERT_MER_ENN_3_AR",
                status = Status.INVALID,
                messageForSender =
                    "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                        "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende: " +
                        "Sykmeldinges fom-dato er mer enn 3 år tilbake i tid.",
                messageForUser = "Startdatoen er mer enn tre år tilbake.",
            ),
    ),
    TOTAL_VARIGHET_OVER_ETT_AAR(
        ruleHit =
            RuleHit(
                rule = "TOTAL_VARIGHET_OVER_ETT_AAR",
                status = Status.INVALID,
                messageForSender =
                    "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                        "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Sykmeldingen første fom og siste tom har ein varighet som er over 1 år",
                messageForUser = "Den kan ikke ha en varighet på over ett år.",
            ),
    ),
}
