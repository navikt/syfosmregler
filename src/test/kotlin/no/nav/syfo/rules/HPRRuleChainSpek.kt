package no.nav.syfo.rules

import no.nav.syfo.client.Behandler
import no.nav.syfo.client.Godkjenning
import no.nav.syfo.client.Kode
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

object HPRRuleChainSpek : Spek({
    describe("Testing validation rules and checking the rule outcomes") {
        it("Should check rule BEHANDLER_IKKE_GYLDIG_I_HPR, should trigger rule") {
            val healthInformation = generateSykmelding()
            val behandler = Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon = Kode(
                            aktiv = false,
                            oid = 7702,
                            verdi = "1"
                        )
                    )
                )
            )

            HPRRuleChain(healthInformation,
                BehandlerOgStartdato(behandler, null)).getRuleByName("BEHANDLER_IKKE_GYLDIG_I_HPR")
                .executeRule().result shouldBeEqualTo true
        }

        it("Should check rule BEHANDLER_IKKE_GYLDIG_I_HPR, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val behandler = Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon = Kode(
                            aktiv = true,
                            oid = 7702,
                            verdi = "1"
                        )
                    )
                )
            )

            HPRRuleChain(
                healthInformation,
                BehandlerOgStartdato(behandler, null)
            ).getRuleByName("BEHANDLER_IKKE_GYLDIG_I_HPR").executeRule().result shouldBeEqualTo false
        }

        it("Should check rule BEHANDLER_NOT_VALID_AUTHORIZATION_IN_HPR, should trigger rule") {
            val healthInformation = generateSykmelding()
            val behandler = Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon = Kode(
                            aktiv = true,
                            oid = 7702,
                            verdi = "11"
                        )
                    )
                )
            )

            HPRRuleChain(
                healthInformation,
                BehandlerOgStartdato(behandler, null)
            ).getRuleByName("BEHANDLER_MANGLER_AUTORISASJON_I_HPR").executeRule().result shouldBeEqualTo true
        }

        it("Should check rule BEHANDLER_MANGLER_AUTORISASJON_I_HPR, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val behandler = Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon = Kode(
                            aktiv = true,
                            oid = 7704,
                            verdi = "1"
                        )
                    )
                )
            )

            HPRRuleChain(
                healthInformation,
                BehandlerOgStartdato(behandler, null)
            ).getRuleByName("BEHANDLER_MANGLER_AUTORISASJON_I_HPR").executeRule().result shouldBeEqualTo false
        }

        it("Should check rule BEHANDLER_MANGLER_AUTORISASJON_I_HPR, should trigger rule") {
            val healthInformation = generateSykmelding()
            val behandler = Behandler(
                listOf(
                    Godkjenning(
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
                    )
                )
            )

            HPRRuleChain(
                healthInformation,
                BehandlerOgStartdato(behandler, null)
            ).getRuleByName("BEHANDLER_MANGLER_AUTORISASJON_I_HPR").executeRule().result shouldBeEqualTo true
        }

        it("Should check rule BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR, should NOT trigger rule") {
            val healthInformation = generateSykmelding()
            val behandler = Behandler(
                listOf(
                    Godkjenning(
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
                    )
                )
            )

            HPRRuleChain(
                healthInformation,
                BehandlerOgStartdato(behandler, null),
            ).getRuleByName("BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR").executeRule().result shouldBeEqualTo false
        }

        it("Should check rule BEHANDLER_MT_OR_FT_OR_KI_OVER_12_WEEKS, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 1),
                        tom = LocalDate.of(2019, 3, 27)
                    )
                )
            )

            val behandler = Behandler(
                listOf(
                    Godkjenning(
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
                    )
                )
            )

            HPRRuleChain(
                healthInformation,
                BehandlerOgStartdato(behandler, null),
            ).getRuleByName("BEHANDLER_MT_FT_KI_OVER_12_UKER").executeRule().result shouldBeEqualTo true
        }

        it("Should check rule BEHANDLER_MT_OR_FT_OR_KI_OVER_12_WEEKS, should NOT trigger rule, when helsepersoner is Lege(LE) and Kiropratkor(KI)") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 1),
                        tom = LocalDate.of(2019, 3, 27)
                    )
                )
            )

            val behandler = Behandler(
                listOf(
                    Godkjenning(
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
                    ),
                    Godkjenning(
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
                    )
                )
            )

            HPRRuleChain(
                healthInformation,
                BehandlerOgStartdato(behandler, null),
            ).getRuleByName("BEHANDLER_MT_FT_KI_OVER_12_UKER").executeRule().result shouldBeEqualTo false
        }

        it("Should check rule BEHANDLER_MT_FT_KI_OVER_12_UKER, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 1),
                        tom = LocalDate.of(2019, 3, 26)
                    )
                )
            )

            val behandler = Behandler(
                listOf(
                    Godkjenning(
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
                    )
                )
            )

            HPRRuleChain(
                healthInformation,
                BehandlerOgStartdato(behandler, null)
            ).getRuleByName("BEHANDLER_MT_FT_KI_OVER_12_UKER")
                .executeRule()
                .result shouldBeEqualTo false
        }

        it("Should check rule BEHANDLER_MANGLER_AUTORISASJON_I_HPR, should trigger rule") {
            val healthInformation = generateSykmelding()
            val behandler = Behandler(
                listOf(
                    Godkjenning(
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
                    )
                )
            )

            HPRRuleChain(
                healthInformation,
                BehandlerOgStartdato(behandler, null)
            ).getRuleByName("BEHANDLER_MANGLER_AUTORISASJON_I_HPR")
                .executeRule()
                .result shouldBeEqualTo true
        }

        it("Sjekker BEHANDLER_MT_FT_KI_OVER_12_UKER, slår ut fordi startdato for tidligere sykefravær gir varighet på mer enn 12 uker") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 3, 1),
                        tom = LocalDate.of(2019, 3, 27)
                    )
                )
            )

            val behandler = Behandler(
                listOf(
                    Godkjenning(
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
                    )
                )
            )

            HPRRuleChain(healthInformation,
                BehandlerOgStartdato(behandler,
                    LocalDate.of(2019, 1, 1))).getRuleByName("BEHANDLER_MT_FT_KI_OVER_12_UKER")
                .executeRule()
                .result shouldBeEqualTo true
        }

        it("Sjekker BEHANDLER_MT_FT_KI_OVER_12_UKER, slår ikke ut fordi det er nytt sykefravær") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 3, 1),
                        tom = LocalDate.of(2019, 3, 27)
                    )
                )
            )

            val behandler = Behandler(
                listOf(
                    Godkjenning(
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
                    )
                )
            )

            HPRRuleChain(healthInformation,
                BehandlerOgStartdato(behandler, null)).getRuleByName("BEHANDLER_MT_FT_KI_OVER_12_UKER")
                .executeRule()
                .result shouldBeEqualTo false
        }

        it("Sjekker BEHANDLER_MT_FT_KI_OVER_12_UKER, slår ikke ut fordi behandler er Lege(LE) og Kiropraktor(KI)") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 3, 1),
                        tom = LocalDate.of(2019, 3, 27)
                    )
                )
            )

            val behandler = Behandler(
                listOf(
                    Godkjenning(
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
                    ),
                    Godkjenning(
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
                    )
                )
            )

            HPRRuleChain(healthInformation, BehandlerOgStartdato(behandler, LocalDate.of(2019, 1, 1)))
                .getRuleByName("BEHANDLER_MT_FT_KI_OVER_12_UKER")
                .executeRule()
                .result shouldBeEqualTo false

        }

        it("Sjekker BEHANDLER_MT_FT_KI_OVER_12_UKER, slår ikke ut fordi startdato for tidligere sykefravær gir varighet på mindre enn 12 uker") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 3, 1),
                        tom = LocalDate.of(2019, 3, 27)
                    )
                )
            )

            val behandler = Behandler(
                listOf(
                    Godkjenning(
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
                    )
                )
            )

            HPRRuleChain(healthInformation,
                BehandlerOgStartdato(behandler,
                    LocalDate.of(2019, 2, 20))).getRuleByName("BEHANDLER_MT_FT_KI_OVER_12_UKER")
                .executeRule().result shouldBeEqualTo false
        }
    }
})
