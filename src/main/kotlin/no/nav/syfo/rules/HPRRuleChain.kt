package no.nav.syfo.rules

import no.nav.syfo.model.Status
import no.nav.syfo.sm.toICPC2
import no.nhn.schemas.reg.hprv2.Person as HPRPerson

enum class HPRRuleChain(
    override val ruleId: Int?,
    override val status: Status,
    override val messageForUser: String,
    override val messageForSender: String,
    override val predicate: (RuleData<HPRPerson>) -> Boolean
) : Rule<RuleData<HPRPerson>> {
    @Description("Hvis manuellterapeut/kiropraktor eller fysioterapeut med autorisasjon har angitt annen diagnose enn kapitel L (muskel og skjelettsykdommer)skal meldingen til manuell behandling")
    BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(
            1143,
            Status.MANUAL_PROCESSING,
            "Den som har skrevet sykmeldingen din har ikke autorisasjon til dette.",
            "Behandler er manuellterapeut/kiropraktor eller fysioterapeut med autorisasjon har angitt annen diagnose enn kapitel L (muskel og skjelettsykdommer)",
            { (healthInformation, doctor) ->
                healthInformation.medisinskVurdering.hovedDiagnose?.toICPC2()?.firstOrNull()?.code?.startsWith("L") == false &&
                doctor.godkjenninger?.godkjenning != null &&
                doctor.godkjenninger.godkjenning.any {
                    it?.helsepersonellkategori?.isAktiv != null &&
                            it.autorisasjon?.isAktiv == true &&
                            it.helsepersonellkategori.isAktiv != null &&
                            it.helsepersonellkategori.verdi != null &&
                            it.helsepersonellkategori.let { it.isAktiv && it.verdi in listOf("KI", "MT", "FT") }
                } && !doctor.godkjenninger.godkjenning.any {
                            it?.helsepersonellkategori?.isAktiv != null &&
                                    it.autorisasjon?.isAktiv == true &&
                                    it.helsepersonellkategori.isAktiv != null &&
                                    it.helsepersonellkategori.verdi != null &&
                                    it.helsepersonellkategori.let { it.isAktiv && it.verdi in listOf("LE", "TL") }
                        }
    }),

    @Description("Behandler er ikke gyldig i HPR på konsultasjonstidspunkt")
    BEHANDLER_IKKE_GYLDIG_I_HPR(
            1402,
            Status.INVALID,
            "Den som skrev sykmeldingen manglet autorisasjon.",
            "Behandler er ikke gyldig i HPR på konsultasjonstidspunkt", { (_, doctor) ->
        doctor.godkjenninger?.godkjenning != null && !doctor.godkjenninger.godkjenning.any {
            it?.autorisasjon?.isAktiv != null && it.autorisasjon.isAktiv
        }
    }),

    @Description("Behandler har ikke gyldig autorisasjon i HPR")
    BEHANDLER_MANGLER_AUTORISASJON_I_HPR(
            1403,
            Status.INVALID,
            "Den som skrev sykmeldingen manglet autorisasjon.",
            "Behandler har ikke gyldig autorisasjon i HPR", { (_, doctor) ->
        doctor.godkjenninger?.godkjenning != null && !doctor.godkjenninger.godkjenning.any {
            it?.autorisasjon?.isAktiv != null &&
            it.autorisasjon.isAktiv &&
                    it.autorisasjon?.oid != null
                    it.autorisasjon.oid == 7704 &&
                    it.autorisasjon?.verdi != null &&
                    it.autorisasjon.verdi in arrayOf("1", "17", "4", "3", "2", "14", "18")
        }
    }),

    @Description("Behandler finnes i HPR men er ikke lege, kiropraktor, manuellterapeut, fysioterapeut eller tannlege")
    BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR(
            1407,
            Status.INVALID,
            "Den som skrev sykmeldingen manglet autorisasjon.",
            "Behandler finnes i HPR men er ikke lege, kiropraktor, manuellterapeut, fysioterapeut eller tannlege", { (_, doctor) ->
        doctor.godkjenninger?.godkjenning != null &&
                !doctor.godkjenninger.godkjenning.any {
                    it?.helsepersonellkategori?.isAktiv != null &&
                    it.autorisasjon?.isAktiv == true &&
                    it.helsepersonellkategori.isAktiv != null &&
                    it.helsepersonellkategori.verdi != null &&
                    it.helsepersonellkategori.let { it.isAktiv && it.verdi in listOf("LE", "KI", "MT", "TL", "FT") }
        }
    }),

    @Description("Hvis en sykmelding fra manuellterapeut/kiropraktor eller fysioterapeut overstiger 12 uker regnet fra første sykefraværsdag skal meldingen avvises")
    BEHANDLER_MT_FT_KI_OVER_12_UKER(
            1519,
            Status.INVALID,
            "Den som skrev sykmeldingen mangler autorisasjon.",
            "Behandler er manuellterapeut/kiropraktor eller fysioterapeut overstiger 12 uker regnet fra første sykefraværsdag", { (healthInformation, doctor) ->
        healthInformation.perioder.any { (it.fom..it.tom).daysBetween() > 84 } &&
                doctor.godkjenninger?.godkjenning != null &&
                doctor.godkjenninger.godkjenning.any {
                            it?.helsepersonellkategori?.isAktiv != null &&
                            it.autorisasjon?.isAktiv == true &&
                            it.helsepersonellkategori.isAktiv != null &&
                            it.helsepersonellkategori.verdi != null &&
                            it.helsepersonellkategori.let { it.isAktiv && it.verdi in listOf("KI", "MT", "FT") } &&
                            !it.helsepersonellkategori.let { it.isAktiv && it.verdi in listOf("LE", "TL") }
                } &&
                !doctor.godkjenninger.godkjenning.any {
                            it?.helsepersonellkategori?.isAktiv != null &&
                            it.autorisasjon?.isAktiv == true &&
                            it.helsepersonellkategori.isAktiv != null &&
                            it.helsepersonellkategori.verdi != null &&
                            it.helsepersonellkategori.let { it.isAktiv && it.verdi in listOf("LE", "TL") }
        }
    }),
}
