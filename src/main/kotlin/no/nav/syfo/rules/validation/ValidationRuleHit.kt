package no.nav.syfo.rules.validation

import no.nav.syfo.model.Status
import no.nav.syfo.model.juridisk.JuridiskHenvisning
import no.nav.syfo.model.juridisk.Lovverk
import no.nav.syfo.rules.common.RuleHit

enum class ValidationRuleHit(
    val ruleHit: RuleHit
) {
    PASIENT_YNGRE_ENN_13(
        ruleHit = RuleHit(
            rule = "PASIENT_YNGRE_ENN_13",
            status = Status.INVALID,
            messageForSender = "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
            messageForUser = "Pasienten er under 13 år. Sykmelding kan ikke benyttes.",
            juridiskHenvisning = null
        )
    ),
    PASIENT_ELDRE_ENN_70(
        ruleHit = RuleHit(
            rule = "PASIENT_ELDRE_ENN_70",
            status = Status.INVALID,
            messageForSender = "Pasienten er over 70 år. Sykmelding kan ikke benyttes. Pasienten har fått beskjed.",
            messageForUser = "Sykmelding kan ikke benyttes etter at du har fylt 70 år",
            juridiskHenvisning = JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-3",
                ledd = 1,
                punktum = 2,
                bokstav = null
            )
        )
    ),
    UKJENT_DIAGNOSEKODETYPE(
        ruleHit = RuleHit(
            rule = "UKJENT_DIAGNOSEKODETYPE",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                "Ukjent kodeverk er benyttet for diagnosen.",
            messageForUser = "Sykmeldingen må ha et kjent kodeverk for diagnosen.",
            juridiskHenvisning = JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-4",
                ledd = 1,
                punktum = 1,
                bokstav = null
            )
        )
    ),
    ICPC_2_Z_DIAGNOSE(
        ruleHit = RuleHit(
            rule = "ICPC_2_Z_DIAGNOSE",
            status = Status.INVALID,
            messageForSender = "Angitt hoveddiagnose (z-diagnose) gir ikke rett til sykepenger. Pasienten har fått beskjed.",
            messageForUser = "Den må ha en gyldig diagnosekode som gir rett til sykepenger.",
            juridiskHenvisning = JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-4",
                ledd = 1,
                punktum = 2,
                bokstav = null
            )
        )
    ),
    HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER(
        ruleHit = RuleHit(
            rule = "HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                "Hoveddiagnose eller annen lovfestet fraværsgrunn mangler. ",
            messageForUser = "Den må ha en hoveddiagnose eller en annen gyldig fraværsgrunn.",
            juridiskHenvisning = JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-4",
                ledd = 1,
                punktum = 1,
                bokstav = null
            )
        )
    ),
    UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE(
        ruleHit = RuleHit(
            rule = "UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                "Kodeverk for hoveddiagnose er feil eller mangler. Prosesskoder ikke kan benyttes for å angi diagnose.",
            messageForUser = "Den må ha riktig kode for hoveddiagnose.",
            juridiskHenvisning = JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-4",
                ledd = 1,
                punktum = 1,
                bokstav = null
            )
        )
    ),
    UGYLDIG_KODEVERK_FOR_BIDIAGNOSE(
        ruleHit = RuleHit(
            rule = "UGYLDIG_KODEVERK_FOR_BIDIAGNOSE",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                "Kodeverk for bidiagnose er ikke angitt eller korrekt. " +
                "Prosesskoder ikke kan benyttes for å angi diagnose.",
            messageForUser = "Det er brukt eit ukjent kodeverk for bidiagnosen.",
            juridiskHenvisning = JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-4",
                ledd = 1,
                punktum = 1,
                bokstav = null
            )
        )
    ),
    UGYLDIG_REGELSETTVERSJON(
        ruleHit = RuleHit(
            rule = "UGYLDIG_REGELSETTVERSJON",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                "Feil regelsett er brukt i sykmeldingen.",
            messageForUser = "Det er brukt en versjon av sykmeldingen som ikke lenger er gyldig.",
            juridiskHenvisning = null
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
                "sykefraværet er lengre enn 39 uker til sammen.",
            juridiskHenvisning = null
        )
    ),
    UGYLDIG_ORGNR_LENGDE(
        ruleHit = RuleHit(
            rule = "UGYLDIG_ORGNR_LENGDE",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                "Feil format på organisasjonsnummer. Dette skal være 9 sifre.",
            messageForUser = "Den må ha riktig organisasjonsnummer.",
            juridiskHenvisning = null
        )
    ),
    AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(
        ruleHit = RuleHit(
            rule = "AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, " +
                "Pasienten har fått beskjed, den ble avvist grunnet følgende:" +
                "Avsender fnr er det samme som pasient fnr",
            messageForUser = "Den som signert sykmeldingen er også pasient.",
            juridiskHenvisning = null
        )
    ),
    BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR(
        ruleHit = RuleHit(
            rule = "BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR",
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes." +
                " Pasienten har fått beskjed, den ble avvist grunnet følgende:" +
                "Behandler fnr er det samme som pasient fnr",
            messageForUser = "Den som er behandler av sykmeldingen er også pasient.",
            juridiskHenvisning = null
        )
    )
}
