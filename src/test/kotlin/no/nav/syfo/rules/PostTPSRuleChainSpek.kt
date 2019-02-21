package no.nav.syfo.rules

import no.nav.syfo.executeFlow
import no.nav.syfo.generateSykmelding
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Diskresjonskoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PostTPSRuleChainSpek : Spek({

    describe("Testing validation rules and checking the rule outcomes") {

        it("Should check rule PATIENT_HAS_SPERREKODE_6, should trigger rule") {
            val healthInformation = generateSykmelding()
            val tpsPerson = Person().apply {
                diskresjonskode = Diskresjonskoder().apply {
                    kodeverksRef = "SPSF"
                }
            }

            val postTPSRuleChainResults = PostTPSRuleChain.values().toList().executeFlow(healthInformation, tpsPerson)

            postTPSRuleChainResults.any { it == PostTPSRuleChain.PATIENT_HAS_SPERREKODE_6 } shouldEqual true
        }
    }
})
