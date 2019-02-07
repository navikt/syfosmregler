package no.nav.syfo.rules

import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.executeFlow
import no.nhn.schemas.reg.common.no.Kode
import no.nhn.schemas.reg.hprv2.ArrayOfGodkjenning
import no.nhn.schemas.reg.hprv2.Godkjenning
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object HPRRuleChainSpek : Spek({

    describe("Testing validation rules and checking the rule outcomes") {

        it("Should check rule BEHANDLER_NOT_VALDIG_IN_HPR, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet()
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

            val hprRuleChainResults = HPRRuleChain.values().toList().executeFlow(healthInformation, person)

            hprRuleChainResults.any { it == HPRRuleChain.BEHANDLER_NOT_VALDIG_IN_HPR } shouldEqual true
        }

        it("Should check rule BEHANDLER_NOT_VALDIG_IN_HPR, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet()
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

            val hprRuleChainResults = HPRRuleChain.values().toList().executeFlow(healthInformation, person)

            hprRuleChainResults.any { it == HPRRuleChain.BEHANDLER_NOT_VALDIG_IN_HPR } shouldEqual false
        }

        it("Should check rule BEHANDLER_NOT_IN_HPR, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet()
            val person = no.nhn.schemas.reg.hprv2.Person()

            val hprRuleChainResults = HPRRuleChain.values().toList().executeFlow(healthInformation, person)

            hprRuleChainResults.any { it == HPRRuleChain.BEHANDLER_NOT_IN_HPR } shouldEqual true
        }

        it("Should check rule BEHANDLER_NOT_IN_HPR, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet()
            val person = no.nhn.schemas.reg.hprv2.Person().apply {
                nin = "1234324"
            }

            val hprRuleChainResults = HPRRuleChain.values().toList().executeFlow(healthInformation, person)

            hprRuleChainResults.any { it == HPRRuleChain.BEHANDLER_NOT_IN_HPR } shouldEqual false
        }

        it("Should check rule BEHANDLER_NOT_VALID_AUTHORIZATION_IN_HPR, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet()
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

            val hprRuleChainResults = HPRRuleChain.values().toList().executeFlow(healthInformation, person)

            hprRuleChainResults.any { it == HPRRuleChain.BEHANDLER_NOT_VALID_AUTHORIZATION_IN_HPR } shouldEqual true
        }

        it("Should check rule BEHANDLER_NOT_LE_KI_MT_TL_IN_HPR, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet()
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

            val hprRuleChainResults = HPRRuleChain.values().toList().executeFlow(healthInformation, person)

            hprRuleChainResults.any { it == HPRRuleChain.BEHANDLER_NOT_LE_KI_MT_TL_IN_HPR } shouldEqual true
        }

        it("Should check rule BEHANDLER_NOT_LE_KI_MT_TL_IN_HPR, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet()
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

            val hprRuleChainResults = HPRRuleChain.values().toList().executeFlow(healthInformation, person)

            hprRuleChainResults.any { it == HPRRuleChain.BEHANDLER_NOT_LE_KI_MT_TL_IN_HPR } shouldEqual false
        }
    }
})