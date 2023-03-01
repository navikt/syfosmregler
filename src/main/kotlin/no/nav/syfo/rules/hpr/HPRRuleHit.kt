package no.nav.syfo.rules.hpr

import no.nav.syfo.model.Status
import no.nav.syfo.rules.common.RuleHit

enum class HPRRuleHit(
    val ruleHit: RuleHit
) {
    BEHANDLER_IKKE_GYLDIG_I_HPR(
        ruleHit = RuleHit(
            rule = "BEHANDLER_IKKE_GYLDIG_I_HPR",
            status = Status.INVALID,
            messageForSender = "Behandler er ikke gyldig i HPR på konsultasjonstidspunkt. Pasienten har fått beskjed.",
            messageForUser = "Den som skrev sykmeldingen manglet autorisasjon.",
        )
    ),
    BEHANDLER_MANGLER_AUTORISASJON_I_HPR(
        ruleHit = RuleHit(
            rule = "BEHANDLER_MANGLER_AUTORISASJON_I_HPR",
            status = Status.INVALID,
            messageForSender = "Behandler har ikke gyldig autorisasjon i HPR. Pasienten har fått beskjed.",
            messageForUser = "Den som skrev sykmeldingen manglet autorisasjon.",
        )
    ),
    BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR(
        ruleHit = RuleHit(
            rule = "BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR",
            status = Status.INVALID,
            messageForSender = "Behandler finnes i HPR, men er ikke lege, kiropraktor, fysioterapeut, " +
                "manuellterapeut eller tannlege. Pasienten har fått beskjed.",
            messageForUser = "Den som skrev sykmeldingen manglet autorisasjon.",
        )
    ),
    BEHANDLER_MT_FT_KI_OVER_12_UKER(
        ruleHit = RuleHit(
            rule = "BEHANDLER_MT_FT_KI_OVER_12_UKER",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen er avvist fordi det totale sykefraværet overstiger 12 uker (du som KI/MT/FT " +
                "kan ikke sykmelde utover 12 uker). Pasienten har fått beskjed.",
            messageForUser = "SSykmeldingen din er avvist fordi den som sykmeldte deg ikke kan skrive en sykmelding som " +
                "gjør at sykefraværet ditt overstiger 12 uker",
        )
    )
}
