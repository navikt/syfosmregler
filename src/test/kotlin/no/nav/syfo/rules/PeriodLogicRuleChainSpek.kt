package no.nav.syfo.rules

import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.executeFlow
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime

object PeriodLogicRuleChainSpek : Spek({

    describe("Testing validation rules and checking the rule outcomes") {

        it("Should check rule SIGNATURE_DATE_AFTER_RECEIVED_DATE, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now()
                }
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now().minusDays(2))

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.SIGNATURE_DATE_AFTER_RECEIVED_DATE } shouldEqual true
        }

        it("Should check rule SIGNATURE_DATE_AFTER_RECEIVED_DATE, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now()
                }
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.SIGNATURE_DATE_AFTER_RECEIVED_DATE } shouldEqual false
        }

        it("Should check rule NO_PERIOD_PROVIDED, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {}

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.NO_PERIOD_PROVIDED } shouldEqual true
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.NO_PERIOD_PROVIDED } shouldEqual false
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.TO_DATE_BEFORE_FROM_DATE } shouldEqual true
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.TO_DATE_BEFORE_FROM_DATE } shouldEqual false
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.OVERLAPPING_PERIODS } shouldEqual true
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.OVERLAPPING_PERIODS } shouldEqual false
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.GAP_BETWEEN_PERIODS } shouldEqual true
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.GAP_BETWEEN_PERIODS } shouldEqual false
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.BACKDATED_MORE_THEN_8_DAYS_AND_UNDER_1_YEAR_BACKDATED } shouldEqual true
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.BACKDATED_MORE_THEN_8_DAYS_AND_UNDER_1_YEAR_BACKDATED } shouldEqual false
        }

        it("Should check rule BACKDATED_MORE_THEN_3_YEARS, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now().minusYears(3).minusDays(1)
                }
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.BACKDATED_MORE_THEN_3_YEARS } shouldEqual true
        }

        it("Should check rule BACKDATED_MORE_THEN_3_YEARS, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now().minusYears(2)
                }
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.BACKDATED_MORE_THEN_3_YEARS } shouldEqual false
        }

        it("Should check rule BACKDATED_WITH_REASON, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now().minusYears(2)
                    begrunnIkkeKontakt = "Noe tull skjedde, med sykmeldingen"
                }
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.BACKDATED_WITH_REASON } shouldEqual true
        }

        it("Should check rule BACKDATED_WITH_REASON, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now().minusYears(3)
                }
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.BACKDATED_WITH_REASON } shouldEqual false
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.PRE_DATED } shouldEqual true
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.PRE_DATED } shouldEqual false
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.END_DATE } shouldEqual true
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.END_DATE } shouldEqual false
        }

        it("Should check rule RECEIVED_DATE_BEFORE_PROCESSED_DATE, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now().plusHours(3)
                }
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.RECEIVED_DATE_BEFORE_PROCESSED_DATE } shouldEqual true
        }

        it("Should check rule RECEIVED_DATE_BEFORE_PROCESSED_DATE, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                    behandletDato = LocalDateTime.now().plusHours(2)
                }
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.RECEIVED_DATE_BEFORE_PROCESSED_DATE } shouldEqual false
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.PENDING_SICK_LEAVE_COMBINED } shouldEqual true
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.PENDING_SICK_LEAVE_COMBINED } shouldEqual false
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.MISSING_INSPILL_TIL_ARBEIDSGIVER } shouldEqual true
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.MISSING_INSPILL_TIL_ARBEIDSGIVER } shouldEqual false
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.PENDING_PERIOD_OUTSIDE_OF_EMPLOYER_PAID } shouldEqual true
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.PENDING_PERIOD_OUTSIDE_OF_EMPLOYER_PAID } shouldEqual false
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.TOO_MANY_TREATMENT_DAYS } shouldEqual true
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.TOO_MANY_TREATMENT_DAYS } shouldEqual false
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.PARTIAL_SICK_LEAVE_PERCENTAGE_TO_LOW } shouldEqual true
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.PARTIAL_SICK_LEAVE_PERCENTAGE_TO_LOW } shouldEqual false
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.PARTIAL_SICK_LEAVE_TOO_HIGH_PERCENTAGE } shouldEqual true
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.PARTIAL_SICK_LEAVE_TOO_HIGH_PERCENTAGE } shouldEqual false
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now().minusMonths(1).minusDays(1))

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.BACKDATING_SYKMELDING_EXTENSION } shouldEqual true
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

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now().minusMonths(1))

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.BACKDATING_SYKMELDING_EXTENSION } shouldEqual false
        }
    }
})