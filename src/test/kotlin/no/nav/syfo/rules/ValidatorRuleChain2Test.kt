package no.nav.syfo.rules

import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime

object ValidatorRuleChain2Test : Spek({
    fun ruleData(
        healthInformation: Sykmelding,
        receivedDate: LocalDateTime = LocalDateTime.now(),
        signatureDate: LocalDateTime = LocalDateTime.now(),
        behandletTidspunkt: LocalDateTime = LocalDateTime.now(),
        patientPersonNumber: String = "08089408084",
        rulesetVersion: String = "1",
        legekontorOrgNr: String = "123456789",
        tssid: String? = "1314445",
        avsenderfnr: String = "12344",
    ): RuleData<RuleMetadata> = RuleData(healthInformation, RuleMetadata(signatureDate, receivedDate, behandletTidspunkt, patientPersonNumber, rulesetVersion, legekontorOrgNr, tssid, avsenderfnr, LocalDate.now()))

    describe("testy test") {
        it("very test") {
            val sykmelding = generateSykmelding()
            val chain = ValidationRuleChain(sykmelding, ruleData(sykmelding).metadata)

            chain.rules.map { it.executeRule() }.forEach {
                println("Did rule pass:")
                println(it.result)
                println("Rule input:")
                println(it.rule.toInputMap())
            }
        }
    }
})
