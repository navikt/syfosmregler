package no.nav.syfo.rules

import no.nav.model.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.Rule
import no.nav.syfo.RuleChain
import no.nav.syfo.model.Status
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person

data class TpsRuleInfo(
    val person: Person,
    val healthInformation: HelseOpplysningerArbeidsuforhet
)

val tpsRuleChain = RuleChain<TpsRuleInfo>(
        name = "TPS rule chain",
        description = "Rules to be executed after doing a WS call to TPS",
        rules = listOf(
                Rule(
                        name = "Patient is not registered",
                        ruleId = 1303,
                        status = Status.INVALID,
                        description = "WS call returned an exception because the patient is not registered in TPS"
                ) {
                    // Since we're getting an exception when doing the WS call it might be difficult to solve this in a
                    // good way
                    false
                },
                Rule(
                        name = "Patient is registered as dead in TPS",
                        ruleId = 1307,
                        status = Status.INVALID,
                        description = "The patient is registered dead in TPS"
                ) {
                    it.person.doedsdato != null
                }
        )
)
