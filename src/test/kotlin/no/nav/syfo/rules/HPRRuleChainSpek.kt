package no.nav.syfo.rules

import no.nav.syfo.RuleData
import no.nav.syfo.generateSykmelding
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

        it("Should check rule BEHANDLER_NOT_VALDIG_IN_HPR, should trigger rule") {
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

            HPRRuleChain.BEHANDLER_NOT_VALDIG_IN_HPR(ruleData(healthInformation, person)) shouldEqual true
        }

        it("Should check rule BEHANDLER_NOT_VALDIG_IN_HPR, should NOT trigger rule") {
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

            HPRRuleChain.BEHANDLER_NOT_VALDIG_IN_HPR(ruleData(healthInformation, person)) shouldEqual false
        }

        it("Should check rule BEHANDLER_NOT_IN_HPR, should trigger rule") {
            val healthInformation = generateSykmelding()
            val person = HPRPerson()

            HPRRuleChain.BEHANDLER_NOT_IN_HPR(ruleData(healthInformation, person)) shouldEqual true
        }

        it("Should check rule BEHANDLER_NOT_IN_HPR, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                nin = "1234324"
            }

            HPRRuleChain.BEHANDLER_NOT_IN_HPR(ruleData(healthInformation, person)) shouldEqual false
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

            HPRRuleChain.BEHANDLER_NOT_VALID_AUTHORIZATION_IN_HPR(ruleData(healthInformation, person)) shouldEqual true
        }

        it("Should check rule BEHANDLER_NOT_VALID_AUTHORIZATION_IN_HPR, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val person = no.nhn.schemas.reg.hprv2.Person()

            HPRRuleChain.BEHANDLER_NOT_VALID_AUTHORIZATION_IN_HPR(ruleData(healthInformation, person)) shouldEqual false
        }

        it("Should check rule BEHANDLER_NOT_LE_KI_MT_TL_IN_HPR, should trigger rule") {
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

            HPRRuleChain.BEHANDLER_NOT_LE_KI_MT_TL_IN_HPR(ruleData(healthInformation, person)) shouldEqual true
        }

        it("Should check rule BEHANDLER_NOT_LE_KI_MT_TL_IN_HPR, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        autorisasjon = Kode().apply {
                            helsepersonellkategori = Kode().apply {
                                isAktiv = true
                                verdi = "LE"
                            }
                            isAktiv = true
                        }
                    })
                }
            }

            HPRRuleChain.BEHANDLER_NOT_LE_KI_MT_TL_IN_HPR(ruleData(healthInformation, person)) shouldEqual false
        }
    }
})
