package no.nav.syfo.rules.validation

import no.nav.syfo.model.Status
import no.nav.syfo.rules.common.RuleHit

enum class ValidationRuleHit(
    val ruleHit: RuleHit
) {
    PASIENT_YNGRE_ENN_13(
        ruleHit = RuleHit(
            rule = "PASIENT_YNGRE_ENN_13",
            status = Status.INVALID,
            messageForSender = "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
            messageForUser = "Pasienten er under 13 år. Sykmelding kan ikke benyttes."
        )
    ),
    UGYLDIG_REGELSETTVERSJON(
        ruleHit = RuleHit(
            rule = "UGYLDIG_REGELSETTVERSJON",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                "Feil regelsett er brukt i sykmeldingen.",
            messageForUser = "Det er brukt en versjon av sykmeldingen som ikke lenger er gyldig."
        )
    ),
    MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39(
        ruleHit = RuleHit(
            rule = "MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny." +
                " Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                "Utdypende opplysninger som kreves ved uke 39 mangler. ",
            messageForUser = "Sykmeldingen mangler utdypende opplysninger som kreves når " +
                "sykefraværet er lengre enn 39 uker til sammen."
        )
    ),
    UGYLDIG_ORGNR_LENGDE(
        ruleHit = RuleHit(
            rule = "UGYLDIG_ORGNR_LENGDE",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                "Feil format på organisasjonsnummer. Dette skal være 9 sifre.",
            messageForUser = "Den må ha riktig organisasjonsnummer."
        )
    ),
    AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(
        ruleHit = RuleHit(
            rule = "AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, " +
                "Pasienten har fått beskjed, den ble avvist grunnet følgende:" +
                "Avsender fnr er det samme som pasient fnr",
            messageForUser = "Den som signert sykmeldingen er også pasient."
        )
    ),
    BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR(
        ruleHit = RuleHit(
            rule = "BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes." +
                " Pasienten har fått beskjed, den ble avvist grunnet følgende:" +
                "Behandler fnr er det samme som pasient fnr",
            messageForUser = "Den som er behandler av sykmeldingen er også pasient."
        )
    )
}
