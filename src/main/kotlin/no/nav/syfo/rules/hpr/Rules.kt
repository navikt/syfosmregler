package no.nav.syfo.rules.hpr

import no.nav.syfo.client.Godkjenning
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.services.BehandlerOgStartdato
import no.nav.syfo.services.daysBetween
import no.nav.syfo.services.sortedFOMDate
import no.nav.syfo.services.sortedTOMDate

typealias Rule<T> = (sykmelding: Sykmelding, behandlerOgStartdato: BehandlerOgStartdato) -> RuleResult<T>
typealias HPRRule = Rule<HPRRules>

val behanderIkkeGyldigHPR: HPRRule = { _, behandlerOgStartdato ->
    val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger

    val aktivAutorisasjon = behandlerGodkjenninger.any {
        (it.autorisasjon?.aktiv != null && it.autorisasjon.aktiv)
    }

    RuleResult(
        ruleInputs = mapOf("behandlerGodkjenninger" to behandlerGodkjenninger),
        rule = HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR,
        ruleResult = !aktivAutorisasjon
    )
}

val behandlerManglerAutorisasjon: HPRRule = { _, behandlerOgStartdato ->
    val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger

    val gyldigeGodkjenninger = behandlerGodkjenninger.any {
        (
            it.autorisasjon?.aktiv != null &&
                it.autorisasjon.aktiv &&
                it.autorisasjon.oid == 7704 &&
                it.autorisasjon.verdi != null &&
                it.autorisasjon.verdi in arrayOf("1", "17", "4", "2", "14", "18")
            )
    }

    RuleResult(
        ruleInputs = mapOf("behandlerGodkjenninger" to behandlerGodkjenninger),
        rule = HPRRules.BEHANDLER_MANGLER_AUTORISASJON_I_HPR,
        ruleResult = !gyldigeGodkjenninger
    )
}

val behandlerIkkeLEKIMTTLFT: HPRRule = { _, behandlerOgStartdato ->
    val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger

    val behandlerLEKIMTTLFT = behandlerGodkjenninger.any {
        (
            it.helsepersonellkategori?.aktiv != null &&
                it.autorisasjon?.aktiv == true && it.helsepersonellkategori.verdi != null &&
                harAktivHelsepersonellAutorisasjonsSom(
                    behandlerGodkjenninger,
                    listOf(
                        HelsepersonellKategori.LEGE.verdi,
                        HelsepersonellKategori.KIROPRAKTOR.verdi,
                        HelsepersonellKategori.MANUELLTERAPEUT.verdi,
                        HelsepersonellKategori.TANNLEGE.verdi,
                        HelsepersonellKategori.FYSIOTERAPAEUT.verdi
                    )
                )
            )
    }

    RuleResult(
        ruleInputs = mapOf("behandlerGodkjenninger" to behandlerGodkjenninger),
        rule = HPRRules.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR,
        ruleResult = !behandlerLEKIMTTLFT
    )
}

val behandlerMTFTKISykmeldtOver12Uker: HPRRule = { sykmelding, behandlerOgStartdato ->
    val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
    val sisteTomDato = sykmelding.perioder.sortedTOMDate().last()
    val behandlerStartDato = behandlerOgStartdato.startdato
    val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger

    val behandlerMTFTKISykmeldtOver12Uker = (
        (forsteFomDato..sisteTomDato).daysBetween() > 84 ||
            (behandlerStartDato != null && (behandlerStartDato..sisteTomDato).daysBetween() > 84)
        ) &&
        !harAktivHelsepersonellAutorisasjonsSom(
            behandlerGodkjenninger,
            listOf(
                HelsepersonellKategori.LEGE.verdi,
                HelsepersonellKategori.TANNLEGE.verdi
            )
        ) &&
        harAktivHelsepersonellAutorisasjonsSom(
            behandlerGodkjenninger,
            listOf(
                HelsepersonellKategori.KIROPRAKTOR.verdi,
                HelsepersonellKategori.MANUELLTERAPEUT.verdi,
                HelsepersonellKategori.FYSIOTERAPAEUT.verdi
            )
        )

    RuleResult(
        ruleInputs = mapOf("behandlerGodkjenninger" to behandlerGodkjenninger),
        rule = HPRRules.BEHANDLER_MT_FT_KI_OVER_12_UKER,
        ruleResult = behandlerMTFTKISykmeldtOver12Uker
    )
}

private fun harAktivHelsepersonellAutorisasjonsSom(
    behandlerGodkjenninger: List<Godkjenning>,
    helsepersonerVerdi: List<String>
): Boolean =
    behandlerGodkjenninger.any { godkjenning ->
        godkjenning.helsepersonellkategori?.aktiv != null &&
            godkjenning.autorisasjon?.aktiv == true && godkjenning.helsepersonellkategori.verdi != null &&
            godkjenning.helsepersonellkategori.let {
                it.aktiv && it.verdi in helsepersonerVerdi
            }
    }
