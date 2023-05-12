package no.nav.syfo.rules.hpr

import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.client.Behandler
import no.nav.syfo.client.Godkjenning
import no.nav.syfo.client.Kode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Status
import no.nav.syfo.rules.tilbakedatering.toRuleMetadata
import no.nav.syfo.services.BehandlerOgStartdato
import no.nav.syfo.services.RuleMetadataSykmelding
import no.nav.syfo.services.SykmeldingMetadataInfo
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate

class HPRTest : FunSpec({
    val ruleTree = HPRRulesExecution()

    context("Testing hpr rules and checking the rule outcomes") {
        test("har aktiv autorisasjon, Status OK") {
            val sykmelding = generateSykmelding(
                fom = LocalDate.of(2020, 1, 1),
                tom = LocalDate.of(2020, 1, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
            )
            val behandler = Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon = Kode(
                            aktiv = true,
                            oid = 7704,
                            verdi = "1",
                        ),
                        helsepersonellkategori = Kode(
                            aktiv = true,
                            oid = 0,
                            verdi = "LE",
                        ),
                    ),
                ),
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = ruleMetadata,
                sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                doctorSuspensjon = false,
                behandlerOgStartdato = BehandlerOgStartdato(behandler, null),
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

            status.first.treeResult.status shouldBeEqualTo Status.OK
            status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR to false,
                HPRRules.BEHANDLER_MANGLER_AUTORISASJON_I_HPR to false,
                HPRRules.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR to false,
                HPRRules.BEHANDLER_MT_FT_KI_OVER_12_UKER to false,
            )

            mapOf(
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
            ) shouldBeEqualTo status.first.ruleInputs

            status.first.treeResult.ruleHit shouldBeEqualTo null
        }

        test("har ikke aktiv autorisasjon, Status INVALID") {
            val sykmelding = generateSykmelding(
                fom = LocalDate.of(2020, 1, 1),
                tom = LocalDate.of(2020, 1, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
            )
            val behandler = Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon = Kode(
                            aktiv = false,
                            oid = 7704,
                            verdi = "1",
                        ),
                        helsepersonellkategori = Kode(
                            aktiv = true,
                            oid = 0,
                            verdi = "LE",
                        ),
                    ),
                ),
            )

            val behandlerOgStartdato = BehandlerOgStartdato(behandler, null)

            val ruleMetadata = sykmelding.toRuleMetadata()

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = ruleMetadata,
                sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                doctorSuspensjon = false,
                behandlerOgStartdato = behandlerOgStartdato,
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

            status.first.treeResult.status shouldBeEqualTo Status.INVALID
            status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR to true,
            )
            mapOf(
                "behandlerGodkjenninger" to behandler.godkjenninger,
            ) shouldBeEqualTo status.first.ruleInputs

            status.first.treeResult.ruleHit shouldBeEqualTo HPRRuleHit.BEHANDLER_IKKE_GYLDIG_I_HPR.ruleHit
        }
        test("mangler autorisasjon, Status INVALID") {
            val sykmelding = generateSykmelding(
                fom = LocalDate.of(2020, 1, 1),
                tom = LocalDate.of(2020, 1, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
            )
            val behandler = Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon = Kode(
                            aktiv = true,
                            oid = 7702,
                            verdi = "19",
                        ),
                        helsepersonellkategori = Kode(
                            aktiv = true,
                            oid = 0,
                            verdi = "LE",
                        ),
                    ),
                ),
            )

            val behandlerOgStartdato = BehandlerOgStartdato(behandler, null)

            val ruleMetadata = sykmelding.toRuleMetadata()

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = ruleMetadata,
                sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                doctorSuspensjon = false,
                behandlerOgStartdato = behandlerOgStartdato,
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

            status.first.treeResult.status shouldBeEqualTo Status.INVALID
            status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR to false,
                HPRRules.BEHANDLER_MANGLER_AUTORISASJON_I_HPR to true,
            )

            mapOf(
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
            ) shouldBeEqualTo status.first.ruleInputs

            status.first.treeResult.ruleHit shouldBeEqualTo HPRRuleHit.BEHANDLER_MANGLER_AUTORISASJON_I_HPR.ruleHit
        }
        test("behandler ikke riktig helsepersonell kategori, Status INVALID") {
            val sykmelding = generateSykmelding(
                fom = LocalDate.of(2020, 1, 1),
                tom = LocalDate.of(2020, 1, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
            )
            val behandler = Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon = Kode(
                            aktiv = true,
                            oid = 7704,
                            verdi = "18",
                        ),
                        helsepersonellkategori = Kode(
                            aktiv = true,
                            oid = 0,
                            verdi = "PL",
                        ),
                    ),
                ),
            )

            val behandlerOgStartdato = BehandlerOgStartdato(behandler, null)

            val ruleMetadata = sykmelding.toRuleMetadata()

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = ruleMetadata,
                sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                doctorSuspensjon = false,
                behandlerOgStartdato = behandlerOgStartdato,
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

            status.first.treeResult.status shouldBeEqualTo Status.INVALID
            status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR to false,
                HPRRules.BEHANDLER_MANGLER_AUTORISASJON_I_HPR to false,
                HPRRules.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR to true,
            )

            mapOf(
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
            ) shouldBeEqualTo status.first.ruleInputs

            status.first.treeResult.ruleHit shouldBeEqualTo HPRRuleHit.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR.ruleHit
        }

        test("behandler KI MT FT over 84 dager, Status INVALID") {
            val sykmelding = generateSykmelding(
                fom = LocalDate.of(2020, 1, 1),
                tom = LocalDate.of(2020, 4, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
            )
            val behandler = Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon = Kode(
                            aktiv = true,
                            oid = 7704,
                            verdi = "18",
                        ),
                        helsepersonellkategori = Kode(
                            aktiv = true,
                            oid = 0,
                            verdi = "MT",
                        ),
                    ),
                ),
            )

            val behandlerOgStartdato = BehandlerOgStartdato(behandler, null)

            val ruleMetadata = sykmelding.toRuleMetadata()

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = ruleMetadata,
                sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                doctorSuspensjon = false,
                behandlerOgStartdato = behandlerOgStartdato,
            )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

            status.first.treeResult.status shouldBeEqualTo Status.INVALID
            status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR to false,
                HPRRules.BEHANDLER_MANGLER_AUTORISASJON_I_HPR to false,
                HPRRules.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR to false,
                HPRRules.BEHANDLER_MT_FT_KI_OVER_12_UKER to true,
            )

            mapOf(
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
                "behandlerGodkjenninger" to behandler.godkjenninger,
            ) shouldBeEqualTo status.first.ruleInputs

            status.first.treeResult.ruleHit shouldBeEqualTo HPRRuleHit.BEHANDLER_MT_FT_KI_OVER_12_UKER.ruleHit
        }
    }

    test("test") {
        val behandler = Behandler(
            listOf(
                Godkjenning(
                    autorisasjon = Kode(
                        aktiv = false,
                        oid = 7704,
                        verdi = "99",
                    ),
                    helsepersonellkategori = Kode(
                        aktiv = true,
                        oid = 9060,
                        verdi = "HP",
                    ),
                ),
                Godkjenning(
                    helsepersonellkategori = Kode(aktiv = true, verdi = "LE", oid = 9060),
                    autorisasjon = Kode(aktiv = true, oid = 7704, verdi = "1"),
                ),
            ),
        )
        val sykmelding = generateSykmelding(
            fom = LocalDate.of(2020, 1, 1),
            tom = LocalDate.of(2020, 4, 2),
            behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
        )
        val mockRuleMetadata = mockk<RuleMetadataSykmelding>()
        every { mockRuleMetadata.behandlerOgStartdato } returns BehandlerOgStartdato(behandler, null)
        val result = ruleTree.runRules(sykmelding, mockRuleMetadata)
        result.first.treeResult.status shouldBeEqualTo Status.OK
    }
})
