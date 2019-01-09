package no.nav.syfo.rules

import no.nav.helse.sm2013.CV
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.helse.sm2013.Ident
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.executeFlow
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime

object PeriodLogicRuleChainSpek : Spek({

    fun deafaultHelseOpplysningerArbeidsuforhet() = HelseOpplysningerArbeidsuforhet().apply {
        aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
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
        }
    }

    describe("Testing validation rules and checking the rule outcomes") {

        it("Should check rule 1110, should trigger rule") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                behandletDato = LocalDateTime.now()
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now().minusDays(2))

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.SIGNATURE_DATE_AFTER_RECEIVED_DATE } shouldEqual true
        }

        it("Should check rule 1200, should trigger rule") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.NO_PERIOD_PROVIDED } shouldEqual true
        }

        it("Should check rule 1201, should trigger rule") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                    periodeFOMDato = LocalDate.of(2018, 1, 9)
                    periodeTOMDato = LocalDate.of(2018, 1, 7)
                })
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.TO_DATE_BEFORE_FROM_DATE } shouldEqual true
        }

        it("Should check rule 1201, should NOT trigger rule") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                    periodeFOMDato = LocalDate.of(2018, 1, 7)
                    periodeTOMDato = LocalDate.of(2018, 1, 9)
                })
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.TO_DATE_BEFORE_FROM_DATE } shouldEqual false
        }

        it("Should check rule 1202, should trigger rule") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                    periodeFOMDato = LocalDate.of(2018, 1, 7)
                    periodeTOMDato = LocalDate.of(2018, 1, 9)
                })

                periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                    periodeFOMDato = LocalDate.of(2018, 1, 8)
                    periodeTOMDato = LocalDate.of(2018, 1, 12)
                })
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.OVERLAPPING_PERIODS } shouldEqual true
        }

        it("Should check rule 1203, should trigger rule") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                    periodeFOMDato = LocalDate.of(2018, 1, 1)
                    periodeTOMDato = LocalDate.of(2018, 2, 1)
                })

                periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                    periodeFOMDato = LocalDate.of(2018, 4, 1)
                    periodeTOMDato = LocalDate.of(2018, 5, 1)
                })
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.GAP_BETWEEN_PERIODS } shouldEqual true
        }

        it("Should check rule 1203, should NOT trigger rule") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                    periodeFOMDato = LocalDate.of(2018, 1, 1)
                    periodeTOMDato = LocalDate.of(2018, 2, 1)
                })

                periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                    periodeFOMDato = LocalDate.of(2018, 2, 1)
                    periodeTOMDato = LocalDate.of(2018, 5, 1)
                })
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.GAP_BETWEEN_PERIODS } shouldEqual false
        }

        it("Should check rule 1204, should trigger rule") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                behandletDato = LocalDateTime.now().minusDays(8)
                kontaktDato = LocalDate.of(2018, 5, 1)
            }

            val ruleMetadata = RuleMetadata(receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now())

            val periodLogicRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    PeriodLogicRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            periodLogicRuleChainResults.any { it == PeriodLogicRuleChain.FIRST_TIME_BACKDATED_MORE_THEN_8_DAYS } shouldEqual true
        }
    }
})