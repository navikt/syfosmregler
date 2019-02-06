package no.nav.syfo.rules

import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.model.Status
import no.nhn.schemas.reg.hprv2.Person as HPRPerson

enum class HPRRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<HPRPerson>) -> Boolean) : Rule<RuleData<HPRPerson>> {
    // TODO: 1401
    // 1401: Behandler ikke registrert i HPR
    // Kommentar fra Camilla: Denne vil endres. I ny løsning skal vi slå opp mot HPR. Dersom behandler ikke ligger i HPR vil meldingen avvises.
    // Dersom det kommer soapfault, som har følgende faultstring: ArgumentException: Personnummer ikke funnet

    @Description("Behandler er ikke gyldig i HPR på konsultasjonstidspunkt..")
    BEHANDLER_NOT_VALDIG_IN_HPR(1402, Status.INVALID, { (_, doctor) ->
        if (!doctor.godkjenninger.godkjenning.isNullOrEmpty()) {
            !doctor.godkjenninger.godkjenning.any {
                it.autorisasjon.isAktiv
            }
        } else {
            false
        }
    }),

    @Description("Behandler har ikkje gylding autorisasjon i HPR")
    BEHANDLER_OPP_IN_HPR(1403, Status.INVALID, { (_, doctor) ->
        !doctor.godkjenninger.godkjenning.any {
            it.autorisasjon.isAktiv && it.autorisasjon.oid == 7704 && ("1,17,4,3,2,14").contains(it.autorisasjon.verdi)
        }
    }),

    @Description("Behandler finnes i TSS men er ikke lege, kiropraktor, manuellterapeut eller tannlege")
    BEHANDLER_NOT_LE_KI_MT_TL_IN_HPR(1407, Status.MANUAL_PROCESSING, { (_, doctor) ->
        if (!doctor.godkjenninger?.godkjenning.isNullOrEmpty()) {
        !doctor.godkjenninger.godkjenning.any {
            if (it?.helsepersonellkategori?.isAktiv != null && it.autorisasjon != null && !it.helsepersonellkategori.verdi.isNullOrEmpty()) {
            it.autorisasjon.isAktiv && it.helsepersonellkategori.let { it.isAktiv && it.verdi in listOf("LE", "KI", "MT", "TL") }
            } else {
                false
            }
        }
        } else {
            false
        }
        }),
}
