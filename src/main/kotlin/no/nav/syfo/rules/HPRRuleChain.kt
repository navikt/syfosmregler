package no.nav.syfo.rules

import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.model.Status
import no.nhn.schemas.reg.hprv2.Person as HPRPerson

enum class HPRRuleChain(
    override val ruleId: Int?,
    override val status: Status,
    override val textToUser: String,
    override val textToTreater: String,
    override val predicate: (RuleData<HPRPerson>) -> Boolean
) : Rule<RuleData<HPRPerson>> {
    @Description("Behandler er ikke gyldig i HPR på konsultasjonstidspunkt")
    BEHANDLER_NOT_VALDIG_IN_HPR(
            1402,
            Status.INVALID,
            "Behandler er ikke gyldig i HPR på konsultasjonstidspunkt",
            "Behandler er ikke gyldig i HPR på konsultasjonstidspunkt", { (_, doctor) ->
        doctor.godkjenninger?.godkjenning != null && !doctor.godkjenninger.godkjenning.any {
            it?.autorisasjon?.isAktiv != null && it.autorisasjon.isAktiv
        }
    }),

    @Description("Behandler har ikkje gylding autorisasjon i HPR")
    BEHANDLER_NOT_VALID_AUTHORIZATION_IN_HPR(
            1403,
            Status.INVALID,
            "Behandler har ikkje gylding autorisasjon i HPR",
            "Behandler har ikkje gylding autorisasjon i HPR", { (_, doctor) ->
        doctor.godkjenninger?.godkjenning != null && !doctor.godkjenninger.godkjenning.any {
            it?.autorisasjon?.isAktiv != null &&
            it.autorisasjon.isAktiv &&
                    it.autorisasjon?.oid != null
                    it.autorisasjon.oid == 7704 &&
                    it.autorisasjon?.verdi != null &&
                    it.autorisasjon.verdi in arrayOf("1", "17", "4", "3", "2", "14", "18")
        }
    }),

    @Description("Behandler finnes i HPR men er ikke lege, kiropraktor, manuellterapeut eller tannlege")
    BEHANDLER_NOT_LE_KI_MT_TL_IN_HPR(
            1407,
            Status.INVALID,
            "Behandler finnes i HPR men er ikke lege, kiropraktor, manuellterapeut eller tannlege",
            "Behandler finnes i HPR men er ikke lege, kiropraktor, manuellterapeut eller tannlege", { (_, doctor) ->
        doctor.godkjenninger?.godkjenning != null &&
                !doctor.godkjenninger.godkjenning.any {
                    it?.helsepersonellkategori?.isAktiv != null &&
                    it.autorisasjon?.isAktiv == true &&
                    it.helsepersonellkategori.isAktiv != null &&
                    it.helsepersonellkategori.verdi != null &&
                    it.helsepersonellkategori.let { it.isAktiv && it.verdi in listOf("LE", "KI", "MT", "TL") }
        }
    }),
    // TODO we need to approv FT see: https://www.nav.no/rettskildene-intern/forskrift/F20051221-1668
    // New rule???
}
