package no.nav.syfo.rules

import no.nav.syfo.client.Behandler
import no.nav.syfo.client.Godkjenning
import no.nav.syfo.model.Rule
import no.nav.syfo.model.RuleChain
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import java.time.LocalDate

class HPRRuleChain(
    private val sykmelding: Sykmelding,
    private val behandlerOgStartdato: BehandlerOgStartdato,
) : RuleChain {
    override val rules: List<Rule<*>> = listOf(
        // Behandler er ikke gyldig i HPR på konsultasjonstidspunkt
        Rule(
            name = "BEHANDLER_IKKE_GYLDIG_I_HPR",
            ruleId = 1402,
            status = Status.INVALID,
            messageForSender = "Den som skrev sykmeldingen manglet autorisasjon.",
            messageForUser = "Behandler er ikke gyldig i HPR på konsultasjonstidspunkt. Pasienten har fått beskjed.",
            juridiskHenvisning = null,
            input = object {
                val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger
            },
            predicate = { input ->
                !input.behandlerGodkjenninger.any {
                    it.autorisasjon?.aktiv != null && it.autorisasjon.aktiv
                }
            }
        ),
        // "Behandler har ikke gyldig autorisasjon i HPR"
        Rule(
            name = "BEHANDLER_MANGLER_AUTORISASJON_I_HPR",
            ruleId = 1403,
            status = Status.INVALID,
            messageForSender = "Den som skrev sykmeldingen manglet autorisasjon.",
            messageForUser = "Behandler har ikke gyldig autorisasjon i HPR. Pasienten har fått beskjed.",
            juridiskHenvisning = null,
            input = object {
                val behandlerGodjenninger = behandlerOgStartdato.behandler.godkjenninger
            },
            predicate = { input ->
                !input.behandlerGodjenninger.any {
                    it.autorisasjon?.aktiv != null &&
                        it.autorisasjon.aktiv &&
                        it.autorisasjon.oid == 7704 &&
                        it.autorisasjon.verdi != null &&
                        it.autorisasjon.verdi in arrayOf("1", "17", "4", "2", "14", "18")
                }
            }
        ),
        // "Behandler finnes i HPR men er ikke lege, kiropraktor, manuellterapeut eller tannlege"
        Rule(
            name = "BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR",
            ruleId = 1407,
            status = Status.INVALID,
            messageForSender = "Den som skrev sykmeldingen manglet autorisasjon.",
            messageForUser = "Behandler finnes i HPR men er ikke lege, kiropraktor, fysioterapeut, manuellterapeut eller tannlege. Pasienten har fått beskjed.",
            juridiskHenvisning = null,
            input = object {
                val behandlerGodjenninger = behandlerOgStartdato.behandler.godkjenninger
            },
            predicate = { input ->
                !input.behandlerGodjenninger.any {
                    it.helsepersonellkategori?.aktiv != null &&
                        it.autorisasjon?.aktiv == true && it.helsepersonellkategori.verdi != null &&
                        harAktivHelsepersonellAutorisasjonsSom(
                            input.behandlerGodjenninger,
                            listOf(
                                HelsepersonellKategori.LEGE.verdi,
                                HelsepersonellKategori.KIROPRAKTOR.verdi,
                                HelsepersonellKategori.MANUELLTERAPEUT.verdi,
                                HelsepersonellKategori.TANNLEGE.verdi,
                                HelsepersonellKategori.FYSIOTERAPAEUT.verdi
                            )
                        )
                }
            }
        ),

        // Behandler er manuellterapeuter, kiropraktorer og fysioterapeuter kan skrive sykmeldinger inntil 12 uker varighet
        Rule(
            name = "BEHANDLER_MT_FT_KI_OVER_12_UKER",
            ruleId = 1519,
            status = Status.INVALID,
            messageForSender = "Sykmeldingen din er avvist fordi den som sykmeldte deg ikke kan skrive en sykmelding som gjør at sykefraværet ditt overstiger 12 uker",
            messageForUser = "Sykmeldingen er avvist fordi det totale sykefraværet overstiger 12 uker (du som KI/MT/FT kan ikke sykmelde utover 12 uker). Pasienten har fått beskjed.",
            juridiskHenvisning = null,
            input = object {
                val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
                val sisteTomDato = sykmelding.perioder.sortedTOMDate().last()
                val behandlerStartDato = behandlerOgStartdato.startdato
                val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger
            },
            predicate = {
                (
                    (it.forsteFomDato..it.sisteTomDato).daysBetween() > 84 ||
                        (it.behandlerStartDato != null && (it.behandlerStartDato..it.sisteTomDato).daysBetween() > 84)
                    ) &&
                    !harAktivHelsepersonellAutorisasjonsSom(
                        it.behandlerGodkjenninger,
                        listOf(
                            HelsepersonellKategori.LEGE.verdi,
                            HelsepersonellKategori.TANNLEGE.verdi
                        )
                    ) &&
                    harAktivHelsepersonellAutorisasjonsSom(
                        it.behandlerGodkjenninger,
                        listOf(
                            HelsepersonellKategori.KIROPRAKTOR.verdi,
                            HelsepersonellKategori.MANUELLTERAPEUT.verdi,
                            HelsepersonellKategori.FYSIOTERAPAEUT.verdi
                        )
                    )
            }
        ),
    )
}

fun harAktivHelsepersonellAutorisasjonsSom(
    behandlerGodkjenninger: List<Godkjenning>,
    helsepersonerVerdi: List<String>,
): Boolean =
    behandlerGodkjenninger.any { godkjenning ->
        godkjenning.helsepersonellkategori?.aktiv != null &&
            godkjenning.autorisasjon?.aktiv == true && godkjenning.helsepersonellkategori.verdi != null &&
            godkjenning.helsepersonellkategori.let {
                it.aktiv && it.verdi in helsepersonerVerdi
            }
    }

data class BehandlerOgStartdato(
    val behandler: Behandler,
    val startdato: LocalDate?,
)
