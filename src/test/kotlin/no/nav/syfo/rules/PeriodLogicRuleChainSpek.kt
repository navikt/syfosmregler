package no.nav.syfo.rules

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.generateGradert
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Periode
import no.nav.syfo.model.RuleMetadata
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate
import java.time.LocalDateTime

class PeriodLogicRuleChainSpek : FunSpec({
    fun ruleMetadata(
        receivedDate: LocalDateTime = LocalDateTime.now(),
        signatureDate: LocalDateTime = LocalDateTime.now(),
        behandletTidspunkt: LocalDateTime = LocalDateTime.now(),
        patientPersonNumber: String = "1234567891",
        tssid: String? = "1314445",
        avsenderFnr: String = "1345525522",
    ): RuleMetadata = RuleMetadata(
        signatureDate,
        receivedDate,
        behandletTidspunkt,
        patientPersonNumber,
        "1",
        "123456789",
        tssid,
        avsenderFnr,
        pasientFodselsdato = LocalDate.now()
    )

    context("Testing validation rules and checking the rule outcomes") {
        test("Should check rule PERIODER_MANGLER, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf()
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("PERIODER_MANGLER")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule PERIODER_MANGLER, should NOT trigger rule") {
            val healthInformation = generateSykmelding()

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("PERIODER_MANGLER")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule FRADATO_ETTER_TILDATO, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 9),
                        tom = LocalDate.of(2018, 1, 7)
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("FRADATO_ETTER_TILDATO")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule FRADATO_ETTER_TILDATO, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 7),
                        tom = LocalDate.of(2018, 1, 9)
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("FRADATO_ETTER_TILDATO")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule OVERLAPPENDE_PERIODER, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 7),
                        tom = LocalDate.of(2018, 1, 9)
                    ),
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 8),
                        tom = LocalDate.of(2018, 1, 12)
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("OVERLAPPENDE_PERIODER")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule OVERLAPPENDE_PERIODER, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 7),
                        tom = LocalDate.of(2018, 1, 9)
                    ),
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 10),
                        tom = LocalDate.of(2018, 1, 12)
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("OVERLAPPENDE_PERIODER")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule OPPHOLD_MELLOM_PERIODER, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 1),
                        tom = LocalDate.of(2018, 1, 3)
                    ),
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 3),
                        tom = LocalDate.of(2018, 1, 9)
                    ),
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 9),
                        tom = LocalDate.of(2018, 1, 10)
                    ),
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 15),
                        tom = LocalDate.of(2018, 1, 20)
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("OPPHOLD_MELLOM_PERIODER")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule OPPHOLD_MELLOM_PERIODER, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2020, 2, 15),
                        tom = LocalDate.of(2020, 3, 18)
                    ),
                    generatePeriode(
                        fom = LocalDate.of(2019, 10, 31),
                        tom = LocalDate.of(2019, 12, 19)
                    ),
                    generatePeriode(
                        fom = LocalDate.of(2020, 3, 19),
                        tom = LocalDate.of(2020, 3, 26)
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("OPPHOLD_MELLOM_PERIODER")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule OPPHOLD_MELLOM_PERIODER, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 1),
                        tom = LocalDate.of(2018, 2, 1)
                    ),

                    generatePeriode(
                        fom = LocalDate.of(2018, 2, 1),
                        tom = LocalDate.of(2018, 5, 1)
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("OPPHOLD_MELLOM_PERIODER")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule IKKE_DEFINERT_PERIODE, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    Periode(
                        fom = LocalDate.of(2018, 2, 1),
                        tom = LocalDate.of(2018, 2, 2),
                        aktivitetIkkeMulig = null,
                        gradert = null,
                        avventendeInnspillTilArbeidsgiver = null,
                        reisetilskudd = false,
                        behandlingsdager = 0
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("IKKE_DEFINERT_PERIODE")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule IKKE_DEFINERT_PERIODE, should not trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    Periode(
                        fom = LocalDate.of(2018, 2, 1),
                        tom = LocalDate.of(2018, 2, 2),
                        aktivitetIkkeMulig = null,
                        gradert = null,
                        avventendeInnspillTilArbeidsgiver = null,
                        reisetilskudd = false,
                        behandlingsdager = 1
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("IKKE_DEFINERT_PERIODE")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule TILBAKEDATERT_MER_ENN_3_AR, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    Periode(
                        fom = LocalDate.now().minusYears(3).minusDays(14),
                        tom = LocalDate.now().minusYears(3),
                        aktivitetIkkeMulig = null,
                        avventendeInnspillTilArbeidsgiver = null,
                        behandlingsdager = 1,
                        gradert = null,
                        reisetilskudd = false
                    )
                ),
                behandletTidspunkt = LocalDateTime.now()
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("TILBAKEDATERT_MER_ENN_3_AR")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule TILBAKEDATERT_MER_ENN_3_AR, should not trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    Periode(
                        fom = LocalDate.now().minusDays(14),
                        tom = LocalDate.now(),
                        aktivitetIkkeMulig = null,
                        avventendeInnspillTilArbeidsgiver = null,
                        behandlingsdager = 1,
                        gradert = null,
                        reisetilskudd = false
                    )
                ),
                behandletTidspunkt = LocalDateTime.now()
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("TILBAKEDATERT_MER_ENN_3_AR")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule FREMDATERT, should trigger rule") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now().plusDays(31),
                        tom = LocalDate.now().plusDays(37)
                    )
                ),
                behandletTidspunkt = LocalDateTime.now()
            )

            PeriodLogicRuleChain(sykmelding, ruleMetadata()).getRuleByName("FREMDATERT")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule FREMDATERT, should NOT trigger rule") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now().plusDays(29),
                        tom = LocalDate.now().plusDays(31)
                    )
                ),
                behandletTidspunkt = LocalDateTime.now().minusDays(1)
            )

            PeriodLogicRuleChain(sykmelding, ruleMetadata()).getRuleByName("FREMDATERT")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule TOTAL_VARIGHET_OVER_ETT_AAR, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(1)
                    ),
                    generatePeriode(
                        fom = LocalDate.now().plusDays(1),
                        tom = LocalDate.now().plusDays(366)
                    )
                ),
                behandletTidspunkt = LocalDateTime.now()
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("TOTAL_VARIGHET_OVER_ETT_AAR")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule TOTAL_VARIGHET_OVER_ETT_AAR, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(350)
                    )
                ),
                behandletTidspunkt = LocalDateTime.now()
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("TOTAL_VARIGHET_OVER_ETT_AAR")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule BEHANDLINGSDATO_ETTER_MOTTATTDATO, should trigger rule") {
            val healthInformation = generateSykmelding(
                behandletTidspunkt = LocalDateTime.now().plusDays(2)
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("BEHANDLINGSDATO_ETTER_MOTTATTDATO")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule BEHANDLINGSDATO_ETTER_MOTTATTDATO, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                behandletTidspunkt = LocalDateTime.now().plusDays(1)
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("BEHANDLINGSDATO_ETTER_MOTTATTDATO")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule AVVENTENDE_SYKMELDING_KOMBINERT, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(5),
                        avventendeInnspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                    ),

                    generatePeriode(
                        fom = LocalDate.now().plusDays(5),
                        tom = LocalDate.now().plusDays(10)
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("AVVENTENDE_SYKMELDING_KOMBINERT")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule AVVENTENDE_SYKMELDING_KOMBINERT, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(5)
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("AVVENTENDE_SYKMELDING_KOMBINERT")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule AVVENTENDE_SYKMELDING_KOMBINERT, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(5),
                        avventendeInnspillTilArbeidsgiver = "Jobbe"
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("AVVENTENDE_SYKMELDING_KOMBINERT")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(5),
                        avventendeInnspillTilArbeidsgiver = "      "
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(5),
                        avventendeInnspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule AVVENTENDE_SYKMELDING_OVER_16_DAGER, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(17),
                        avventendeInnspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("AVVENTENDE_SYKMELDING_OVER_16_DAGER")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule AVVENTENDE_SYKMELDING_OVER_16_DAGER, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(16),
                        avventendeInnspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("AVVENTENDE_SYKMELDING_OVER_16_DAGER")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule FOR_MANGE_BEHANDLINGSDAGER_PER_UKE, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 7),
                        tom = LocalDate.of(2019, 1, 13),
                        behandlingsdager = 2
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("FOR_MANGE_BEHANDLINGSDAGER_PER_UKE")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule FOR_MANGE_BEHANDLINGSDAGER_PER_UKE, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 7),
                        tom = LocalDate.of(2019, 1, 13),
                        behandlingsdager = 1
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("FOR_MANGE_BEHANDLINGSDAGER_PER_UKE")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule GRADERT_SYKMELDING_UNDER_20_PROSENT, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        gradert = generateGradert(grad = 19)
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("GRADERT_SYKMELDING_UNDER_20_PROSENT")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule GRADERT_SYKMELDING_UNDER_20_PROSENT, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        gradert = generateGradert(grad = 20)
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("GRADERT_SYKMELDING_UNDER_20_PROSENT")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule GRADERT_SYKMELDING_OVER_99_PROSENT, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        gradert = generateGradert(grad = 100)
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("GRADERT_SYKMELDING_OVER_99_PROSENT")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule GRADERT_SYKMELDING_OVER_99_PROSENT, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        gradert = generateGradert(grad = 99)
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("GRADERT_SYKMELDING_OVER_99_PROSENT")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule SYKMELDING_MED_BEHANDLINGSDAGER, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 7),
                        tom = LocalDate.of(2019, 1, 13),
                        behandlingsdager = 1
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("SYKMELDING_MED_BEHANDLINGSDAGER")
                .executeRule().result shouldBeEqualTo true
        }

        test("Should check rule SYKMELDING_MED_BEHANDLINGSDAGER, should not trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 7),
                        tom = LocalDate.of(2019, 1, 13)
                    )
                )
            )

            PeriodLogicRuleChain(healthInformation, ruleMetadata()).getRuleByName("SYKMELDING_MED_BEHANDLINGSDAGER")
                .executeRule().result shouldBeEqualTo false
        }
    }
})
