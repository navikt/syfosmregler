package no.nav.syfo.rules

import no.nav.syfo.api.Godkjenning
import no.nav.syfo.api.Kode
import no.nav.syfo.api.Lege
import no.nav.syfo.generateMedisinskVurdering
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.Sykmelding
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

object HPRRuleChainSpek : Spek({

    describe("Testing validation rules and checking the rule outcomes") {
        fun ruleData(healthInformation: Sykmelding, person: Lege) =
                RuleData(healthInformation, person)

        it("Should check rule BEHANDLER_IKKE_GYLDIG_I_HPR, should trigger rule") {
            val healthInformation = generateSykmelding()
            val lege = Lege(godkjenninger = listOf(
                Godkjenning(
                    autorisasjon = Kode(
                        aktiv = false,
                        oid = 7704,
                        verdi = "1"
                    )
                )
            ))

            HPRRuleChain.BEHANDLER_IKKE_GYLDIG_I_HPR(ruleData(healthInformation, lege)) shouldEqual true
        }

        it("Should check rule BEHANDLER_IKKE_GYLDIG_I_HPR, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val lege = Lege(godkjenninger = listOf(
                Godkjenning(
                    autorisasjon = Kode(
                        aktiv = true,
                        oid = 7704,
                        verdi = "1"
                    )
                )
            ))

            HPRRuleChain.BEHANDLER_IKKE_GYLDIG_I_HPR(ruleData(healthInformation, lege)) shouldEqual false
        }

        it("Should check rule BEHANDLER_NOT_VALID_AUTHORIZATION_IN_HPR, should trigger rule") {
            val healthInformation = generateSykmelding()
            val lege = Lege(godkjenninger = listOf(
                Godkjenning(
                    autorisasjon = Kode(
                        aktiv = true,
                        oid = 7704,
                        verdi = "5"
                    )
                )
            ))

            HPRRuleChain.BEHANDLER_MANGLER_AUTORISASJON_I_HPR(ruleData(healthInformation, lege)) shouldEqual true
        }

        it("Lege uten godkjenning, skal trigge regel") {
            val healthInformation = generateSykmelding()
            val lege = Lege()

            HPRRuleChain.BEHANDLER_MANGLER_AUTORISASJON_I_HPR(ruleData(healthInformation, lege)) shouldEqual true
        }

        it("Should check rule BEHANDLER_MANGLER_AUTORISASJON_I_HPR, should trigger rule") {
            val healthInformation = generateSykmelding()
            val lege = Lege(godkjenninger = listOf(
                Godkjenning(
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        verdi = "PL"
                    ),
                    autorisasjon = Kode(
                        aktiv = true,
                        verdi = ""
                    )
                )
            ))

            HPRRuleChain.BEHANDLER_MANGLER_AUTORISASJON_I_HPR(ruleData(healthInformation, lege)) shouldEqual true
        }

        it("Should check rule BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val lege = Lege(godkjenninger = listOf(
                Godkjenning(
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        verdi = "LE"
                    ),
                    autorisasjon = Kode(
                        aktiv = true,
                        verdi = ""
                    )
                )
            ))

            HPRRuleChain.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR(ruleData(healthInformation, lege)) shouldEqual false
        }

        it("Should check rule BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L, should trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7170", kode = "S82")
            ))
            val lege = Lege(godkjenninger = listOf(
                Godkjenning(
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        verdi = "KI"
                    ),
                    autorisasjon = Kode(
                        aktiv = true,
                        verdi = ""
                    )
                )
            ))

            HPRRuleChain.BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(ruleData(healthInformation, lege)) shouldEqual true
        }

        it("Should check rule BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L, should NOT trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                            hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7170", kode = "L02")
                    ))
            val lege = Lege(godkjenninger = listOf(
                Godkjenning(
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        verdi = "KI"
                    ),
                    autorisasjon = Kode(
                        aktiv = true,
                        verdi = ""
                    )
                )
            ))

            HPRRuleChain.BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(ruleData(healthInformation, lege)) shouldEqual false
        }

        it("Should check rule BEHANDLER_MT_OR_FT_OR_KI_OVER_12_WEEKS, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 1),
                            tom = LocalDate.of(2019, 3, 27)
                    )
            ))

            val lege = Lege(godkjenninger = listOf(
                Godkjenning(
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        verdi = "KI"
                    ),
                    autorisasjon = Kode(
                        aktiv = true,
                        verdi = ""
                    )
                )
            ))

            HPRRuleChain.BEHANDLER_MT_FT_KI_OVER_12_UKER(ruleData(healthInformation, lege)) shouldEqual true
        }

        it("Should check rule BEHANDLER_MT_FT_KI_OVER_12_UKER, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 1),
                            tom = LocalDate.of(2019, 3, 26)
                    )
            ))

            val lege = Lege(godkjenninger = listOf(
                Godkjenning(
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        verdi = "KI"
                    ),
                    autorisasjon = Kode(
                        aktiv = true,
                        verdi = ""
                    )
                )
            ))

            HPRRuleChain.BEHANDLER_MT_FT_KI_OVER_12_UKER(ruleData(healthInformation, lege)) shouldEqual false
        }

        it("Should check rule BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L, should trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7170", kode = "A92")
            ))

            val lege = Lege(godkjenninger = listOf(
                Godkjenning(
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        verdi = "KI"
                    ),
                    autorisasjon = Kode(
                        aktiv = true,
                        verdi = ""
                    )
                )
            ))

            HPRRuleChain.BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(ruleData(healthInformation, lege)) shouldEqual true
        }

        it("Should check rule BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L, should NOT trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7170", kode = "L02")
            ))

            val lege = Lege(godkjenninger = listOf(
                Godkjenning(
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        verdi = "KI"
                    ),
                    autorisasjon = Kode(
                        aktiv = true,
                        verdi = ""
                    )
                )
            ))

            HPRRuleChain.BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(ruleData(healthInformation, lege)) shouldEqual false
        }
    }
})
