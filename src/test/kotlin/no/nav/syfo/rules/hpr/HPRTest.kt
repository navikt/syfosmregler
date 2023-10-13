package no.nav.syfo.rules.hpr

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import no.nav.syfo.client.Behandler
import no.nav.syfo.client.Godkjenning
import no.nav.syfo.client.Kode
import no.nav.syfo.client.Periode
import no.nav.syfo.client.Tilleggskompetanse
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Status
import no.nav.syfo.objectMapper
import no.nav.syfo.rules.tilbakedatering.toRuleMetadata
import no.nav.syfo.services.BehandlerOgStartdato
import no.nav.syfo.services.RuleMetadataSykmelding
import no.nav.syfo.services.SykmeldingMetadataInfo
import org.amshove.kluent.`should contain all`
import org.amshove.kluent.shouldBeEqualTo

class HPRTest :
    FunSpec(
        {
            val ruleTree = HPRRulesExecution()

            context("Testing hpr rules and checking the rule outcomes") {
                test("har ikke aktiv autorisasjon, Status INVALID") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.of(2020, 1, 1),
                            tom = LocalDate.of(2020, 1, 2),
                            behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
                        )
                    val behandler =
                        generateBehandler(
                            "LE",
                            autorisasjon =
                                Kode(
                                    aktiv = false,
                                    oid = 7704,
                                    verdi = "1",
                                ),
                        )

                    val behandlerOgStartdato = BehandlerOgStartdato(behandler, null)

                    val ruleMetadata = sykmelding.toRuleMetadata()

                    val ruleMetadataSykmelding =
                        RuleMetadataSykmelding(
                            ruleMetadata = ruleMetadata,
                            sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato = behandlerOgStartdato,
                        )

                    val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                    status.first.treeResult.status shouldBeEqualTo Status.INVALID
                    status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            HPRRules.BEHANDLER_GYLIDG_I_HPR to false,
                        )
                    mapOf(
                        "behandlerGodkjenninger" to behandler.godkjenninger,
                    ) shouldBeEqualTo status.first.ruleInputs

                    status.first.treeResult.ruleHit shouldBeEqualTo
                        HPRRuleHit.BEHANDLER_IKKE_GYLDIG_I_HPR.ruleHit
                }

                test("mangler autorisasjon, Status INVALID") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.of(2020, 1, 1),
                            tom = LocalDate.of(2020, 1, 2),
                            behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
                        )
                    val behandler =
                        generateBehandler(
                            "LE",
                            autorisasjon =
                                Kode(
                                    aktiv = true,
                                    oid = 7702,
                                    verdi = "19",
                                ),
                        )

                    val behandlerOgStartdato = BehandlerOgStartdato(behandler, null)

                    val ruleMetadata = sykmelding.toRuleMetadata()

                    val ruleMetadataSykmelding =
                        RuleMetadataSykmelding(
                            ruleMetadata = ruleMetadata,
                            sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato = behandlerOgStartdato,
                        )

                    val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                    status.first.treeResult.status shouldBeEqualTo Status.INVALID
                    status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            HPRRules.BEHANDLER_GYLIDG_I_HPR to true,
                            HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR to false,
                        )

                    mapOf(
                        "behandlerGodkjenninger" to behandler.godkjenninger,
                    ) shouldBeEqualTo status.first.ruleInputs

                    status.first.treeResult.ruleHit shouldBeEqualTo
                        HPRRuleHit.BEHANDLER_MANGLER_AUTORISASJON_I_HPR.ruleHit
                }

                test("LEGE har aktiv autorisasjon, Status OK") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.of(2020, 1, 1),
                            tom = LocalDate.of(2020, 1, 2),
                            behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
                        )
                    val behandler = generateBehandler("LE")

                    val ruleMetadata = sykmelding.toRuleMetadata()

                    val ruleMetadataSykmelding =
                        RuleMetadataSykmelding(
                            ruleMetadata = ruleMetadata,
                            sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato = BehandlerOgStartdato(behandler, null),
                        )

                    val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                    status.first.treeResult.status shouldBeEqualTo Status.OK
                    status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            HPRRules.BEHANDLER_GYLIDG_I_HPR to true,
                            HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR to true,
                            HPRRules.BEHANDLER_ER_LEGE_I_HPR to true,
                        )

                    mapOf(
                        "behandlerGodkjenninger" to behandler.godkjenninger,
                    ) shouldBeEqualTo status.first.ruleInputs
                    status.first.treeResult.ruleHit shouldBeEqualTo null
                }

                test("TANNLEGE har aktiv autorisasjon, Status OK") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.of(2020, 1, 1),
                            tom = LocalDate.of(2020, 1, 2),
                            behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
                        )
                    val behandler = generateBehandler("TL")

                    val ruleMetadata = sykmelding.toRuleMetadata()

                    val ruleMetadataSykmelding =
                        RuleMetadataSykmelding(
                            ruleMetadata = ruleMetadata,
                            sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato = BehandlerOgStartdato(behandler, null),
                        )

                    val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                    status.first.treeResult.status shouldBeEqualTo Status.OK
                    status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            HPRRules.BEHANDLER_GYLIDG_I_HPR to true,
                            HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR to true,
                            HPRRules.BEHANDLER_ER_LEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_TANNLEGE_I_HPR to true,
                        )

                    mapOf(
                        "behandlerGodkjenninger" to behandler.godkjenninger,
                    ) shouldBeEqualTo status.first.ruleInputs
                    status.first.treeResult.ruleHit shouldBeEqualTo null
                }

                test("Manuellterapaut har aktiv autorisasjon, Status OK") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.of(2020, 1, 1),
                            tom = LocalDate.of(2020, 1, 2),
                            behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
                        )
                    val behandler = generateBehandler("MT")

                    val ruleMetadata = sykmelding.toRuleMetadata()

                    val ruleMetadataSykmelding =
                        RuleMetadataSykmelding(
                            ruleMetadata = ruleMetadata,
                            sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato = BehandlerOgStartdato(behandler, null),
                        )

                    val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                    status.first.treeResult.status shouldBeEqualTo Status.OK
                    status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            HPRRules.BEHANDLER_GYLIDG_I_HPR to true,
                            HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR to true,
                            HPRRules.BEHANDLER_ER_LEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_TANNLEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_MANUELLTERAPEUT_I_HPR to true,
                            HPRRules.SYKEFRAVAR_OVER_12_UKER to false,
                        )

                    mapOf(
                        "behandlerGodkjenninger" to behandler.godkjenninger,
                        "fom" to sykmelding.perioder.first().fom,
                        "tom" to sykmelding.perioder.first().tom,
                        "startDatoSykefravær" to sykmelding.perioder.first().fom,
                    ) shouldBeEqualTo status.first.ruleInputs
                    status.first.treeResult.ruleHit shouldBeEqualTo null
                }

                test(
                    "Manuellterapaut har aktiv autorisasjon, sykefravæt over 12 uker, Status INVALID"
                ) {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.of(2020, 1, 1),
                            tom = LocalDate.of(2020, 1, 2),
                            behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
                        )
                    val behandler = generateBehandler("MT")

                    val ruleMetadata = sykmelding.toRuleMetadata()

                    val ruleMetadataSykmelding =
                        RuleMetadataSykmelding(
                            ruleMetadata = ruleMetadata,
                            sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    behandler,
                                    sykmelding.perioder.first().tom.minusDays(85),
                                ),
                        )

                    val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                    status.first.treeResult.status shouldBeEqualTo Status.INVALID
                    status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            HPRRules.BEHANDLER_GYLIDG_I_HPR to true,
                            HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR to true,
                            HPRRules.BEHANDLER_ER_LEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_TANNLEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_MANUELLTERAPEUT_I_HPR to true,
                            HPRRules.SYKEFRAVAR_OVER_12_UKER to true,
                        )

                    mapOf(
                        "behandlerGodkjenninger" to behandler.godkjenninger,
                        "fom" to sykmelding.perioder.first().fom,
                        "tom" to sykmelding.perioder.first().tom,
                        "startDatoSykefravær" to
                            (ruleMetadataSykmelding.behandlerOgStartdato.startdato),
                    ) shouldBeEqualTo status.first.ruleInputs
                    status.first.treeResult.ruleHit shouldBeEqualTo
                        HPRRuleHit.BEHANDLER_MT_FT_KI_OVER_12_UKER.ruleHit
                }

                test("behandler ikke riktig helsepersonell kategori, Status INVALID") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.of(2020, 1, 1),
                            tom = LocalDate.of(2020, 1, 2),
                            behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
                        )
                    val behandler = generateBehandler("PL")

                    val behandlerOgStartdato = BehandlerOgStartdato(behandler, null)

                    val ruleMetadata = sykmelding.toRuleMetadata()

                    val ruleMetadataSykmelding =
                        RuleMetadataSykmelding(
                            ruleMetadata = ruleMetadata,
                            sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato = behandlerOgStartdato,
                        )

                    val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                    status.first.treeResult.status shouldBeEqualTo Status.INVALID
                    status.first.rulePath.map { it.rule to it.ruleResult } `should contain all`
                        listOf(
                            HPRRules.BEHANDLER_GYLIDG_I_HPR to true,
                            HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR to true,
                            HPRRules.BEHANDLER_ER_LEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_TANNLEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_MANUELLTERAPEUT_I_HPR to false,
                            HPRRules.BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR to false,
                        )

                    mapOf(
                        "behandlerGodkjenninger" to behandler.godkjenninger,
                    ) shouldBeEqualTo status.first.ruleInputs

                    status.first.treeResult.ruleHit shouldBeEqualTo
                        HPRRuleHit.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR.ruleHit
                }

                test("FT uten tilleggsinfo, Status INVALID") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.of(2020, 1, 1),
                            tom = LocalDate.of(2020, 4, 2),
                            behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
                        )
                    val behandler = generateBehandler("FT")

                    val behandlerOgStartdato = BehandlerOgStartdato(behandler, null)

                    val ruleMetadata = sykmelding.toRuleMetadata()

                    val ruleMetadataSykmelding =
                        RuleMetadataSykmelding(
                            ruleMetadata = ruleMetadata,
                            sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato = behandlerOgStartdato,
                        )

                    val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                    status.first.treeResult.status shouldBeEqualTo Status.INVALID
                    status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            HPRRules.BEHANDLER_GYLIDG_I_HPR to true,
                            HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR to true,
                            HPRRules.BEHANDLER_ER_LEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_TANNLEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_MANUELLTERAPEUT_I_HPR to false,
                            HPRRules.BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR to false,
                        )

                    mapOf(
                        "behandlerGodkjenninger" to behandler.godkjenninger,
                    ) shouldBeEqualTo status.first.ruleInputs

                    status.first.treeResult.ruleHit shouldBeEqualTo
                        HPRRuleHit.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR.ruleHit
                }

                test("FT Gyldig") {
                    val behandler =
                        generateBehandler(
                            "FT",
                            tilleggskompetanse =
                                Tilleggskompetanse(
                                    avsluttetStatus = null,
                                    eTag = null,
                                    gyldig =
                                        Periode(
                                            fra = LocalDate.of(2000, 1, 1).atStartOfDay(),
                                            til = null,
                                        ),
                                    id = null,
                                    type =
                                        Kode(
                                            aktiv = true,
                                            oid = 7702,
                                            verdi = "1",
                                        ),
                                ),
                        )

                    val sykmedling = generateSykmelding()
                    val mockRuleMetadata = mockk<RuleMetadataSykmelding>()
                    every { mockRuleMetadata.behandlerOgStartdato } returns
                        BehandlerOgStartdato(behandler, null)

                    val result = ruleTree.runRules(sykmedling, mockRuleMetadata)
                    result.first.treeResult.status shouldBeEqualTo Status.OK
                    result.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            HPRRules.BEHANDLER_GYLIDG_I_HPR to true,
                            HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR to true,
                            HPRRules.BEHANDLER_ER_LEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_TANNLEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_MANUELLTERAPEUT_I_HPR to false,
                            HPRRules.BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR to true,
                            HPRRules.SYKEFRAVAR_OVER_12_UKER to false
                        )
                }

                test("FT Gyldig over 12 uker, INVALID") {
                    val behandler =
                        generateBehandler(
                            "FT",
                            tilleggskompetanse =
                                Tilleggskompetanse(
                                    avsluttetStatus = null,
                                    eTag = null,
                                    gyldig =
                                        Periode(
                                            fra = LocalDate.of(2000, 1, 1).atStartOfDay(),
                                            til = null,
                                        ),
                                    id = null,
                                    type =
                                        Kode(
                                            aktiv = true,
                                            oid = 7702,
                                            verdi = "1",
                                        ),
                                ),
                        )

                    val sykmelding = generateSykmelding()
                    val ruleMetadata = sykmelding.toRuleMetadata()

                    val ruleMetadataSykmelding =
                        RuleMetadataSykmelding(
                            ruleMetadata = ruleMetadata,
                            sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    behandler,
                                    sykmelding.perioder.first().tom.minusDays(85),
                                ),
                        )

                    val result = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)
                    result.first.treeResult.status shouldBeEqualTo Status.INVALID
                    result.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            HPRRules.BEHANDLER_GYLIDG_I_HPR to true,
                            HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR to true,
                            HPRRules.BEHANDLER_ER_LEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_TANNLEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_MANUELLTERAPEUT_I_HPR to false,
                            HPRRules.BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR to true,
                            HPRRules.SYKEFRAVAR_OVER_12_UKER to true
                        )
                }

                test("KI uten tilleggsinfo, Status INVALID") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.of(2020, 1, 1),
                            tom = LocalDate.of(2020, 4, 2),
                            behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay(),
                        )
                    val behandler = generateBehandler("KI")

                    val behandlerOgStartdato = BehandlerOgStartdato(behandler, null)

                    val ruleMetadata = sykmelding.toRuleMetadata()

                    val ruleMetadataSykmelding =
                        RuleMetadataSykmelding(
                            ruleMetadata = ruleMetadata,
                            sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato = behandlerOgStartdato,
                        )

                    val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                    status.first.treeResult.status shouldBeEqualTo Status.INVALID
                    status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            HPRRules.BEHANDLER_GYLIDG_I_HPR to true,
                            HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR to true,
                            HPRRules.BEHANDLER_ER_LEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_TANNLEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_MANUELLTERAPEUT_I_HPR to false,
                            HPRRules.BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR to false,
                        )

                    mapOf(
                        "behandlerGodkjenninger" to behandler.godkjenninger,
                    ) shouldBeEqualTo status.first.ruleInputs

                    status.first.treeResult.ruleHit shouldBeEqualTo
                        HPRRuleHit.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR.ruleHit
                }

                test("KI Gyldig") {
                    val behandler =
                        generateBehandler(
                            "KI",
                            tilleggskompetanse =
                                Tilleggskompetanse(
                                    avsluttetStatus = null,
                                    eTag = null,
                                    gyldig =
                                        Periode(
                                            fra = LocalDate.of(2000, 1, 1).atStartOfDay(),
                                            til = null,
                                        ),
                                    id = null,
                                    type =
                                        Kode(
                                            aktiv = true,
                                            oid = 7702,
                                            verdi = "1",
                                        ),
                                ),
                        )

                    val sykmedling = generateSykmelding()
                    val mockRuleMetadata = mockk<RuleMetadataSykmelding>()
                    every { mockRuleMetadata.behandlerOgStartdato } returns
                        BehandlerOgStartdato(behandler, null)

                    val result = ruleTree.runRules(sykmedling, mockRuleMetadata)
                    result.first.treeResult.status shouldBeEqualTo Status.OK
                    result.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            HPRRules.BEHANDLER_GYLIDG_I_HPR to true,
                            HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR to true,
                            HPRRules.BEHANDLER_ER_LEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_TANNLEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_MANUELLTERAPEUT_I_HPR to false,
                            HPRRules.BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR to true,
                            HPRRules.SYKEFRAVAR_OVER_12_UKER to false
                        )
                }

                test("KI Gyldig over 12 uker, INVALID") {
                    val behandler =
                        generateBehandler(
                            "KI",
                            tilleggskompetanse =
                                Tilleggskompetanse(
                                    avsluttetStatus = null,
                                    eTag = null,
                                    gyldig =
                                        Periode(
                                            fra = LocalDate.of(2000, 1, 1).atStartOfDay(),
                                            til = null,
                                        ),
                                    id = null,
                                    type =
                                        Kode(
                                            aktiv = true,
                                            oid = 7702,
                                            verdi = "1",
                                        ),
                                ),
                        )

                    val sykmelding = generateSykmelding()
                    val ruleMetadata = sykmelding.toRuleMetadata()

                    val ruleMetadataSykmelding =
                        RuleMetadataSykmelding(
                            ruleMetadata = ruleMetadata,
                            sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    behandler,
                                    sykmelding.perioder.first().tom.minusDays(85),
                                ),
                        )

                    val result = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)
                    result.first.treeResult.status shouldBeEqualTo Status.INVALID
                    result.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            HPRRules.BEHANDLER_GYLIDG_I_HPR to true,
                            HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR to true,
                            HPRRules.BEHANDLER_ER_LEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_TANNLEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_MANUELLTERAPEUT_I_HPR to false,
                            HPRRules.BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR to true,
                            HPRRules.SYKEFRAVAR_OVER_12_UKER to true
                        )
                }

                test("FT, Gyldig periode fom samme dag som genereringstidspunkt") {
                    val sykmedling = generateSykmelding()
                    val behandler =
                        Behandler(
                            godkjenninger =
                                listOf(
                                    Godkjenning(
                                        autorisasjon = Kode(aktiv = true, oid = 7704, verdi = "18"),
                                        helsepersonellkategori =
                                            Kode(aktiv = true, oid = 0, verdi = "FT"),
                                        tillegskompetanse =
                                            listOf(
                                                Tilleggskompetanse(
                                                    avsluttetStatus = null,
                                                    eTag = null,
                                                    gyldig =
                                                        Periode(
                                                            fra = sykmedling.signaturDato,
                                                            til = null,
                                                        ),
                                                    id = null,
                                                    type =
                                                        Kode(
                                                            aktiv = true,
                                                            oid = 7702,
                                                            verdi = "1",
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                        )

                    val mockRuleMetadata = mockk<RuleMetadataSykmelding>()
                    every { mockRuleMetadata.behandlerOgStartdato } returns
                        BehandlerOgStartdato(behandler, null)

                    val result = ruleTree.runRules(sykmedling, mockRuleMetadata)
                    result.first.treeResult.status shouldBeEqualTo Status.OK
                }
                test("FT, ugylidg periode") {
                    val sykmedling = generateSykmelding()
                    val behandler =
                        Behandler(
                            godkjenninger =
                                listOf(
                                    Godkjenning(
                                        autorisasjon = Kode(aktiv = true, oid = 7704, verdi = "18"),
                                        helsepersonellkategori =
                                            Kode(aktiv = true, oid = 0, verdi = "FT"),
                                        tillegskompetanse =
                                            listOf(
                                                Tilleggskompetanse(
                                                    avsluttetStatus = null,
                                                    eTag = null,
                                                    gyldig =
                                                        Periode(
                                                            fra =
                                                                sykmedling.signaturDato.plusDays(1),
                                                            til = null,
                                                        ),
                                                    id = null,
                                                    type =
                                                        Kode(
                                                            aktiv = true,
                                                            oid = 7702,
                                                            verdi = "1",
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                        )

                    val mockRuleMetadata = mockk<RuleMetadataSykmelding>()
                    every { mockRuleMetadata.behandlerOgStartdato } returns
                        BehandlerOgStartdato(behandler, null)

                    val result = ruleTree.runRules(sykmedling, mockRuleMetadata)
                    result.first.treeResult.status shouldBeEqualTo Status.INVALID
                }

                test("FT, ugylidg periode tom før genereringstidspunkt") {
                    val sykmedling = generateSykmelding()
                    val behandler =
                        Behandler(
                            godkjenninger =
                                listOf(
                                    Godkjenning(
                                        autorisasjon = Kode(aktiv = true, oid = 7704, verdi = "18"),
                                        helsepersonellkategori =
                                            Kode(aktiv = true, oid = 0, verdi = "FT"),
                                        tillegskompetanse =
                                            listOf(
                                                Tilleggskompetanse(
                                                    avsluttetStatus = null,
                                                    eTag = null,
                                                    gyldig =
                                                        Periode(
                                                            fra =
                                                                sykmedling.signaturDato.minusDays(
                                                                    10,
                                                                ),
                                                            til =
                                                                sykmedling.signaturDato.minusDays(
                                                                    1,
                                                                ),
                                                        ),
                                                    id = null,
                                                    type =
                                                        Kode(
                                                            aktiv = true,
                                                            oid = 7702,
                                                            verdi = "1",
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                        )

                    val mockRuleMetadata = mockk<RuleMetadataSykmelding>()
                    every { mockRuleMetadata.behandlerOgStartdato } returns
                        BehandlerOgStartdato(behandler, null)

                    val result = ruleTree.runRules(sykmedling, mockRuleMetadata)
                    result.first.treeResult.status shouldBeEqualTo Status.INVALID
                }

                test("FT, gyldig periode tom samme som genereringstidspunkt") {
                    val sykmedling = generateSykmelding()
                    val behandler =
                        Behandler(
                            godkjenninger =
                                listOf(
                                    Godkjenning(
                                        autorisasjon = Kode(aktiv = true, oid = 7704, verdi = "18"),
                                        helsepersonellkategori =
                                            Kode(aktiv = true, oid = 0, verdi = "FT"),
                                        tillegskompetanse =
                                            listOf(
                                                Tilleggskompetanse(
                                                    avsluttetStatus = null,
                                                    eTag = null,
                                                    gyldig =
                                                        Periode(
                                                            fra =
                                                                sykmedling.signaturDato.minusDays(
                                                                    10,
                                                                ),
                                                            til = sykmedling.signaturDato,
                                                        ),
                                                    id = null,
                                                    type =
                                                        Kode(
                                                            aktiv = true,
                                                            oid = 7702,
                                                            verdi = "1",
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                        )

                    val mockRuleMetadata = mockk<RuleMetadataSykmelding>()
                    every { mockRuleMetadata.behandlerOgStartdato } returns
                        BehandlerOgStartdato(behandler, null)

                    val result = ruleTree.runRules(sykmedling, mockRuleMetadata)
                    result.first.treeResult.status shouldBeEqualTo Status.OK
                }
                test("FT, ugyldig, feil tillegskompetanse type verdi") {
                    val sykmedling = generateSykmelding()
                    val behandler =
                        Behandler(
                            godkjenninger =
                                listOf(
                                    Godkjenning(
                                        autorisasjon = Kode(aktiv = true, oid = 7704, verdi = "18"),
                                        helsepersonellkategori =
                                            Kode(aktiv = true, oid = 0, verdi = "FT"),
                                        tillegskompetanse =
                                            listOf(
                                                Tilleggskompetanse(
                                                    avsluttetStatus = null,
                                                    eTag = null,
                                                    gyldig =
                                                        Periode(
                                                            fra =
                                                                sykmedling.signaturDato.minusDays(
                                                                    10,
                                                                ),
                                                            til = sykmedling.signaturDato,
                                                        ),
                                                    id = null,
                                                    type =
                                                        Kode(
                                                            aktiv = true,
                                                            oid = 7702,
                                                            verdi = "2",
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                        )

                    val mockRuleMetadata = mockk<RuleMetadataSykmelding>()
                    every { mockRuleMetadata.behandlerOgStartdato } returns
                        BehandlerOgStartdato(behandler, null)

                    val result = ruleTree.runRules(sykmedling, mockRuleMetadata)
                    result.first.treeResult.status shouldBeEqualTo Status.INVALID
                }
                test("FT, ugyldig, feil tillegskompetanse er ikke aktiv") {
                    val sykmedling = generateSykmelding()
                    val behandler =
                        Behandler(
                            godkjenninger =
                                listOf(
                                    Godkjenning(
                                        autorisasjon = Kode(aktiv = true, oid = 7704, verdi = "18"),
                                        helsepersonellkategori =
                                            Kode(aktiv = true, oid = 0, verdi = "FT"),
                                        tillegskompetanse =
                                            listOf(
                                                Tilleggskompetanse(
                                                    avsluttetStatus = null,
                                                    eTag = null,
                                                    gyldig =
                                                        Periode(
                                                            fra =
                                                                LocalDate.of(2000, 1, 1)
                                                                    .atStartOfDay(),
                                                            til = null,
                                                        ),
                                                    id = null,
                                                    type =
                                                        Kode(
                                                            aktiv = false,
                                                            oid = 7702,
                                                            verdi = "2",
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                        )

                    val mockRuleMetadata = mockk<RuleMetadataSykmelding>()
                    every { mockRuleMetadata.behandlerOgStartdato } returns
                        BehandlerOgStartdato(behandler, null)

                    val result = ruleTree.runRules(sykmedling, mockRuleMetadata)
                    result.first.treeResult.status shouldBeEqualTo Status.INVALID
                }

                test("Behandler er KI med gyldig stuff ") {
                                    val behandlerSTring = "[\n" +
                                        "  {\n" +
                                        "    \"helsepersonellkategori\": {\n" +
                                        "      \"aktiv\": true,\n" +
                                        "      \"oid\": 9060,\n" +
                                        "      \"verdi\": \"ET\"\n" +
                                        "    },\n" +
                                        "    \"autorisasjon\": {\n" +
                                        "      \"aktiv\": true,\n" +
                                        "      \"oid\": 7704,\n" +
                                        "      \"verdi\": \"1\"\n" +
                                        "    },\n" +
                                        "    \"tillegskompetanse\": null\n" +
                                        "  },\n" +
                                        "  {\n" +
                                        "    \"helsepersonellkategori\": {\n" +
                                        "      \"aktiv\": true,\n" +
                                        "      \"oid\": 9060,\n" +
                                        "      \"verdi\": \"KI\"\n" +
                                        "    },\n" +
                                        "    \"autorisasjon\": {\n" +
                                        "      \"aktiv\": true,\n" +
                                        "      \"oid\": 7704,\n" +
                                        "      \"verdi\": \"1\"\n" +
                                        "    },\n" +
                                        "    \"tillegskompetanse\": [\n" +
                                        "      {\n" +
                                        "        \"avsluttetStatus\": null,\n" +
                                        "        \"gyldig\": {\n" +
                                        "          \"fra\": \"2015-08-16T22:00:00\",\n" +
                                        "          \"til\": \"2059-01-05T23:00:00\"\n" +
                                        "        },\n" +
                                        "        \"id\": 20358,\n" +
                                        "        \"type\": {\n" +
                                        "          \"aktiv\": true,\n" +
                                        "          \"oid\": 7702,\n" +
                                        "          \"verdi\": \"1\"\n" +
                                        "        },\n" +
                                        "        \"etag\": null\n" +
                                        "      }\n" +
                                        "    ]\n" +
                                        "  }\n" +
                                        "]"

                    val behandlerGodkjenninger = objectMapper.readValue<List<Godkjenning>>(behandlerSTring)

                    val behandler = Behandler(behandlerGodkjenninger)

                    val sykmedling = generateSykmelding()
                    val mockRuleMetadata = mockk<RuleMetadataSykmelding>()
                    every { mockRuleMetadata.behandlerOgStartdato } returns
                        BehandlerOgStartdato(behandler, null)

                    val result = ruleTree.runRules(sykmedling, mockRuleMetadata)
                    result.first.treeResult.status shouldBeEqualTo Status.OK
                    result.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            HPRRules.BEHANDLER_GYLIDG_I_HPR to true,
                            HPRRules.BEHANDLER_HAR_AUTORISASJON_I_HPR to true,
                            HPRRules.BEHANDLER_ER_LEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_TANNLEGE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_MANUELLTERAPEUT_I_HPR to false,
                            HPRRules.BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR to false,
                            HPRRules.BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR to true,
                            HPRRules.SYKEFRAVAR_OVER_12_UKER to false
                        )

                }
            }
        },
    )

private fun generateBehandler(
    helsepersonellKategori: String,
    tilleggskompetanse: Tilleggskompetanse? = null,
    autorisasjon: Kode =
        Kode(
            aktiv = true,
            oid = 7704,
            verdi = "18",
        ),
) =
    Behandler(
        listOf(
            Godkjenning(
                autorisasjon = autorisasjon,
                helsepersonellkategori =
                    Kode(
                        aktiv = true,
                        oid = 0,
                        verdi = helsepersonellKategori,
                    ),
                tillegskompetanse =
                    tilleggskompetanse?.let {
                        listOf(
                            tilleggskompetanse,
                        )
                    }
                        ?: emptyList(),
            ),
        ),
    )
