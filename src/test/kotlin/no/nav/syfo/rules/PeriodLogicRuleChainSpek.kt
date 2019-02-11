package no.nav.syfo.rules

import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.RuleData
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime

object PeriodLogicRuleChainSpek : Spek({
    fun ruleData(
        healthInformation: HelseOpplysningerArbeidsuforhet,
        receivedDate: LocalDateTime = LocalDateTime.now(),
        signatureDate: LocalDateTime = LocalDateTime.now()
    ): RuleData<RuleMetadata> = RuleData(healthInformation, RuleMetadata(signatureDate, receivedDate))

    describe("Testing validation rules and checking the rule outcomes") {

        it("Should check rule SIGNATURE_DATE_AFTER_RECEIVED_DATE, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now()
                }
            }

            PeriodLogicRuleChain.SIGNATURE_DATE_AFTER_RECEIVED_DATE(ruleData(healthInformation, signatureDate = LocalDateTime.now().minusDays(2))) shouldEqual true
        }

        it("Should check rule SIGNATURE_DATE_AFTER_RECEIVED_DATE, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now()
                }
            }

            PeriodLogicRuleChain.SIGNATURE_DATE_AFTER_RECEIVED_DATE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule NO_PERIOD_PROVIDED, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet()

            PeriodLogicRuleChain.NO_PERIOD_PROVIDED(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule NO_PERIOD_PROVIDED, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 1, 9)
                        periodeTOMDato = LocalDate.of(2018, 1, 7)
                    })
                }
            }

            PeriodLogicRuleChain.NO_PERIOD_PROVIDED(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule TO_DATE_BEFORE_FROM_DATE, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 1, 9)
                        periodeTOMDato = LocalDate.of(2018, 1, 7)
                    })
                }
            }

            PeriodLogicRuleChain.TO_DATE_BEFORE_FROM_DATE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule TO_DATE_BEFORE_FROM_DATE, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 1, 7)
                        periodeTOMDato = LocalDate.of(2018, 1, 9)
                    })
                }
            }

            PeriodLogicRuleChain.TO_DATE_BEFORE_FROM_DATE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule OVERLAPPING_PERIODS, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 1, 7)
                        periodeTOMDato = LocalDate.of(2018, 1, 9)
                    })

                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 1, 8)
                        periodeTOMDato = LocalDate.of(2018, 1, 12)
                    })
                }
            }

            PeriodLogicRuleChain.OVERLAPPING_PERIODS(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule OVERLAPPING_PERIODS, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 1, 7)
                        periodeTOMDato = LocalDate.of(2018, 1, 9)
                    })

                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 1, 10)
                        periodeTOMDato = LocalDate.of(2018, 1, 12)
                    })
                }
            }

            PeriodLogicRuleChain.OVERLAPPING_PERIODS(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule GAP_BETWEEN_PERIODS, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 1, 1)
                        periodeTOMDato = LocalDate.of(2018, 1, 3)
                    })

                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 1, 3)
                        periodeTOMDato = LocalDate.of(2018, 1, 9)
                    })

                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 1, 20)
                        periodeTOMDato = LocalDate.of(2018, 1, 15)
                    })

                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 1, 9)
                        periodeTOMDato = LocalDate.of(2018, 1, 10)
                    })
                }
            }

            PeriodLogicRuleChain.GAP_BETWEEN_PERIODS(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule GAP_BETWEEN_PERIODS, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 1, 1)
                        periodeTOMDato = LocalDate.of(2018, 2, 1)
                    })

                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 2, 1)
                        periodeTOMDato = LocalDate.of(2018, 5, 1)
                    })
                }
            }

            PeriodLogicRuleChain.GAP_BETWEEN_PERIODS(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule BACKDATED_MORE_THEN_8_DAYS_AND_UNDER_1_YEAR_BACKDATED, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    kontaktDato = LocalDate.of(2018, 1, 9)
                }
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 1, 1)
                        periodeTOMDato = LocalDate.of(2018, 2, 1)
                    })
                }
                syketilfelleStartDato = LocalDate.of(2018, 1, 1)
            }

            PeriodLogicRuleChain.BACKDATED_MORE_THEN_8_DAYS_AND_UNDER_1_YEAR_BACKDATED(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule BACKDATED_MORE_THEN_8_DAYS_AND_UNDER_1_YEAR_BACKDATED, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    kontaktDato = LocalDate.of(2018, 1, 8)
                }
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2018, 1, 1)
                        periodeTOMDato = LocalDate.of(2018, 2, 1)
                    })
                }
                syketilfelleStartDato = LocalDate.of(2018, 1, 1)
            }

            PeriodLogicRuleChain.BACKDATED_MORE_THEN_8_DAYS_AND_UNDER_1_YEAR_BACKDATED(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule BACKDATED_MORE_THEN_3_YEARS, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now().minusYears(3).minusDays(1)
                }
            }

            PeriodLogicRuleChain.BACKDATED_MORE_THEN_3_YEARS(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule BACKDATED_MORE_THEN_3_YEARS, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now().minusYears(2)
                }
            }

            PeriodLogicRuleChain.BACKDATED_MORE_THEN_3_YEARS(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule BACKDATED_WITH_REASON, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now().minusYears(2)
                    begrunnIkkeKontakt = "Noe tull skjedde, med sykmeldingen"
                }
            }

            PeriodLogicRuleChain.BACKDATED_WITH_REASON(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule BACKDATED_WITH_REASON, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now().minusYears(3)
                }
            }

            PeriodLogicRuleChain.BACKDATED_WITH_REASON(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule PRE_DATED, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now().plusDays(31)
                        periodeTOMDato = LocalDate.now().plusDays(37)
                    })
                }
            }

            PeriodLogicRuleChain.PRE_DATED(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule PRE_DATED, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now().plusDays(29)
                        periodeTOMDato = LocalDate.now().plusDays(31)
                    })
                }
            }

            PeriodLogicRuleChain.PRE_DATED(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule END_DATE, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now().plusYears(1)
                    })
                }
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now().minusDays(1)
                }
            }

            PeriodLogicRuleChain.END_DATE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule END_DATE, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now().plusYears(1)
                    })
                }
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now()
                }
            }

            PeriodLogicRuleChain.END_DATE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule RECEIVED_DATE_BEFORE_PROCESSED_DATE, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now().plusHours(3)
                }
            }

            PeriodLogicRuleChain.RECEIVED_DATE_BEFORE_PROCESSED_DATE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule RECEIVED_DATE_BEFORE_PROCESSED_DATE, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now().plusHours(2)
                }
            }

            PeriodLogicRuleChain.RECEIVED_DATE_BEFORE_PROCESSED_DATE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule PENDING_SICK_LEAVE_COMBINED, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now().plusDays(5)
                        avventendeSykmelding = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AvventendeSykmelding().apply {
                            innspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                        }
                    })

                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now().plusDays(5)
                        periodeTOMDato = LocalDate.now().plusDays(10)
                    })
                }
            }

            PeriodLogicRuleChain.PENDING_SICK_LEAVE_COMBINED(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule PENDING_SICK_LEAVE_COMBINED, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now().plusDays(5)
                        avventendeSykmelding = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AvventendeSykmelding().apply {
                            innspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                        }
                    })
                }
            }

            PeriodLogicRuleChain.PENDING_SICK_LEAVE_COMBINED(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule MISSING_INSPILL_TIL_ARBEIDSGIVER, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now().plusDays(5)
                        avventendeSykmelding = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AvventendeSykmelding().apply {
                        }
                    })
                }
            }

            PeriodLogicRuleChain.MISSING_INSPILL_TIL_ARBEIDSGIVER(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule MISSING_INSPILL_TIL_ARBEIDSGIVER, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now().plusDays(5)
                        avventendeSykmelding = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AvventendeSykmelding().apply {
                            innspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                        }
                    })
                }
            }

            PeriodLogicRuleChain.MISSING_INSPILL_TIL_ARBEIDSGIVER(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule PENDING_PERIOD_OUTSIDE_OF_EMPLOYER_PAID, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now().plusDays(17)
                        avventendeSykmelding = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AvventendeSykmelding().apply {
                            innspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                        }
                    })
                }
            }

            PeriodLogicRuleChain.PENDING_PERIOD_OUTSIDE_OF_EMPLOYER_PAID(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule PENDING_PERIOD_OUTSIDE_OF_EMPLOYER_PAID, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now().plusDays(16)
                        avventendeSykmelding = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AvventendeSykmelding().apply {
                            innspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                        }
                    })
                }
            }

            PeriodLogicRuleChain.PENDING_PERIOD_OUTSIDE_OF_EMPLOYER_PAID(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule TOO_MANY_TREATMENT_DAYS, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2019, 1, 7)
                        periodeTOMDato = LocalDate.of(2019, 1, 13)
                        behandlingsdager = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.Behandlingsdager().apply {
                            antallBehandlingsdagerUke = 2
                        }
                    })
                }
            }

            PeriodLogicRuleChain.TOO_MANY_TREATMENT_DAYS(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule TOO_MANY_TREATMENT_DAYS, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.of(2019, 1, 7)
                        periodeTOMDato = LocalDate.of(2019, 1, 13)
                        behandlingsdager = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.Behandlingsdager().apply {
                            antallBehandlingsdagerUke = 1
                        }
                    })
                }
            }

            PeriodLogicRuleChain.TOO_MANY_TREATMENT_DAYS(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule PARTIAL_SICK_LEAVE_PERCENTAGE_TO_LOW, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now()
                        gradertSykmelding = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.GradertSykmelding().apply {
                            sykmeldingsgrad = 19
                        }
                    })
                }
            }

            PeriodLogicRuleChain.PARTIAL_SICK_LEAVE_PERCENTAGE_TO_LOW(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule PARTIAL_SICK_LEAVE_PERCENTAGE_TO_LOW, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now()
                        gradertSykmelding = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.GradertSykmelding().apply {
                            sykmeldingsgrad = 20
                        }
                    })
                }
            }

            PeriodLogicRuleChain.PARTIAL_SICK_LEAVE_PERCENTAGE_TO_LOW(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule PARTIAL_SICK_LEAVE_TOO_HIGH_PERCENTAGE, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now()
                        gradertSykmelding = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.GradertSykmelding().apply {
                            sykmeldingsgrad = 100
                        }
                    })
                }
            }

            PeriodLogicRuleChain.PARTIAL_SICK_LEAVE_TOO_HIGH_PERCENTAGE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule PARTIAL_SICK_LEAVE_TOO_HIGH_PERCENTAGE, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now()
                        gradertSykmelding = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.GradertSykmelding().apply {
                            sykmeldingsgrad = 99
                        }
                    })
                }
            }

            PeriodLogicRuleChain.PARTIAL_SICK_LEAVE_TOO_HIGH_PERCENTAGE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule BACKDATING_SYKMELDING_EXTENSION, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now()
                    })
                }
            }

            PeriodLogicRuleChain.BACKDATING_SYKMELDING_EXTENSION(ruleData(healthInformation, signatureDate = LocalDateTime.now().minusMonths(1).minusDays(1))) shouldEqual true
        }

        it("Should check rule BACKDATING_SYKMELDING_EXTENSION, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now()
                    })
                }
            }

            PeriodLogicRuleChain.BACKDATING_SYKMELDING_EXTENSION(ruleData(healthInformation, signatureDate = LocalDateTime.now().minusMonths(1))) shouldEqual false
        }
    }
})
