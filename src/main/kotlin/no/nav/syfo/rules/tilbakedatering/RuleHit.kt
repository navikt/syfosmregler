package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.Status
import no.nav.syfo.model.juridisk.JuridiskHenvisning
import no.nav.syfo.model.juridisk.Lovverk
import no.nav.syfo.rules.common.RuleHit

private fun getJuridiskHenvisning(): JuridiskHenvisning {
    return JuridiskHenvisning(
        lovverk = Lovverk.FOLKETRYGDLOVEN,
        paragraf = "8-7",
        ledd = 2,
        punktum = null,
        bokstav = null
    )
}

enum class TilbakedateringRuleHit(
    val ruleHit: RuleHit
) {
    INNTIL_8_DAGER(
        ruleHit = RuleHit(
            rule = INNTIL_8_DAGER.name,
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Første sykmelding er tilbakedatert uten at begrunnelse (felt 11.2) er tilstrekkelig utfylt",
            messageForUser = "Sykmeldingen er tilbakedatert uten tilstrekkelig begrunnelse fra den som sykmeldte deg.",
            juridiskHenvisning = getJuridiskHenvisning()
        )
    ),
    INNTIL_30_DAGER(
        ruleHit = RuleHit(
            rule = INNTIL_30_DAGER.name,
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Sykmelding er tilbakedatert uten begrunnelse (felt 11.2) er tilstrekkelig utfylt",
            messageForUser = "Sykmeldingen er tilbakedatert uten tilstrekkelig begrunnelse fra den som sykmeldte deg.",
            juridiskHenvisning = getJuridiskHenvisning()
        )
    ),
    INNTIL_30_DAGER_MED_BEGRUNNELSE(
        ruleHit = RuleHit(
            rule = INNTIL_30_DAGER_MED_BEGRUNNELSE.name,
            status = Status.MANUAL_PROCESSING,
            messageForSender = "Første sykmelding er tilbakedatert og felt 11.2 (begrunnelse) er utfylt",
            messageForUser = "Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert",
            juridiskHenvisning = getJuridiskHenvisning()
        )
    ),
    OVER_30_DAGER(
        ruleHit = RuleHit(
            rule = OVER_30_DAGER.name,
            status = Status.INVALID,
            messageForSender = "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. " +
                    "Grunnet følgende: Sykmelding er tilbakedatert mer enn det som er tillatt og felt 11.2 er ikke utfylt.",
            messageForUser = "Sykmeldingen er tilbakedatert uten tilstrekkelig begrunnelse fra den som sykmeldte deg.",
            juridiskHenvisning = getJuridiskHenvisning()
        )
    ),
    OVER_30_DAGER_MED_BEGRUNNELSE(
        ruleHit = RuleHit(
            rule = OVER_30_DAGER_MED_BEGRUNNELSE.name,
            status = Status.MANUAL_PROCESSING,
            messageForSender = "Sykmeldingen er tilbakedatert og felt 11.2 (begrunnelse) er utfylt",
            messageForUser = "Sykmeldingen blir manuell behandlet fordi den er tilbakedatert",
            juridiskHenvisning = getJuridiskHenvisning()
        )
    ),
    OVER_30_DAGER_SPESIALISTHELSETJENESTEN(
        ruleHit = RuleHit(
            rule = OVER_30_DAGER_SPESIALISTHELSETJENESTEN.name,
            status = Status.MANUAL_PROCESSING,
            messageForSender = "Sykmeldingen er tilbakedatert over 30 dager og er fra spesialisthelsetjenesten",
            messageForUser = "Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert",
            juridiskHenvisning = null
        )
    )
}