package no.nav.syfo.rules

import no.nav.syfo.RuleData
import no.nav.syfo.generateGradert
import no.nav.syfo.generateKontaktMedPasient
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime

object PeriodLogicRuleChainSpek : Spek({
    fun ruleData(
        healthInformation: Sykmelding,
        receivedDate: LocalDateTime = LocalDateTime.now(),
        signatureDate: LocalDateTime = LocalDateTime.now(),
        patientPersonNumber: String = "1234567891"
    ): RuleData<RuleMetadata> = RuleData(healthInformation, RuleMetadata(signatureDate, receivedDate, patientPersonNumber, "1", "123456789"))

    describe("Testing validation rules and checking the rule outcomes") {

        it("Should check rule SIGNATURE_DATE_AFTER_RECEIVED_DATE, should trigger rule") {
            val healthInformation = generateSykmelding()

            PeriodLogicRuleChain.SIGNATURE_DATE_AFTER_RECEIVED_DATE(ruleData(healthInformation, signatureDate = LocalDateTime.now().minusDays(2))) shouldEqual true
        }

        it("Should check rule SIGNATURE_DATE_AFTER_RECEIVED_DATE, should NOT trigger rule") {
            val healthInformation = generateSykmelding()

            PeriodLogicRuleChain.SIGNATURE_DATE_AFTER_RECEIVED_DATE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule NO_PERIOD_PROVIDED, should trigger rule") {
            val healthInformation = generateSykmelding(
                    perioder = listOf()
            )

            PeriodLogicRuleChain.NO_PERIOD_PROVIDED(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule NO_PERIOD_PROVIDED, should NOT trigger rule") {
            val healthInformation = generateSykmelding()

            PeriodLogicRuleChain.NO_PERIOD_PROVIDED(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule TO_DATE_BEFORE_FROM_DATE, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(generatePeriode(fom = LocalDate.of(2018, 1, 9), tom = LocalDate.of(2018, 1, 7))))

            PeriodLogicRuleChain.TO_DATE_BEFORE_FROM_DATE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule TO_DATE_BEFORE_FROM_DATE, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2018, 1, 7),
                            tom = LocalDate.of(2018, 1, 9)
                    )))

            PeriodLogicRuleChain.TO_DATE_BEFORE_FROM_DATE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule OVERLAPPING_PERIODS, should trigger rule") {
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

            PeriodLogicRuleChain.OVERLAPPING_PERIODS(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule OVERLAPPING_PERIODS, should NOT trigger rule") {
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

            PeriodLogicRuleChain.OVERLAPPING_PERIODS(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule GAP_BETWEEN_PERIODS, should trigger rule") {
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

            PeriodLogicRuleChain.GAP_BETWEEN_PERIODS(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule GAP_BETWEEN_PERIODS, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2018, 1, 1),
                            tom = LocalDate.of(2018, 2, 1)
                    ),

                    generatePeriode(
                            fom = LocalDate.of(2018, 2, 1),
                            tom = LocalDate.of(2018, 5, 1)
                    )))

            PeriodLogicRuleChain.GAP_BETWEEN_PERIODS(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule BACKDATED_MORE_THEN_3_YEARS, should trigger rule") {
            val healthInformation = generateSykmelding(
                    behandletTidspunkt = LocalDateTime.now().minusYears(3).minusDays(1)
            )

            PeriodLogicRuleChain.BACKDATED_MORE_THEN_3_YEARS(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule BACKDATED_MORE_THEN_3_YEARS, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                    behandletTidspunkt = LocalDateTime.now().minusYears(2)
            )

            PeriodLogicRuleChain.BACKDATED_MORE_THEN_3_YEARS(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule BACKDATED_WITH_REASON, should trigger rule") {
            val healthInformation = generateSykmelding(
                    behandletTidspunkt = LocalDateTime.now().minusYears(2),
                    kontaktMedPasient = generateKontaktMedPasient(
                            begrunnelseIkkeKontakt = "Noe tull skjedde, med sykmeldingen"
                    )
            )

            PeriodLogicRuleChain.BACKDATED_WITH_REASON(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule BACKDATED_WITH_REASON, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                    behandletTidspunkt = LocalDateTime.now().minusYears(3)
            )

            PeriodLogicRuleChain.BACKDATED_WITH_REASON(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule PRE_DATED, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now().plusDays(31),
                            tom = LocalDate.now().plusDays(37)
                    )
            ))

            PeriodLogicRuleChain.PRE_DATED(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule PRE_DATED, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now().plusDays(29),
                            tom = LocalDate.now().plusDays(31)
                    )))

            PeriodLogicRuleChain.PRE_DATED(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule END_DATE, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusYears(1)
                    )
            ),
                    behandletTidspunkt = LocalDateTime.now().minusDays(1)
            )

            PeriodLogicRuleChain.END_DATE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule END_DATE, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusYears(1)
                    )
            ),
                    behandletTidspunkt = LocalDateTime.now()
            )

            PeriodLogicRuleChain.END_DATE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule RECEIVED_DATE_BEFORE_PROCESSED_DATE, should trigger rule") {
            val healthInformation = generateSykmelding(
                    behandletTidspunkt = LocalDateTime.now().plusHours(3)
            )

            PeriodLogicRuleChain.RECEIVED_DATE_BEFORE_PROCESSED_DATE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule RECEIVED_DATE_BEFORE_PROCESSED_DATE, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                    behandletTidspunkt = LocalDateTime.now().plusHours(2)
            )

            PeriodLogicRuleChain.RECEIVED_DATE_BEFORE_PROCESSED_DATE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule PENDING_SICK_LEAVE_COMBINED, should trigger rule") {
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

            PeriodLogicRuleChain.PENDING_SICK_LEAVE_COMBINED(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule PENDING_SICK_LEAVE_COMBINED, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(5),
                            avventendeInnspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                    )
            ))

            PeriodLogicRuleChain.PENDING_SICK_LEAVE_COMBINED(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule MISSING_INSPILL_TIL_ARBEIDSGIVER, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(5),
                            avventendeInnspillTilArbeidsgiver = "      "
                    )
            ))

            PeriodLogicRuleChain.MISSING_INSPILL_TIL_ARBEIDSGIVER(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule MISSING_INSPILL_TIL_ARBEIDSGIVER, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(5),
                            avventendeInnspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                    )
            ))

            PeriodLogicRuleChain.MISSING_INSPILL_TIL_ARBEIDSGIVER(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule PENDING_PERIOD_OUTSIDE_OF_EMPLOYER_PAID, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(17),
                            avventendeInnspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                    )
            ))

            PeriodLogicRuleChain.PENDING_PERIOD_OUTSIDE_OF_EMPLOYER_PAID(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule PENDING_PERIOD_OUTSIDE_OF_EMPLOYER_PAID, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(16),
                            avventendeInnspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                    )
            ))

            PeriodLogicRuleChain.PENDING_PERIOD_OUTSIDE_OF_EMPLOYER_PAID(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule TOO_MANY_TREATMENT_DAYS, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 7),
                            tom = LocalDate.of(2019, 1, 13),
                            behandlingsdager = 2
                    )
            ))

            PeriodLogicRuleChain.TOO_MANY_TREATMENT_DAYS(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule TOO_MANY_TREATMENT_DAYS, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 7),
                            tom = LocalDate.of(2019, 1, 13),
                            behandlingsdager = 1
                    )
            ))

            PeriodLogicRuleChain.TOO_MANY_TREATMENT_DAYS(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule PARTIAL_SICK_LEAVE_PERCENTAGE_TO_LOW, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now(),
                            gradert = generateGradert(grad = 19)
                    )
            ))

            PeriodLogicRuleChain.PARTIAL_SICK_LEAVE_PERCENTAGE_TO_LOW(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule PARTIAL_SICK_LEAVE_PERCENTAGE_TO_LOW, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now(),
                            gradert = generateGradert(grad = 20)
                    )
            ))

            PeriodLogicRuleChain.PARTIAL_SICK_LEAVE_PERCENTAGE_TO_LOW(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule PARTIAL_SICK_LEAVE_TOO_HIGH_PERCENTAGE, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now(),
                            gradert = generateGradert(grad = 100)
                    )
            ))

            PeriodLogicRuleChain.PARTIAL_SICK_LEAVE_TOO_HIGH_PERCENTAGE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule PARTIAL_SICK_LEAVE_TOO_HIGH_PERCENTAGE, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now(),
                            gradert = generateGradert(grad = 99)
                    )
            ))

            PeriodLogicRuleChain.PARTIAL_SICK_LEAVE_TOO_HIGH_PERCENTAGE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule BACKDATING_SYKMELDING_EXTENSION, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now()
                    )
            ))

            PeriodLogicRuleChain.BACKDATING_SYKMELDING_EXTENSION(ruleData(healthInformation, signatureDate = LocalDateTime.now().minusMonths(1).minusDays(1))) shouldEqual true
        }

        it("Should check rule BACKDATING_SYKMELDING_EXTENSION, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now()
                    )
            ))

            PeriodLogicRuleChain.BACKDATING_SYKMELDING_EXTENSION(ruleData(healthInformation, signatureDate = LocalDateTime.now().minusMonths(1))) shouldEqual false
        }
    }
})
