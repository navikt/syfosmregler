package no.nav.syfo.rules

import no.nav.syfo.api.Behandler
import no.nav.syfo.model.Status
import no.nav.syfo.sm.toICPC2

enum class HPRRuleChain(
    override val ruleId: Int?,
    override val status: Status,
    override val messageForUser: String,
    override val messageForSender: String,
    override val predicate: (RuleData<Behandler>) -> Boolean
) : Rule<RuleData<Behandler>> {
    @Description("Hvis manuellterapeut/kiropraktor eller fysioterapeut med autorisasjon har angitt annen diagnose enn kapitel L (muskel og skjelettsykdommer)skal meldingen til manuell behandling")
    BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(
            1143,
            Status.MANUAL_PROCESSING,
            "Behandler er manuellterapeut/kiropraktor eller fysioterapeut med autorisasjon har angitt annen diagnose enn kapitel L (muskel og skjelettsykdommer)",
            "Behandler er manuellterapeut/kiropraktor eller fysioterapeut med autorisasjon har angitt annen diagnose enn kapitel L (muskel og skjelettsykdommer)",
            { (healthInformation, behandler) ->
                healthInformation.medisinskVurdering.hovedDiagnose?.toICPC2()?.firstOrNull()?.code?.startsWith("L") == false &&
                    !harAktivHelsepersonellAutorisasjonsSom(behandler, listOf(
                        HelsepersonellKategori.LEGE.verdi,
                        HelsepersonellKategori.TANNLEGE.verdi)) && harAktivHelsepersonellAutorisasjonsSom(behandler, listOf(
                    HelsepersonellKategori.KIROPRAKTOR.verdi,
                    HelsepersonellKategori.MANUELLTERAPEUT.verdi,
                    HelsepersonellKategori.FYSIOTERAPAEUT.verdi))
    }),

    @Description("Behandler er ikke gyldig i HPR på konsultasjonstidspunkt")
    BEHANDLER_IKKE_GYLDIG_I_HPR(
            1402,
            Status.INVALID,
            "Den som skrev sykmeldingen manglet autorisasjon.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Behandler er ikke gyldig i HPR på konsultasjonstidspunkt", { (_, behandler) ->
        !behandler.godkjenninger.any {
            it.autorisasjon?.aktiv != null && it.autorisasjon.aktiv
        }
    }),

    @Description("Behandler har ikke gyldig autorisasjon i HPR")
    BEHANDLER_MANGLER_AUTORISASJON_I_HPR(
            1403,
            Status.INVALID,
            "Den som skrev sykmeldingen manglet autorisasjon.",
            "Behandler har ikke til gyldig autorisasjon i HPR", { (_, behandler) ->
        !behandler.godkjenninger.any {
            it.autorisasjon?.aktiv != null &&
                    it.autorisasjon.aktiv &&
                    it.autorisasjon.oid == 7704 &&
                    it.autorisasjon.verdi != null &&
                    it.autorisasjon.verdi in arrayOf("1", "17", "4", "2", "14", "18")
        }
    }),

    @Description("Behandler finnes i HPR men er ikke lege, kiropraktor, manuellterapeut eller tannlege")
    BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR(
            1407,
            Status.INVALID,
            "Den som skrev sykmeldingen manglet autorisasjon.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Behandler finnes i HPR men er ikke lege, kiropraktor, fysioterapeut, manuellterapeut eller tannlege", { (_, behandler) ->
        !behandler.godkjenninger.any {
            it.helsepersonellkategori?.aktiv != null &&
                it.autorisasjon?.aktiv == true && it.helsepersonellkategori.verdi != null &&
                harAktivHelsepersonellAutorisasjonsSom(behandler, listOf(
                    HelsepersonellKategori.LEGE.verdi,
                    HelsepersonellKategori.KIROPRAKTOR.verdi,
                    HelsepersonellKategori.MANUELLTERAPEUT.verdi,
                    HelsepersonellKategori.TANNLEGE.verdi,
                    HelsepersonellKategori.FYSIOTERAPAEUT.verdi))
        }
    }),

    @Description("Hvis en sykmelding fra manuellterapeut/kiropraktor eller fysioterapeut overstiger 12 uker regnet fra første fom dato til siste tom skal meldingen avvises")
    BEHANDLER_MT_FT_KI_OVER_12_UKER(
            1519,
            Status.INVALID,
            "Den som skrev sykmeldingen mangler autorisasjon.",
            "Sykmeldingen kan ikke rettes, det må skrives en ny. Pasienten har fått beskjed om å vente på ny sykmelding fra deg. Grunnet følgende:" +
                    "Behandler er manuellterapeut/kiropraktor eller fysioterapeut overstiger 12 uker regnet fra første fom dato til siste tom dato", { (sykmelding, behandler) ->
        (sykmelding.perioder.sortedFOMDate().first()..sykmelding.perioder.sortedTOMDate().last()).daysBetween() > 84 &&
            !harAktivHelsepersonellAutorisasjonsSom(behandler, listOf(
                HelsepersonellKategori.LEGE.verdi,
                HelsepersonellKategori.TANNLEGE.verdi)) &&
            harAktivHelsepersonellAutorisasjonsSom(behandler, listOf(
                HelsepersonellKategori.KIROPRAKTOR.verdi,
                HelsepersonellKategori.MANUELLTERAPEUT.verdi,
                HelsepersonellKategori.FYSIOTERAPAEUT.verdi))
    }),
}

fun harAktivHelsepersonellAutorisasjonsSom(behandler: Behandler, helsepersonerVerdi: List<String>): Boolean =
    behandler.godkjenninger.any { godkjenning ->
        godkjenning.helsepersonellkategori?.aktiv != null &&
            godkjenning.autorisasjon?.aktiv == true && godkjenning.helsepersonellkategori.verdi != null &&
            godkjenning.helsepersonellkategori.let {
                it.aktiv && it.verdi in helsepersonerVerdi }
    }
