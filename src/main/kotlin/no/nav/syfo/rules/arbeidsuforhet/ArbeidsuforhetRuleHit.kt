package no.nav.syfo.rules.arbeidsuforhet

import no.nav.syfo.model.Status
import no.nav.syfo.model.juridisk.JuridiskHenvisning
import no.nav.syfo.model.juridisk.Lovverk
import no.nav.syfo.rules.common.RuleHit

private fun getJuridiskHenvisning(): JuridiskHenvisning =
    JuridiskHenvisning(
        lovverk = Lovverk.FOLKETRYGDLOVEN,
        paragraf = "8-4",
        ledd = 1,
        punktum = 1,
        bokstav = null,
    )

enum class ArbeidsuforhetRuleHit(
    val ruleHit: RuleHit,
) {
    UKJENT_DIAGNOSEKODETYPE(
        ruleHit =
            RuleHit(
                rule = "UKJENT_DIAGNOSEKODETYPE",
                status = Status.INVALID,
                messageForSender =
                    "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                        "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Ukjent kodeverk er benyttet for diagnosen.",
                messageForUser = "Sykmeldingen må ha et kjent kodeverk for diagnosen.",
            ),
    ),
    ICPC_2_Z_DIAGNOSE(
        ruleHit =
            RuleHit(
                rule = "ICPC_2_Z_DIAGNOSE",
                status = Status.INVALID,
                messageForSender =
                    "Angitt hoveddiagnose (z-diagnose) gir ikke rett til sykepenger. Pasienten har fått beskjed.",
                messageForUser = "Den må ha en gyldig diagnosekode som gir rett til sykepenger.",
            ),
    ),
    HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER(
        ruleHit =
            RuleHit(
                rule = "HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER",
                status = Status.INVALID,
                messageForSender =
                    "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                        "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Hoveddiagnose eller annen lovfestet fraværsgrunn mangler. ",
                messageForUser = "Den må ha en hoveddiagnose eller en annen gyldig fraværsgrunn.",
            ),
    ),
    UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE(
        ruleHit =
            RuleHit(
                rule = "UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE",
                status = Status.INVALID,
                messageForSender =
                    "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                        "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Kodeverk for hoveddiagnose er ikke angitt eller ukjent.",
                messageForUser = "Den må ha riktig kode for hoveddiagnose.",
            ),
    ),
    UGYLDIG_KODEVERK_FOR_BIDIAGNOSE(
        ruleHit =
            RuleHit(
                rule = "UGYLDIG_KODEVERK_FOR_BIDIAGNOSE",
                status = Status.INVALID,
                messageForSender =
                    "Sykmeldingen kan ikke rettes, det må skrives en ny. " +
                        "Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Kodeverk for bidiagnose er ikke angitt eller ukjent.",
                messageForUser = "Det er brukt eit ukjent kodeverk for bidiagnosen.",
            ),
    ),
}
