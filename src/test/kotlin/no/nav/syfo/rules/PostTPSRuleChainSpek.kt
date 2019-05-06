package no.nav.syfo.rules

import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Sykmelding
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Diskresjonskoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PostTPSRuleChainSpek : Spek({

    fun ruleData(
        healthInformation: Sykmelding,
        person: Person
    ): RuleData<Person> = RuleData(healthInformation, person)

    describe("Testing validation rules and checking the rule outcomes") {

        it("Should check rule PASIENTEN_HAR_KODE_6, should trigger rule") {
            val healthInformation = generateSykmelding()
            val tpsPerson = Person().apply {
                diskresjonskode = Diskresjonskoder().apply {
                    kodeverksRef = "SPSF"
                }
            }

            PostTPSRuleChain.PASIENTEN_HAR_KODE_6(ruleData(healthInformation, tpsPerson)) shouldEqual true
        }

        it("Should check rule PASIENTEN_HAR_KODE_6, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val tpsPerson = Person()

            PostTPSRuleChain.PASIENTEN_HAR_KODE_6(ruleData(healthInformation, tpsPerson)) shouldEqual false
        }
    }
})
