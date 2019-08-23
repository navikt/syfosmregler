package no.nav.syfo.rules

import java.time.LocalDate
import no.nav.syfo.api.Behandler
import no.nav.syfo.api.Godkjenning
import no.nav.syfo.api.Kode
import no.nav.syfo.generateMedisinskVurdering
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.Sykmelding
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object HPRRuleChainSpek : Spek({

    describe("Testing validation rules and checking the rule outcomes") {
        fun ruleData(healthInformation: Sykmelding, behandler: Behandler) =
                RuleData(healthInformation, behandler)

        it("Should check rule BEHANDLER_IKKE_GYLDIG_I_HPR, should trigger rule") {
            val healthInformation = generateSykmelding()
            val behandler = Behandler(listOf(Godkjenning(
                autorisasjon = Kode(
                    aktiv = false,
                    oid = 7704,
                    verdi = "1")
            )))

            HPRRuleChain.BEHANDLER_IKKE_GYLDIG_I_HPR(ruleData(healthInformation, behandler)) shouldEqual true
        }

        it("Should check rule BEHANDLER_IKKE_GYLDIG_I_HPR, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val behandler = Behandler(listOf(Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 7704,
                    verdi = "1")
            )))

            HPRRuleChain.BEHANDLER_IKKE_GYLDIG_I_HPR(ruleData(healthInformation, behandler)) shouldEqual false
        }

        it("Should check rule BEHANDLER_NOT_VALID_AUTHORIZATION_IN_HPR, should trigger rule") {
            val healthInformation = generateSykmelding()
            val behandler = Behandler(listOf(Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 7704,
                    verdi = "5")
            )))

            HPRRuleChain.BEHANDLER_MANGLER_AUTORISASJON_I_HPR(ruleData(healthInformation, behandler)) shouldEqual true
        }

        it("Should check rule BEHANDLER_MANGLER_AUTORISASJON_I_HPR, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val behandler = Behandler(listOf(Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 7704,
                    verdi = "1")
            )))

            HPRRuleChain.BEHANDLER_MANGLER_AUTORISASJON_I_HPR(ruleData(healthInformation, behandler)) shouldEqual false
        }

        it("Should check rule BEHANDLER_MANGLER_AUTORISASJON_I_HPR, should trigger rule") {
            val healthInformation = generateSykmelding()
            val behandler = Behandler(listOf(Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = ""
                ),
                helsepersonellkategori = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = "PL"
                )
            )))

            HPRRuleChain.BEHANDLER_MANGLER_AUTORISASJON_I_HPR(ruleData(healthInformation, behandler)) shouldEqual true
        }

        it("Should check rule BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val behandler = Behandler(listOf(Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = ""
                ),
                helsepersonellkategori = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = HelsepersonellKategori.LEGE.verdi
                )
            )))

            HPRRuleChain.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR(ruleData(healthInformation, behandler)) shouldEqual false
        }

        it("Should check rule BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L, should trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7170", kode = "S82")
            ))
            val behandler = Behandler(listOf(Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = ""
                ),
                helsepersonellkategori = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                )
            )))

            HPRRuleChain.BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(ruleData(healthInformation, behandler)) shouldEqual true
        }

        it("Should check rule BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L, should NOT trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                            hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7170", kode = "L02")
                    ))
            val behandler = Behandler(listOf(Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = ""
                ),
                helsepersonellkategori = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                )
            )))

            HPRRuleChain.BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(ruleData(healthInformation, behandler)) shouldEqual false
        }

        it("Should check rule BEHANDLER_MT_OR_FT_OR_KI_OVER_12_WEEKS, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 1),
                            tom = LocalDate.of(2019, 3, 27)
                    )
            ))

            val behandler = Behandler(listOf(Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = ""
                ),
                helsepersonellkategori = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                )
            )))

            HPRRuleChain.BEHANDLER_MT_FT_KI_OVER_12_UKER(ruleData(healthInformation, behandler)) shouldEqual true
        }

        it("Should check rule BEHANDLER_MT_OR_FT_OR_KI_OVER_12_WEEKS, should NOT trigger rule, when helsepersoner is Lege(LE) and Kiropratkor(KI)") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 1),
                            tom = LocalDate.of(2019, 3, 27)
                    )
            ))

            val behandler = Behandler(listOf(Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = ""
                ),
                helsepersonellkategori = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                )
            ), Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = ""
                ),
                helsepersonellkategori = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = HelsepersonellKategori.LEGE.verdi
                )
            )))

            HPRRuleChain.BEHANDLER_MT_FT_KI_OVER_12_UKER(ruleData(healthInformation, behandler)) shouldEqual false
        }

        it("Should check rule BEHANDLER_MT_FT_KI_OVER_12_UKER, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 1),
                            tom = LocalDate.of(2019, 3, 26)
                    )
            ))

            val behandler = Behandler(listOf(Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = ""
                ),
                helsepersonellkategori = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                )
            )))

            HPRRuleChain.BEHANDLER_MT_FT_KI_OVER_12_UKER(ruleData(healthInformation, behandler)) shouldEqual false
        }

        it("Should check rule BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L, should trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7170", kode = "A92")
            ))

            val behandler = Behandler(listOf(Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = ""
                ),
                helsepersonellkategori = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                )
            )))

            HPRRuleChain.BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(ruleData(healthInformation, behandler)) shouldEqual true
        }

        it("Should check rule BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L, should NOT trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7170", kode = "L02")
            ))

            val behandler = Behandler(listOf(Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = ""
                ),
                helsepersonellkategori = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                )
            ), Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = ""
                ),
                helsepersonellkategori = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = HelsepersonellKategori.LEGE.verdi
                )
            )))

            HPRRuleChain.BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(ruleData(healthInformation, behandler)) shouldEqual false
        }

        it("Should check rule BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L, should NOT trigger rule") {
            val healthInformation = generateSykmelding(medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(system = "2.16.578.1.12.4.1.1.7170", kode = "L02")
            ))

            val behandler = Behandler(listOf(Godkjenning(
                autorisasjon = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = ""
                ),
                helsepersonellkategori = Kode(
                    aktiv = true,
                    oid = 0,
                    verdi = HelsepersonellKategori.KIROPRAKTOR.verdi
                )
            )))

            HPRRuleChain.BEHANDLER_KI_FT_MT_BENYTTER_ANNEN_DIAGNOSEKODE_ENN_L(ruleData(healthInformation, behandler)) shouldEqual false
        }
    }
})
