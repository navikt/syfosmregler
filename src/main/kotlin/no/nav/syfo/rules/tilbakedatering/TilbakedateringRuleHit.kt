package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.Status
import no.nav.syfo.rules.common.RuleHit

enum class TilbakedateringRuleHit(
    val ruleHit: RuleHit,
) {
    INNTIL_8_DAGER(
        ruleHit =
            RuleHit(
                rule = "INNTIL_8_DAGER",
                status = Status.INVALID,
                messageForSender =
                    "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Første sykmelding er tilbakedatert uten at begrunnelse (felt 11.2) er tilstrekkelig utfylt",
                messageForUser =
                    "Sykmeldingen er tilbakedatert uten tilstrekkelig begrunnelse fra den som sykmeldte deg.",
            ),
    ),
    INNTIL_1_MAANDE(
        ruleHit =
            RuleHit(
                rule = "INNTIL_1_MAANDE",
                status = Status.INVALID,
                messageForSender =
                    "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                        "Sykmelding er tilbakedatert uten begrunnelse (felt 11.2) er tilstrekkelig utfylt",
                messageForUser =
                    "Sykmeldingen er tilbakedatert uten tilstrekkelig begrunnelse fra den som sykmeldte deg.",
            ),
    ),
    INNTIL_1_MAANDE_MED_BEGRUNNELSE(
        ruleHit =
            RuleHit(
                rule = "INNTIL_1_MAANDE_MED_BEGRUNNELSE",
                status = Status.MANUAL_PROCESSING,
                messageForSender =
                    "Første sykmelding er tilbakedatert og felt 11.2 (begrunnelse) er utfylt",
                messageForUser = "Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert",
            ),
    ),
    OVER_1_MND(
        ruleHit =
            RuleHit(
                rule = "OVER_1_MND",
                status = Status.INVALID,
                messageForSender =
                    "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. " +
                        "Grunnet følgende: Sykmelding er tilbakedatert mer enn det som er tillatt og felt 11.2 (begrunnelse) er utfylt uten tilstrekkelig begrunnelse",
                messageForUser =
                    "Sykmeldingen er tilbakedatert uten tilstrekkelig begrunnelse fra den som sykmeldte deg.",
            ),
    ),
    OVER_1_DAGER_MED_BEGRUNNELSE(
        ruleHit =
            RuleHit(
                rule = "OVER_1_DAGER_MED_BEGRUNNELSE",
                status = Status.MANUAL_PROCESSING,
                messageForSender =
                    "Sykmeldingen er tilbakedatert og felt 11.2 (begrunnelse) er utfylt",
                messageForUser = "Sykmeldingen blir manuell behandlet fordi den er tilbakedatert",
            ),
    ),
    OVER_1_MND_SPESIALISTHELSETJENESTEN(
        ruleHit =
            RuleHit(
                rule = "OVER_1_MND_SPESIALISTHELSETJENESTEN",
                status = Status.MANUAL_PROCESSING,
                messageForSender =
                    "Sykmeldingen er tilbakedatert over 1 månede og er fra spesialisthelsetjenesten",
                messageForUser = "Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert",
            ),
    ),
}
