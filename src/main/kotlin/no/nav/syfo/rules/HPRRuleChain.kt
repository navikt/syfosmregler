package no.nav.syfo.rules

import no.nav.syfo.client.Behandler
import no.nav.syfo.model.Status
import java.time.LocalDate

enum class HPRRuleChain(
    override val ruleId: Int?,
    override val status: Status,
    override val messageForUser: String,
    override val messageForSender: String,
    override val predicate: (RuleData<BehandlerOgStartdato>) -> Boolean
) : Rule<RuleData<BehandlerOgStartdato>> {
    @Description("Behandler er ikke gyldig i HPR på konsultasjonstidspunkt")
    BEHANDLER_IKKE_GYLDIG_I_HPR(
        1402,
        Status.INVALID,
        "Den som skrev sykmeldingen manglet autorisasjon.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Behandler er ikke gyldig i HPR på konsultasjonstidspunkt",
        { (_, behandlerOgStartdato) ->
            !behandlerOgStartdato.behandler.godkjenninger.any {
                it.autorisasjon?.aktiv != null && it.autorisasjon.aktiv
            }
        }
    ),

    @Description("Behandler har ikke gyldig autorisasjon i HPR")
    BEHANDLER_MANGLER_AUTORISASJON_I_HPR(
        1403,
        Status.INVALID,
        "Den som skrev sykmeldingen manglet autorisasjon.",
        "Behandler har ikke til gyldig autorisasjon i HPR",
        { (_, behandlerOgStartdato) ->
            !behandlerOgStartdato.behandler.godkjenninger.any {
                it.autorisasjon?.aktiv != null &&
                    it.autorisasjon.aktiv &&
                    it.autorisasjon.oid == 7704 &&
                    it.autorisasjon.verdi != null &&
                    it.autorisasjon.verdi in arrayOf("1", "17", "4", "2", "14", "18")
            }
        }
    ),

    @Description("Behandler finnes i HPR men er ikke lege, kiropraktor, manuellterapeut eller tannlege")
    BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR(
        1407,
        Status.INVALID,
        "Den som skrev sykmeldingen manglet autorisasjon.",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Behandler finnes i HPR men er ikke lege, kiropraktor, fysioterapeut, manuellterapeut eller tannlege",
        { (_, behandlerOgStartdato) ->
            !behandlerOgStartdato.behandler.godkjenninger.any {
                it.helsepersonellkategori?.aktiv != null &&
                    it.autorisasjon?.aktiv == true && it.helsepersonellkategori.verdi != null &&
                    harAktivHelsepersonellAutorisasjonsSom(
                        behandlerOgStartdato.behandler,
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

    @Description("Behandler er manuellterapeuter, kiropraktorer og fysioterapeuter kan skrive sykmeldinger inntil 12 uker varighet")
    BEHANDLER_MT_FT_KI_OVER_12_UKER(
        1519,
        Status.INVALID,
        "Sykmeldingen din er avvist fordi den som sykmeldte deg ikke kan skrive en sykmelding som gjør at sykefraværet ditt overstiger 12 uker",
        "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
            "Sykmeldingen er avvist fordi det totale sykefraværet overstiger 12 uker (du som KI/MT/FT kan ikke sykmelde utover 12 uker)",
        { (sykmelding, behandlerOgStartdato) ->
            (
                (sykmelding.perioder.sortedFOMDate().first()..sykmelding.perioder.sortedTOMDate().last()).daysBetween() > 84 ||
                    (behandlerOgStartdato.startdato != null && (behandlerOgStartdato.startdato..sykmelding.perioder.sortedTOMDate().last()).daysBetween() > 84)
                ) &&
                !harAktivHelsepersonellAutorisasjonsSom(
                    behandlerOgStartdato.behandler,
                    listOf(
                        HelsepersonellKategori.LEGE.verdi,
                        HelsepersonellKategori.TANNLEGE.verdi
                    )
                ) &&
                harAktivHelsepersonellAutorisasjonsSom(
                    behandlerOgStartdato.behandler,
                    listOf(
                        HelsepersonellKategori.KIROPRAKTOR.verdi,
                        HelsepersonellKategori.MANUELLTERAPEUT.verdi,
                        HelsepersonellKategori.FYSIOTERAPAEUT.verdi
                    )
                )
        }
    ),
}

fun harAktivHelsepersonellAutorisasjonsSom(behandler: Behandler, helsepersonerVerdi: List<String>): Boolean =
    behandler.godkjenninger.any { godkjenning ->
        godkjenning.helsepersonellkategori?.aktiv != null &&
            godkjenning.autorisasjon?.aktiv == true && godkjenning.helsepersonellkategori.verdi != null &&
            godkjenning.helsepersonellkategori.let {
                it.aktiv && it.verdi in helsepersonerVerdi
            }
    }

data class BehandlerOgStartdato(
    val behandler: Behandler,
    val startdato: LocalDate?
)
