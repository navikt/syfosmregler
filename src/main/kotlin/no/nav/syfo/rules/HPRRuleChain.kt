package no.nav.syfo.rules

import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.model.Status
import no.nhn.schemas.reg.hprv2.Person as HPRPerson

enum class HPRRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<HPRPerson>) -> Boolean) : Rule<RuleData<HPRPerson>> {
    @Description(" Behandler ikke registrert i HPR")
    BEHANDLER_NOT_IN_HPR(1401, Status.INVALID, { (_, doctor) ->
        doctor.nin.isNullOrEmpty()
    }),

    @Description("Behandler er ikke gyldig i HPR på konsultasjonstidspunkt..")
    BEHANDLER_NOT_VALDIG_IN_HPR(1402, Status.INVALID, { (_, doctor) ->
        !doctor.godkjenninger.godkjenning.any {
            it.autorisasjon.isAktiv
        }
    }),

    @Description("Behandler har ikkje gylding autorisasjon i HPR")
    BEHANDLER_NOT_VALID_AUTHORIZATION_IN_HPR(1403, Status.INVALID, { (_, doctor) ->
        !doctor.godkjenninger.godkjenning.any {
            it.autorisasjon.isAktiv && it.autorisasjon.oid == 7704 && it.autorisasjon.verdi in arrayOf("1", "17", "4", "3", "2", "14")
        }
    }),

    @Description("Behandler finnes i HPR men er ikke lege, kiropraktor, manuellterapeut eller tannlege")
    BEHANDLER_NOT_LE_KI_MT_TL_IN_HPR(1407, Status.MANUAL_PROCESSING, { (_, doctor) ->
        !doctor.godkjenninger.godkjenning.any {
            it?.helsepersonellkategori?.isAktiv != null &&
                    it.autorisasjon?.isAktiv == true &&
                    it.helsepersonellkategori.let { it.isAktiv && it.verdi in listOf("LE", "KI", "MT", "TL") }
        }
    }),
}
