package no.nav.syfo.rules

import com.devskiller.jfairy.Fairy
import com.devskiller.jfairy.producer.person.PersonProperties
import com.devskiller.jfairy.producer.person.PersonProvider
import no.nav.helse.sm2013.CV
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.helse.sm2013.Ident
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.executeFlow
import no.nav.syfo.validation.validatePersonAndDNumber
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


val fairy: Fairy = Fairy.create(Locale("no", "NO"))
val personNumberDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy")

object ValidationRuleChainSpek : Spek({
    fun deafaultHelseOpplysningerArbeidsuforhet() = HelseOpplysningerArbeidsuforhet().apply {
        aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
            periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                periodeFOMDato = LocalDate.of(2018, 1, 7)
                periodeTOMDato = LocalDate.of(2018, 1, 9)
            })
        }

        pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
            fodselsnummer = Ident().apply {
                id = generatePersonNumber(LocalDate.of(1991, 1, 1), false)
                typeId = CV().apply {
                    dn = "Fødselsnummer"
                    s = "2.16.578.1.12.4.1.1.8116"
                    v = "FNR"
                }
            }
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
            healthInformation.pasient.fodselsnummer.id = "3006310441"

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.INVALID_FNR_SIZE } shouldEqual true
        }

        it("Should check rule 1006") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.pasient.fodselsnummer.id = "30063104424"

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.INVALID_FNR } shouldEqual true
        }

        it("Should check rule 1101") {
            val person = fairy.person(
                    PersonProperties.ageBetween(PersonProvider.MIN_AGE, 12))

            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.pasient.fodselsnummer.id = generatePersonNumber(person.dateOfBirth, false)

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.YOUNGER_THAN_13 } shouldEqual true
        }

        it("Should check rule 1102") {
            val person = fairy.person(
                    PersonProperties.ageBetween(71, 88))

            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.pasient.fodselsnummer.id = generatePersonNumber(person.dateOfBirth, false)

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.PATIENT_OVER_70_YEARS } shouldEqual true
        }

        it("Should check rule 1137") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.medisinskVurdering.hovedDiagnose.diagnosekode.s = "2.16.578.1.12.4.1.1.9999"

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.UNKNOWN_DIAGNOSECODE_TYPE } shouldEqual true
        }

    }
})

fun generatePersonNumber(bornDate: LocalDate, useDNumber: Boolean = false): String {
    val personDate = bornDate.format(personNumberDateFormat).let {
        if (useDNumber) "${it[0] + 4}${it.substring(1)}" else it
    }
    return (if (bornDate.year >= 2000) (75011..99999) else (11111..50099))
            .map { "$personDate$it" }
            .first {
                validatePersonAndDNumber(it)
            }
}