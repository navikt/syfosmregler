package no.nav.syfo.rules

import com.devskiller.jfairy.Fairy
import com.devskiller.jfairy.producer.person.PersonProperties
import com.devskiller.jfairy.producer.person.PersonProvider
import no.nav.syfo.ICPC2
import no.nav.syfo.Kodeverk
import no.nav.syfo.RuleData
import no.nav.syfo.QuestionId
import no.nav.syfo.QuestionGroup
import no.nav.syfo.generateMedisinskVurdering
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.SporsmalSvar
import no.nav.syfo.model.SvarRestriksjon
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.validation.validatePersonAndDNumber
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val fairy: Fairy = Fairy.create() // (Locale("no", "NO"))
val personNumberDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy")

object ValidationRuleChainSpek : Spek({
    fun Kodeverk.toDiagnose() = Diagnose(system = oid, kode = codeValue)

    fun ruleData(
        healthInformation: Sykmelding,
        receivedDate: LocalDateTime = LocalDateTime.now(),
        signatureDate: LocalDateTime = LocalDateTime.now(),
        patientPersonNumber: String = "1234567891",
        rulesetVersion: String = "1"
    ): RuleData<RuleMetadata> = RuleData(healthInformation, RuleMetadata(signatureDate, receivedDate, patientPersonNumber, rulesetVersion))

    describe("Testing validation rules and checking the rule outcomes") {

        it("Should check rule INVALID_FNR_SIZE, should trigger rule") {
            ValidationRuleChain.INVALID_FNR_SIZE(ruleData(generateSykmelding(), patientPersonNumber = "3006310441")) shouldEqual true
        }

        it("Should check rule INVALID_FNR_SIZE, should NOT trigger rule") {
            ValidationRuleChain.INVALID_FNR_SIZE(ruleData(generateSykmelding(), patientPersonNumber = "04030350265")) shouldEqual false
        }

        it("Should check rule INVALID_FNR,should trigger rule") {
            ValidationRuleChain.INVALID_FNR(ruleData(generateSykmelding(), patientPersonNumber = "30063104424")) shouldEqual true
        }

        it("Should check rule INVALID_FNR,should NOT trigger rule") {
            ValidationRuleChain.INVALID_FNR(ruleData(generateSykmelding(), patientPersonNumber = "04030350265")) shouldEqual false
        }

        it("Should check rule YOUNGER_THAN_13,should trigger rule") {
            val person = fairy.person(PersonProperties.ageBetween(PersonProvider.MIN_AGE, 12))

            ValidationRuleChain.YOUNGER_THAN_13(ruleData(
                    generateSykmelding(),
                    patientPersonNumber = generatePersonNumber(person.dateOfBirth, false)
            )) shouldEqual true
        }

        it("Should check rule YOUNGER_THAN_13,should NOT trigger rule") {
            val person = fairy.person(
                    PersonProperties.ageBetween(13, 70))

            ValidationRuleChain.YOUNGER_THAN_13(ruleData(
                    generateSykmelding(),
                    patientPersonNumber = generatePersonNumber(person.dateOfBirth, false)
            )) shouldEqual false
        }

        it("Should check rule PATIENT_OVER_70_YEARS,should trigger rule") {
            val person = fairy.person(
                    PersonProperties.ageBetween(71, 88))

            ValidationRuleChain.PATIENT_OVER_70_YEARS(ruleData(
                    generateSykmelding(),
                    patientPersonNumber = generatePersonNumber(person.dateOfBirth, false)
            )) shouldEqual true
        }

        it("Should check rule PATIENT_OVER_70_YEARS,should NOT trigger rule") {
            val person = fairy.person(
                    PersonProperties.ageBetween(13, 69))

            ValidationRuleChain.PATIENT_OVER_70_YEARS(ruleData(
                    generateSykmelding(),
                    patientPersonNumber = generatePersonNumber(person.dateOfBirth, false)
            )) shouldEqual false
        }

        it("Should check rule ICPC_2_Z_DIAGNOSE,should trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = ICPC2.Z09.toDiagnose()
            ))

            ValidationRuleChain.ICPC_2_Z_DIAGNOSE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule ICPC_2_Z_DIAGNOSE,should NOT trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = ICPC2.A09.toDiagnose()
            ))

            ValidationRuleChain.ICPC_2_Z_DIAGNOSE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule ICPC_2_Z_DIAGNOSE,should NOT trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                            system = "2.16.578.1.12.4.1.1.7170",
                            kode = "A62"
                    )
            ))

            ValidationRuleChain.ICPC_2_Z_DIAGNOSE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule MAIN_DIAGNOSE_MISSING_AND_MISSING_REASON,should trigger rule") {
            val healthInformation = generateSykmelding(
                    medisinskVurdering = generateMedisinskVurdering(
                            hovedDiagnose = null,
                            annenFraversArsak = null
                    )
            )

            ValidationRuleChain.MAIN_DIAGNOSE_MISSING_AND_MISSING_REASON(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule MAIN_DIAGNOSE_MISSING_AND_MISSING_REASON,should NOT trigger rule") {
            val healthInformation = generateSykmelding()

            ValidationRuleChain.MAIN_DIAGNOSE_MISSING_AND_MISSING_REASON(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule UNKNOWN_DIAGNOSECODE_TYPE,should trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.9999", kode = "A09")
            ))

            ValidationRuleChain.UNKNOWN_DIAGNOSECODE_TYPE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule UNKNOWN_DIAGNOSECODE_TYPE,should NOT trigger rule") {
            val healthInformation = generateSykmelding()

            ValidationRuleChain.UNKNOWN_DIAGNOSECODE_TYPE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule INVALID_KODEVERK_FOR_MAIN_DIAGNOSE, wrong kodeverk for hoveddiagnose") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7110", kode = "Z09")
            ))

            ValidationRuleChain.INVALID_KODEVERK_FOR_MAIN_DIAGNOSE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule INVALID_KODEVERK_FOR_BI_DIAGNOSE, wrong kodeverk for biDiagnoser") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    bidiagnoser = listOf(Diagnose(system = "2.16.578.1.12.4.1.1.7110", kode = "Z09"))
            ))

            ValidationRuleChain.INVALID_KODEVERK_FOR_BI_DIAGNOSE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule MISSING_REQUIRED_DYNAMIC_QUESTIONS, missing utdypendeOpplysninger") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2018, 1, 7),
                            tom = LocalDate.of(2018, 3, 9)
                    )
            ))

            ValidationRuleChain.MISSING_REQUIRED_DYNAMIC_QUESTIONS(ruleData(healthInformation)) shouldEqual true
        }

        it("MISSING_REQUIRED_DYNAMIC_QUESTIONS should not hit whenever there is no period longer then 8 weeks") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2018, 1, 7),
                            tom = LocalDate.of(2018, 1, 9)
                    )
            ))

            ValidationRuleChain.MISSING_REQUIRED_DYNAMIC_QUESTIONS(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule MISSING_REQUIRED_DYNAMIC_QUESTIONS, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                    perioder = listOf(
                            generatePeriode(
                                    fom = LocalDate.of(2018, 1, 7),
                                    tom = LocalDate.of(2018, 3, 9)
                            )
                    ),
                    utdypendeOpplysninger = mapOf(
                            QuestionGroup.GROUP_6_2.spmGruppeId to mapOf(
                                    QuestionId.ID_6_2_1.spmId to SporsmalSvar("Pasienten er syk", listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)),
                                    QuestionId.ID_6_2_2.spmId to SporsmalSvar("Pasienten er syk", listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)),
                                    QuestionId.ID_6_2_3.spmId to SporsmalSvar("Pasienten er syk", listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)),
                                    QuestionId.ID_6_2_4.spmId to SporsmalSvar("Pasienten er syk", listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER))
                            )
                    )
            )

            ValidationRuleChain.MISSING_REQUIRED_DYNAMIC_QUESTIONS(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule INVALID_RULESET_VERSION, should trigger rule") {
            ValidationRuleChain.INVALID_RULESET_VERSION(ruleData(generateSykmelding(), rulesetVersion = "999")) shouldEqual true
        }

        it("Should check rule INVALID_RULESET_VERSION, should NOT trigger rule") {
            ValidationRuleChain.INVALID_RULESET_VERSION(ruleData(generateSykmelding(), rulesetVersion = "2")) shouldEqual false
        }

        it("Should check rule MISSING_REQUIRED_DYNAMIC_QUESTIONS_AFTER_RULE_SET_VERSION_2, should trigger rule") {
            val healthInformation = generateSykmelding(
                    perioder = listOf(generatePeriode(
                            fom = LocalDate.of(2018, 1, 7),
                            tom = LocalDate.of(2018, 9, 9)
                    )),
                    utdypendeOpplysninger = mapOf(
                            QuestionGroup.GROUP_6_3.spmGruppeId to mapOf(
                                    QuestionId.ID_6_3_1.spmId to SporsmalSvar("Pasienten er syk", listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER))
                            )
                    )
            )

            ValidationRuleChain.MISSING_DYNAMIC_QUESTION_VERSION2_WEEK_17(ruleData(healthInformation, rulesetVersion = "2")) shouldEqual true
        }

        it("Should check rule MISSING_REQUIRED_DYNAMIC_QUESTIONS_AFTER_RULE_SET_VERSION_2, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                    perioder = listOf(generatePeriode(
                            fom = LocalDate.of(2018, 1, 7),
                            tom = LocalDate.of(2018, 9, 9)
                    )),
                    utdypendeOpplysninger = mapOf(
                            QuestionGroup.GROUP_6_3.spmGruppeId to mapOf(
                                    QuestionId.ID_6_3_1.spmId to SporsmalSvar("Pasienten er syk", listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)),
                                    QuestionId.ID_6_3_2.spmId to SporsmalSvar("Pasienten er syk", listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER))
                            )
                    )
            )

            ValidationRuleChain.MISSING_DYNAMIC_QUESTION_VERSION2_WEEK_7(ruleData(healthInformation, rulesetVersion = "2")) shouldEqual false
        }

        it("MISSING_DYNAMIC_QUESTION_VERSION2_WEEK_7 should trigger on missing UtdypendeOpplysninger") {
            val healthInformation = generateSykmelding(
                    perioder = listOf(generatePeriode(
                            fom = LocalDate.of(2018, 1, 7),
                            tom = LocalDate.of(2018, 9, 9)
                    ))
            )

            ValidationRuleChain.MISSING_DYNAMIC_QUESTION_VERSION2_WEEK_7(ruleData(healthInformation, rulesetVersion = "2")) shouldEqual true
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
