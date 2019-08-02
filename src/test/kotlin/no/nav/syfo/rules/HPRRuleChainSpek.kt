package no.nav.syfo.rules

import java.time.LocalDate
import no.nav.syfo.generateMedisinskVurdering
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.Sykmelding
import no.nhn.schemas.reg.common.no.Kode
import no.nhn.schemas.reg.hprv2.ArrayOfGodkjenning
import no.nhn.schemas.reg.hprv2.Godkjenning
import no.nhn.schemas.reg.hprv2.Person as HPRPerson
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object HPRRuleChainSpek : Spek({

    describe("Testing validation rules and checking the rule outcomes") {
        fun ruleData(healthInformation: Sykmelding, person: HPRPerson) =
                RuleData(healthInformation, person)

        it("Should check rule BEHANDLER_IKKE_GYLDIG_I_HPR, should trigger rule") {
            val healthInformation = generateSykmelding()
            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            isAktiv = false
                            oid = 7704
                            verdi = "1"
                        }
                    })
                }
            }

            HPRRuleChain.BEHANDLER_IKKE_GYLDIG_I_HPR(ruleData(healthInformation, person)) shouldEqual true
        }

        it("Should check rule BEHANDLER_IKKE_GYLDIG_I_HPR, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            isAktiv = true
                            oid = 7704
                            verdi = "1"
                        }
                    })
                }
            }

            HPRRuleChain.BEHANDLER_IKKE_GYLDIG_I_HPR(ruleData(healthInformation, person)) shouldEqual false
        }

        it("Should check rule BEHANDLER_NOT_VALID_AUTHORIZATION_IN_HPR, should trigger rule") {
            val healthInformation = generateSykmelding()
            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            isAktiv = true
                            oid = 7704
                            verdi = "5"
                        }
                    })
                }
            }

            HPRRuleChain.BEHANDLER_MANGLER_AUTORISASJON_I_HPR(ruleData(healthInformation, person)) shouldEqual true
        }

        it("Should check rule BEHANDLER_MANGLER_AUTORISASJON_I_HPR, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val person = no.nhn.schemas.reg.hprv2.Person()

            HPRRuleChain.BEHANDLER_MANGLER_AUTORISASJON_I_HPR(ruleData(healthInformation, person)) shouldEqual false
        }

        it("Should check rule BEHANDLER_MANGLER_AUTORISASJON_I_HPR, should trigger rule") {
            val healthInformation = generateSykmelding()
            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            helsepersonellkategori = Kode().apply {
                                isAktiv = true
                                verdi = "PL"
                            }
                            isAktiv = true
                        }
                    })
                }
            }

            HPRRuleChain.BEHANDLER_MANGLER_AUTORISASJON_I_HPR(ruleData(healthInformation, person)) shouldEqual true
        }

        it("Should check rule BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            helsepersonellkategori = Kode().apply {
                                isAktiv = true
                                verdi = HelsepersonellKategori.LEGE.verdi
                            }
                            isAktiv = true
                        }
                    })
                }
            }

            HPRRuleChain.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR(ruleData(healthInformation, person)) shouldEqual false
        }

        it("Should check rule BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L, should trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7170", kode = "S82")
            ))
            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            helsepersonellkategori = Kode().apply {
                                isAktiv = true
                                verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                            }
                            isAktiv = true
                        }
                    })
                }
            }

            HPRRuleChain.BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(ruleData(healthInformation, person)) shouldEqual true
        }

        it("Should check rule BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L, should NOT trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                            hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7170", kode = "L02")
                    ))
            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            helsepersonellkategori = Kode().apply {
                                isAktiv = true
                                verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                            }
                            isAktiv = true
                        }
                    })
                }
            }

            HPRRuleChain.BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(ruleData(healthInformation, person)) shouldEqual false
        }

        it("Should check rule BEHANDLER_MT_OR_FT_OR_KI_OVER_12_WEEKS, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 1),
                            tom = LocalDate.of(2019, 3, 27)
                    )
            ))

            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            helsepersonellkategori = Kode().apply {
                                isAktiv = true
                                verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                            }
                            isAktiv = true
                        }
                    })
                }
            }

            HPRRuleChain.BEHANDLER_MT_FT_KI_OVER_12_UKER(ruleData(healthInformation, person)) shouldEqual true
        }

        it("Should check rule BEHANDLER_MT_OR_FT_OR_KI_OVER_12_WEEKS, should NOT trigger rule, when helsepersoner is Lege(LE) and Kiropratkor(KI)") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 1),
                            tom = LocalDate.of(2019, 3, 27)
                    )
            ))

            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            helsepersonellkategori = Kode().apply {
                                isAktiv = true
                                verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                            }
                            isAktiv = true
                        }
                    })

                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            helsepersonellkategori = Kode().apply {
                                isAktiv = true
                                verdi = HelsepersonellKategori.LEGE.verdi
                            }
                            isAktiv = true
                        }
                    })
                }
            }

            HPRRuleChain.BEHANDLER_MT_FT_KI_OVER_12_UKER(ruleData(healthInformation, person)) shouldEqual false
        }

        it("Should check rule BEHANDLER_MT_FT_KI_OVER_12_UKER, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 1),
                            tom = LocalDate.of(2019, 3, 26)
                    )
            ))

            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            helsepersonellkategori = Kode().apply {
                                isAktiv = true
                                verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                            }
                            isAktiv = true
                        }
                    })
                }
            }

            HPRRuleChain.BEHANDLER_MT_FT_KI_OVER_12_UKER(ruleData(healthInformation, person)) shouldEqual false
        }

        it("Should check rule BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L, should trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7170", kode = "A92")
            ))

            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            helsepersonellkategori = Kode().apply {
                                isAktiv = true
                                verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                            }
                            isAktiv = true
                        }
                    })
                }
            }

            HPRRuleChain.BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(ruleData(healthInformation, person)) shouldEqual true
        }

        it("Should check rule BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L, should NOT trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7170", kode = "L02")
            ))

            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            helsepersonellkategori = Kode().apply {
                                isAktiv = true
                                verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                            }
                            isAktiv = true
                        }
                    })
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            helsepersonellkategori = Kode().apply {
                                isAktiv = true
                                verdi = HelsepersonellKategori.LEGE.verdi
                            }
                            isAktiv = true
                        }
                    })
                }
            }

            HPRRuleChain.BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(ruleData(healthInformation, person)) shouldEqual false
        }

        it("Should check rule BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L, should NOT trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7170", kode = "L02")
            ))

            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            helsepersonellkategori = Kode().apply {
                                isAktiv = true
                                verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                            }
                            isAktiv = true
                        }
                    })
                }
            }

            HPRRuleChain.BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(ruleData(healthInformation, person)) shouldEqual false
        }
    }
})
