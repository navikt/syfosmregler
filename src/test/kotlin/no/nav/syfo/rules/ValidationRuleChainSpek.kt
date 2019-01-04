package no.nav.syfo.rules

import no.nav.helse.sm2013.CV
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.helse.sm2013.Ident
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.executeFlow
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime

object ValidationRuleChainSpek : Spek({
    fun deafaultHelseOpplysningerArbeidsuforhet() = HelseOpplysningerArbeidsuforhet().apply {
        aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
            periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                periodeFOMDato = LocalDate.of(2018, 1, 7)
                periodeTOMDato = LocalDate.of(2018, 1, 9)
            })
        }
        medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
            hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    dn = "Diabetes mellitus INA"
                    s = "2.16.578.1.12.4.1.1.7170"
                    v = "T90"
                }
            }
        }
    }

    fun deafaultRuleMetadata() = RuleMetadata(receivedDate = LocalDateTime.now(),
            signatureDate = LocalDateTime.now())

    describe("Testing validation rules and checking the rule outcomes") {

        it("Should check rule 1002") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
                fodselsnummer = Ident().apply {
                    id = "3006310441"
                    typeId = CV().apply {
                        dn = "Fødselsnummer"
                        s = "2.16.578.1.12.4.1.1.8116"
                        v = "FNR"
                    }
                }
            }

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.INVALID_FNR_SIZE } shouldEqual true
        }
    }
})