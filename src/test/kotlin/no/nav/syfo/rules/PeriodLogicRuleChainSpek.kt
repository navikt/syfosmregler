package no.nav.syfo.rules

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.syfo.generateGradert
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PeriodLogicRuleChainSpek : Spek({
    fun ruleData(
        sykmelding: Sykmelding,
        receivedDate: LocalDateTime = LocalDateTime.now(),
        signatureDate: LocalDateTime = LocalDateTime.now(),
        behandletTidspunkt: LocalDateTime = LocalDateTime.now(),
        patientPersonNumber: String = "1234567891",
        tssid: String? = "1314445",
        avsenderFnr: String = "1345525522"
    ): RuleData<RuleMetadata> = RuleData(sykmelding, RuleMetadata(signatureDate, receivedDate, behandletTidspunkt, patientPersonNumber, "1", "123456789", tssid, avsenderFnr))

    describe("Testing validation rules and checking the rule outcomes") {

        it("Should check rule PERIODER_MANGLER, should trigger rule") {
            val healthInformation = generateSykmelding(
                    perioder = listOf()
            )

            PeriodLogicRuleChain.PERIODER_MANGLER(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule PERIODER_MANGLER, should NOT trigger rule") {
            val healthInformation = generateSykmelding()

            PeriodLogicRuleChain.PERIODER_MANGLER(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule FRADATO_ETTER_TILDATO, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(generatePeriode(fom = LocalDate.of(2018, 1, 9), tom = LocalDate.of(2018, 1, 7))))

            PeriodLogicRuleChain.FRADATO_ETTER_TILDATO(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule FRADATO_ETTER_TILDATO, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2018, 1, 7),
                            tom = LocalDate.of(2018, 1, 9)
                    )))

            PeriodLogicRuleChain.FRADATO_ETTER_TILDATO(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule OVERLAPPENDE_PERIODER, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2018, 1, 7),
                            tom = LocalDate.of(2018, 1, 9)
                    ),
                    generatePeriode(
                            fom = LocalDate.of(2018, 1, 8),
                            tom = LocalDate.of(2018, 1, 12)
                    )
            ))

            PeriodLogicRuleChain.OVERLAPPENDE_PERIODER(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule OVERLAPPENDE_PERIODER, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2018, 1, 7),
                            tom = LocalDate.of(2018, 1, 9)
                    ),
                    generatePeriode(
                            fom = LocalDate.of(2018, 1, 10),
                            tom = LocalDate.of(2018, 1, 12)
                    )
            ))

            PeriodLogicRuleChain.OVERLAPPENDE_PERIODER(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule OPPHOLD_MELLOM_PERIODER, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
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
            ))

            PeriodLogicRuleChain.OPPHOLD_MELLOM_PERIODER(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule OPPHOLD_MELLOM_PERIODER, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2018, 1, 1),
                            tom = LocalDate.of(2018, 2, 1)
                    ),

                    generatePeriode(
                            fom = LocalDate.of(2018, 2, 1),
                            tom = LocalDate.of(2018, 5, 1)
                    )))

            PeriodLogicRuleChain.OPPHOLD_MELLOM_PERIODER(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_MER_ENN_3_AR, should trigger rule") {
            val healthInformation = generateSykmelding(
                    behandletTidspunkt = LocalDateTime.now().minusYears(3).minusDays(1)
            )

            PeriodLogicRuleChain.TILBAKEDATERT_MER_ENN_3_AR(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule TILBAKEDATERT_MER_ENN_3_AR, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                    behandletTidspunkt = LocalDateTime.now().minusYears(2)
            )

            PeriodLogicRuleChain.TILBAKEDATERT_MER_ENN_3_AR(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule FREMDATERT, should trigger rule") {
            val sykmelding = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now().plusDays(31),
                            tom = LocalDate.now().plusDays(37)
                    )
            ),
                    behandletTidspunkt = LocalDateTime.now()
            )

            PeriodLogicRuleChain.FREMDATERT(ruleData(sykmelding)) shouldEqual true
        }

        it("Should check rule FREMDATERT, should NOT trigger rule") {
            val sykmelding = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now().plusDays(29),
                            tom = LocalDate.now().plusDays(31)
                    )),
                    behandletTidspunkt = LocalDateTime.now().minusDays(1)
            )

            PeriodLogicRuleChain.FREMDATERT(ruleData(sykmelding)) shouldEqual false
        }

        it("Should check rule VARIGHET_OVER_ETT_AAR, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusYears(1)
                    )
            ),
                    behandletTidspunkt = LocalDateTime.now().minusDays(1)
            )

            PeriodLogicRuleChain.OVER_ETT_AAR_MELLOM_BEHANDLETDATO_OG_FOMDATO(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule VARIGHET_OVER_ETT_AAR, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusYears(1)
                    )
            ),
                    behandletTidspunkt = LocalDateTime.now()
            )

            PeriodLogicRuleChain.OVER_ETT_AAR_MELLOM_BEHANDLETDATO_OG_FOMDATO(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule TOTAL_VARIGHET_OVER_ETT_AAR, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
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

            PeriodLogicRuleChain.TOTAL_VARIGHET_OVER_ETT_AAR(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule TOTAL_VARIGHET_OVER_ETT_AAR, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(350)
                    )
            ),
                    behandletTidspunkt = LocalDateTime.now()
            )

            PeriodLogicRuleChain.TOTAL_VARIGHET_OVER_ETT_AAR(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule BEHANDLINGSDATO_ETTER_MOTTATTDATO, should trigger rule") {
            val healthInformation = generateSykmelding(
                    behandletTidspunkt = LocalDateTime.now().plusDays(2)
            )

            PeriodLogicRuleChain.BEHANDLINGSDATO_ETTER_MOTTATTDATO(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule BEHANDLINGSDATO_ETTER_MOTTATTDATO, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                    behandletTidspunkt = LocalDateTime.now().plusDays(1)
            )

            PeriodLogicRuleChain.BEHANDLINGSDATO_ETTER_MOTTATTDATO(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule AVVENTENDE_SYKMELDING_KOMBINERT, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(5),
                            avventendeInnspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                    ),

                    generatePeriode(
                            fom = LocalDate.now().plusDays(5),
                            tom = LocalDate.now().plusDays(10)
                    )
            ))

            PeriodLogicRuleChain.AVVENTENDE_SYKMELDING_KOMBINERT(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule AVVENTENDE_SYKMELDING_KOMBINERT, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(5)
                    )
            ))

            PeriodLogicRuleChain.AVVENTENDE_SYKMELDING_KOMBINERT(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule AVVENTENDE_SYKMELDING_KOMBINERT, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(5),
                            avventendeInnspillTilArbeidsgiver = "Jobbe"
                    )
            ))

            PeriodLogicRuleChain.AVVENTENDE_SYKMELDING_KOMBINERT(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(5),
                            avventendeInnspillTilArbeidsgiver = "      "
                    )
            ))

            PeriodLogicRuleChain.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(5),
                            avventendeInnspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                    )
            ))

            PeriodLogicRuleChain.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule AVVENTENDE_SYKMELDING_OVER_16_DAGER, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(17),
                            avventendeInnspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                    )
            ))

            PeriodLogicRuleChain.AVVENTENDE_SYKMELDING_OVER_16_DAGER(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule AVVENTENDE_SYKMELDING_OVER_16_DAGER, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(16),
                            avventendeInnspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                    )
            ))

            PeriodLogicRuleChain.AVVENTENDE_SYKMELDING_OVER_16_DAGER(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule FOR_MANGE_BEHANDLINGSDAGER_PER_UKE, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 7),
                            tom = LocalDate.of(2019, 1, 13),
                            behandlingsdager = 2
                    )
            ))

            PeriodLogicRuleChain.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule FOR_MANGE_BEHANDLINGSDAGER_PER_UKE, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 7),
                            tom = LocalDate.of(2019, 1, 13),
                            behandlingsdager = 1
                    )
            ))

            PeriodLogicRuleChain.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule GRADERT_SYKMELDING_UNDER_20_PROSENT, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now(),
                            gradert = generateGradert(grad = 19)
                    )
            ))

            PeriodLogicRuleChain.GRADERT_SYKMELDING_UNDER_20_PROSENT(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule GRADERT_SYKMELDING_UNDER_20_PROSENT, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now(),
                            gradert = generateGradert(grad = 20)
                    )
            ))

            PeriodLogicRuleChain.GRADERT_SYKMELDING_UNDER_20_PROSENT(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule GRADERT_SYKMELDING_OVER_99_PROSENT, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now(),
                            gradert = generateGradert(grad = 100)
                    )
            ))

            PeriodLogicRuleChain.GRADERT_SYKMELDING_OVER_99_PROSENT(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule GRADERT_SYKMELDING_OVER_99_PROSENT, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now(),
                            gradert = generateGradert(grad = 99)
                    )
            ))

            PeriodLogicRuleChain.GRADERT_SYKMELDING_OVER_99_PROSENT(ruleData(healthInformation)) shouldEqual false
        }
    }
})
