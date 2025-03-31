package no.nav.syfo.rules.tilbakedatering

import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import no.nav.helse.diagnosekoder.Diagnosekoder
import no.nav.syfo.client.Behandler
import no.nav.syfo.client.SmregisterClient
import no.nav.syfo.generateSykmelding
import no.nav.syfo.generateSykmeldingDTO
import no.nav.syfo.model.KontaktMedPasient
import no.nav.syfo.model.MedisinskVurdering
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.ARBEIDSGIVERPERIODE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.BEGRUNNELSE_MIN_1_ORD
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.BEGRUNNELSE_MIN_3_ORD
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.ETTERSENDING
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.FORLENGELSE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.SPESIALISTHELSETJENESTEN
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERING
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_INNTIL_4_DAGER
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_INNTIL_8_DAGER
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_MINDRE_ENN_1_MAANED
import no.nav.syfo.services.BehandlerOgStartdato
import no.nav.syfo.services.RuleMetadataSykmelding
import no.nav.syfo.services.SykmeldingInfo
import no.nav.syfo.services.SykmeldingMetadataInfo
import no.nav.syfo.services.SykmeldingService
import no.nav.syfo.services.sortedFOMDate
import no.nav.syfo.services.sortedTOMDate
import no.nav.syfo.toDiagnose
import no.nav.syfo.utils.LoggingMeta
import org.amshove.kluent.shouldBeEqualTo

class TilbakedateringTest :
    FunSpec({
        val ruleTree = TilbakedateringRulesExecution()
        val smregisterClient = mockk<SmregisterClient>()
        val sykmeldingService = SykmeldingService(smregisterClient)
        val loggingMetadata = mockk<LoggingMeta>(relaxed = true)
        context("Test tilbakedateringsregler mindre enn 9 dager") {
            test("ikke tilbakedatert, Status OK") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(1),
                        behandletTidspunkt = LocalDate.now().atStartOfDay(),
                    )
                val sykmeldingMetadata =
                    RuleMetadataSykmelding(
                        ruleMetadata = sykmelding.toRuleMetadata(),
                        SykmeldingMetadataInfo(null, null, LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)
                status.treeResult.status shouldBeEqualTo Status.OK
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(TILBAKEDATERING to false)
                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                    )
                status.treeResult.ruleHit shouldBeEqualTo null
            }
            test("tilbakedatert med en dag") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(1),
                        behandletTidspunkt = LocalDate.now().plusDays(1).atStartOfDay(),
                    )
                val sykmeldingMetadata =
                    RuleMetadataSykmelding(
                        ruleMetadata = sykmelding.toRuleMetadata(),
                        SykmeldingMetadataInfo(
                            SykmeldingInfo(
                                sykmeldingId = "sykmeldingID",
                                fom = LocalDate.now(),
                                tom = LocalDate.now().plusDays(1),
                                null
                            ),
                            null,
                            LocalDate.now()
                        ),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)
                status.treeResult.status shouldBeEqualTo Status.OK
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to true,
                    )
                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                        "ettersending" to
                            SykmeldingInfo(
                                sykmeldingId = "sykmeldingID",
                                fom = LocalDate.now(),
                                tom = LocalDate.now().plusDays(1),
                                null
                            ),
                    )
                status.treeResult.ruleHit shouldBeEqualTo null
            }

            test("tilbakedatert forlengelse med ettersending") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(1),
                        behandletTidspunkt = LocalDate.now().plusDays(8).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, null),
                    )

                val sykmeldingMetadata =
                    RuleMetadataSykmelding(
                        ruleMetadata = sykmelding.toRuleMetadata(),
                        SykmeldingMetadataInfo(
                            SykmeldingInfo(
                                sykmeldingId = "sykmeldingID",
                                fom = LocalDate.now(),
                                tom = LocalDate.now().plusDays(1),
                                null
                            ),
                            null,
                            LocalDate.now()
                        ),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)
                status.treeResult.status shouldBeEqualTo Status.OK
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to true,
                    )
                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                        "ettersending" to
                            SykmeldingInfo(
                                sykmeldingId = "sykmeldingID",
                                fom = LocalDate.now(),
                                tom = LocalDate.now().plusDays(1),
                                null
                            ),
                    )
            }
            test("tilbakedatert forlengelse uten ettersending") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(1),
                        behandletTidspunkt = LocalDate.now().plusDays(3).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, "begrunnelse"),
                    )
                val sykmeldingMetadata =
                    RuleMetadataSykmelding(
                        ruleMetadata = sykmelding.toRuleMetadata(),
                        SykmeldingMetadataInfo(null, null, LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)
                status.treeResult.status shouldBeEqualTo Status.OK
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to false,
                        TILBAKEDATERT_INNTIL_4_DAGER to true,
                    )
                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                    )
            }
            context("Tilbakedatert") {
                context("med begrunnelse") {
                    test("Med begrunnelse OK") {
                        val sykmelding =
                            generateSykmelding(
                                fom = LocalDate.now(),
                                tom = LocalDate.now().plusDays(1),
                                behandletTidspunkt = LocalDate.now().plusDays(5).atStartOfDay(),
                                kontaktMedPasient = KontaktMedPasient(null, "arst"),
                            )
                        val sykmeldingMetadata =
                            RuleMetadataSykmelding(
                                ruleMetadata = sykmelding.toRuleMetadata(),
                                SykmeldingMetadataInfo(null, null, LocalDate.now()),
                                doctorSuspensjon = false,
                                behandlerOgStartdato =
                                    BehandlerOgStartdato(
                                        Behandler(emptyList(), null),
                                        null,
                                    ),
                            )
                        val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)
                        status.treeResult.status shouldBeEqualTo Status.OK
                        status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                            listOf(
                                TILBAKEDATERING to true,
                                ETTERSENDING to false,
                                TILBAKEDATERT_INNTIL_4_DAGER to false,
                                TILBAKEDATERT_INNTIL_8_DAGER to true,
                                BEGRUNNELSE_MIN_1_ORD to true,
                            )
                        status.ruleInputs shouldBeEqualTo
                            mapOf(
                                "fom" to sykmelding.perioder.first().fom,
                                "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                                "begrunnelse" to
                                    "${getNumberOfWords(sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt)} ord",
                            )
                        status.treeResult.status shouldBeEqualTo Status.OK
                    }
                    test("Med kontaktdato uten begrunnelse, Invalid") {
                        val sykmelding =
                            generateSykmelding(
                                fom = LocalDate.now(),
                                tom = LocalDate.now().plusDays(1),
                                behandletTidspunkt = LocalDate.now().plusDays(5).atStartOfDay(),
                                kontaktMedPasient = KontaktMedPasient(LocalDate.now(), null),
                            )
                        val sykmeldingMetadata =
                            RuleMetadataSykmelding(
                                ruleMetadata = sykmelding.toRuleMetadata(),
                                SykmeldingMetadataInfo(null, null, LocalDate.now()),
                                doctorSuspensjon = false,
                                behandlerOgStartdato =
                                    BehandlerOgStartdato(
                                        Behandler(emptyList(), null),
                                        null,
                                    ),
                            )
                        val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)
                        status.treeResult.status shouldBeEqualTo Status.INVALID
                        status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                            listOf(
                                TILBAKEDATERING to true,
                                ETTERSENDING to false,
                                TILBAKEDATERT_INNTIL_4_DAGER to false,
                                TILBAKEDATERT_INNTIL_8_DAGER to true,
                                BEGRUNNELSE_MIN_1_ORD to false,
                                FORLENGELSE to false,
                                SPESIALISTHELSETJENESTEN to false,
                            )
                        status.ruleInputs shouldBeEqualTo
                            mapOf(
                                "fom" to sykmelding.perioder.first().fom,
                                "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                                "begrunnelse" to "0 ord",
                                "diagnosesystem" to
                                    sykmelding.medisinskVurdering.hovedDiagnose?.system,
                                "spesialisthelsetjenesten" to false,
                            )
                        status.treeResult.ruleHit shouldBeEqualTo
                            TilbakedateringRuleHit.INNTIL_8_DAGER.ruleHit
                    }
                }
                context("Uten Begrunnelse") {
                    test("Forlengelse, OK") {
                        val sykmelding =
                            generateSykmelding(
                                fom = LocalDate.now(),
                                tom = LocalDate.now().plusDays(1),
                                behandletTidspunkt = LocalDate.now().plusDays(5).atStartOfDay(),
                                kontaktMedPasient = KontaktMedPasient(null, null),
                            )
                        val forlengelse =
                            SykmeldingInfo(
                                "sykmeldingId",
                                sykmelding.perioder.first().fom,
                                sykmelding.perioder.first().tom,
                                null
                            )
                        val sykmeldingMetadata =
                            RuleMetadataSykmelding(
                                ruleMetadata = sykmelding.toRuleMetadata(),
                                SykmeldingMetadataInfo(null, forlengelse, LocalDate.now()),
                                doctorSuspensjon = false,
                                behandlerOgStartdato =
                                    BehandlerOgStartdato(
                                        Behandler(emptyList(), null),
                                        null,
                                    ),
                            )
                        val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)
                        status.treeResult.status shouldBeEqualTo Status.OK
                        status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                            listOf(
                                TILBAKEDATERING to true,
                                ETTERSENDING to false,
                                TILBAKEDATERT_INNTIL_4_DAGER to false,
                                TILBAKEDATERT_INNTIL_8_DAGER to true,
                                BEGRUNNELSE_MIN_1_ORD to false,
                                FORLENGELSE to true,
                            )
                        status.ruleInputs shouldBeEqualTo
                            mapOf(
                                "fom" to sykmelding.perioder.first().fom,
                                "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                                "begrunnelse" to "0 ord",
                                "forlengelse" to forlengelse,
                            )
                    }
                    test("Ikke forlengelse, INVALID") {
                        val sykmelding =
                            generateSykmelding(
                                fom = LocalDate.now(),
                                tom = LocalDate.now().plusDays(1),
                                behandletTidspunkt = LocalDate.now().plusDays(5).atStartOfDay(),
                                kontaktMedPasient = KontaktMedPasient(null, ""),
                            )
                        val sykmeldingMetadata =
                            RuleMetadataSykmelding(
                                ruleMetadata = sykmelding.toRuleMetadata(),
                                SykmeldingMetadataInfo(null, null, LocalDate.now()),
                                doctorSuspensjon = false,
                                behandlerOgStartdato =
                                    BehandlerOgStartdato(
                                        Behandler(emptyList(), null),
                                        null,
                                    ),
                            )
                        val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                        status.treeResult.status shouldBeEqualTo Status.INVALID
                        status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                            listOf(
                                TILBAKEDATERING to true,
                                ETTERSENDING to false,
                                TILBAKEDATERT_INNTIL_4_DAGER to false,
                                TILBAKEDATERT_INNTIL_8_DAGER to true,
                                BEGRUNNELSE_MIN_1_ORD to false,
                                FORLENGELSE to false,
                                SPESIALISTHELSETJENESTEN to false,
                            )
                        status.ruleInputs shouldBeEqualTo
                            mapOf(
                                "fom" to sykmelding.perioder.first().fom,
                                "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                                "begrunnelse" to "0 ord",
                                "diagnosesystem" to
                                    sykmelding.medisinskVurdering.hovedDiagnose?.system,
                                "spesialisthelsetjenesten" to false,
                            )
                    }

                    test("Ikke forlengelse, men fra spesialishelsetjenesten, OK") {
                        val sykmelding =
                            generateSykmelding(
                                fom = LocalDate.now(),
                                tom = LocalDate.now().plusDays(1),
                                behandletTidspunkt = LocalDate.now().plusDays(5).atStartOfDay(),
                                kontaktMedPasient = KontaktMedPasient(null, ""),
                                medisinskVurdering = fraSpesialhelsetjenesten(),
                            )
                        val sykmeldingMetadata =
                            RuleMetadataSykmelding(
                                ruleMetadata = sykmelding.toRuleMetadata(),
                                SykmeldingMetadataInfo(null, null, LocalDate.now()),
                                doctorSuspensjon = false,
                                behandlerOgStartdato =
                                    BehandlerOgStartdato(
                                        Behandler(emptyList(), null),
                                        null,
                                    ),
                            )
                        val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                        status.treeResult.status shouldBeEqualTo Status.OK
                        status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                            listOf(
                                TILBAKEDATERING to true,
                                ETTERSENDING to false,
                                TILBAKEDATERT_INNTIL_4_DAGER to false,
                                TILBAKEDATERT_INNTIL_8_DAGER to true,
                                BEGRUNNELSE_MIN_1_ORD to false,
                                FORLENGELSE to false,
                                SPESIALISTHELSETJENESTEN to true,
                            )
                        status.ruleInputs shouldBeEqualTo
                            mapOf(
                                "fom" to sykmelding.perioder.first().fom,
                                "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                                "begrunnelse" to "0 ord",
                                "diagnosesystem" to
                                    sykmelding.medisinskVurdering.hovedDiagnose?.system,
                                "spesialisthelsetjenesten" to true,
                            )
                    }
                }
            }
        }

        context("Test tilbakedatering mellog 8 og 30 dager") {
            context("uten begrunnelse") {
                test("Fra Spesialhelsetjenesten, OK") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(1),
                            behandletTidspunkt = LocalDate.now().plusDays(10).atStartOfDay(),
                            kontaktMedPasient = KontaktMedPasient(null, ""),
                            medisinskVurdering = fraSpesialhelsetjenesten(),
                        )
                    val sykmeldingMetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            SykmeldingMetadataInfo(null, null, LocalDate.now()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                    status.treeResult.status shouldBeEqualTo Status.OK
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERT_INNTIL_4_DAGER to false,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_MINDRE_ENN_1_MAANED to true,
                            BEGRUNNELSE_MIN_1_ORD to false,
                            SPESIALISTHELSETJENESTEN to true,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "begrunnelse" to "0 ord",
                            "diagnosesystem" to sykmelding.medisinskVurdering.hovedDiagnose?.system,
                            "spesialisthelsetjenesten" to true,
                        )
                }
                test("Ikke fra spesialhelsetjenesten, INVALID") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(1),
                            behandletTidspunkt = LocalDate.now().plusDays(20).atStartOfDay(),
                            kontaktMedPasient = KontaktMedPasient(null, null),
                        )
                    val sykmeldingMetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            SykmeldingMetadataInfo(null, null, LocalDate.now()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                    status.treeResult.status shouldBeEqualTo Status.INVALID
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERT_INNTIL_4_DAGER to false,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_MINDRE_ENN_1_MAANED to true,
                            BEGRUNNELSE_MIN_1_ORD to false,
                            SPESIALISTHELSETJENESTEN to false,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "begrunnelse" to "0 ord",
                            "diagnosesystem" to sykmelding.medisinskVurdering.hovedDiagnose?.system,
                            "spesialisthelsetjenesten" to false,
                        )

                    status.treeResult.ruleHit shouldBeEqualTo
                        TilbakedateringRuleHit.MINDRE_ENN_1_MAANED.ruleHit
                }
            }
            context("Med Begrunnelse") {
                test("ikke god nok begrunnelse, INVALID") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(1),
                            behandletTidspunkt = LocalDate.now().plusDays(10).atStartOfDay(),
                            kontaktMedPasient =
                                KontaktMedPasient(null, "12344123112341232....,,,..12"),
                        )
                    val sykmeldingMetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            SykmeldingMetadataInfo(null, null, LocalDate.now()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                    status.treeResult.status shouldBeEqualTo Status.INVALID
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERT_INNTIL_4_DAGER to false,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_MINDRE_ENN_1_MAANED to true,
                            BEGRUNNELSE_MIN_1_ORD to false,
                            SPESIALISTHELSETJENESTEN to false,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "begrunnelse" to
                                "${getNumberOfWords(sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt)} ord",
                            "diagnosesystem" to sykmelding.medisinskVurdering.hovedDiagnose?.system,
                            "spesialisthelsetjenesten" to false,
                        )
                }
                test("Forlengelse, OK") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(1),
                            behandletTidspunkt = LocalDate.now().plusDays(10).atStartOfDay(),
                            kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmnopq"),
                        )
                    val forlengelse =
                        SykmeldingInfo(
                            "sykmeldingId",
                            sykmelding.perioder.first().fom,
                            sykmelding.perioder.first().tom,
                            null
                        )
                    val sykmeldingMetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            SykmeldingMetadataInfo(null, forlengelse, LocalDate.now()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                    status.treeResult.status shouldBeEqualTo Status.OK
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERT_INNTIL_4_DAGER to false,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_MINDRE_ENN_1_MAANED to true,
                            BEGRUNNELSE_MIN_1_ORD to true,
                            FORLENGELSE to true,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "begrunnelse" to
                                "${getNumberOfWords(sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt)} ord",
                            "forlengelse" to forlengelse,
                        )
                }
                test("Ikke forlengelse, MANUELL") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(16),
                            behandletTidspunkt = LocalDate.now().plusDays(10).atStartOfDay(),
                            kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmnopq"),
                        )
                    coEvery { smregisterClient.getSykmeldinger("11234") } returns listOf()
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "11234",
                            sykmelding,
                            loggingMetadata
                        )
                    val rulemetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            sykmeldingMetadataInfo = sykmeldingMetadata,
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, rulemetadata)

                    status.treeResult.status shouldBeEqualTo Status.MANUAL_PROCESSING
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERT_INNTIL_4_DAGER to false,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_MINDRE_ENN_1_MAANED to true,
                            BEGRUNNELSE_MIN_1_ORD to true,
                            FORLENGELSE to false,
                            ARBEIDSGIVERPERIODE to false,
                            SPESIALISTHELSETJENESTEN to false,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "begrunnelse" to
                                "${getNumberOfWords(sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt)} ord",
                            "syketilfelletStartdato" to sykmelding.perioder.first().fom,
                            "tom" to sykmelding.perioder.first().tom,
                            "dagerForArbeidsgiverperiode" to
                                sykmeldingService.allDaysBetween(
                                    sykmelding.perioder.sortedFOMDate().first(),
                                    sykmelding.perioder.sortedTOMDate().last()
                                ),
                            "arbeidsgiverperiode" to false,
                            "diagnosesystem" to sykmelding.medisinskVurdering.hovedDiagnose?.system,
                            "spesialisthelsetjenesten" to false,
                        )
                }

                test("Innenfor arbeidsgiverperioden, OK") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(1),
                            behandletTidspunkt = LocalDate.now().plusDays(10).atStartOfDay(),
                            kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmnopq"),
                        )
                    coEvery { smregisterClient.getSykmeldinger("12345678901") } returns listOf()
                    val sykmeldingMetadataInfo =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "12345678901",
                            sykmelding,
                            loggingMetadata
                        )

                    val sykmeldingMetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            sykmeldingMetadataInfo,
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                    status.treeResult.status shouldBeEqualTo Status.OK
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERT_INNTIL_4_DAGER to false,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_MINDRE_ENN_1_MAANED to true,
                            BEGRUNNELSE_MIN_1_ORD to true,
                            FORLENGELSE to false,
                            ARBEIDSGIVERPERIODE to true,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "begrunnelse" to
                                "${getNumberOfWords(sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt)} ord",
                            "syketilfelletStartdato" to sykmelding.perioder.first().fom,
                            "tom" to sykmelding.perioder.first().tom,
                            "dagerForArbeidsgiverperiode" to
                                listOf(LocalDate.now(), LocalDate.now().plusDays(1)),
                            "arbeidsgiverperiode" to true,
                        )
                }
                test("Utenfor arbeidsgiverperioden, MANUELL") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(19),
                            behandletTidspunkt = LocalDate.now().plusDays(10).atStartOfDay(),
                            kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmnopq"),
                        )

                    coEvery { smregisterClient.getSykmeldinger(any()) } returns listOf()
                    val sykmeldingMetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            sykmeldingMetadataInfo =
                                sykmeldingService.getSykmeldingMetadataInfo(
                                    "12345678901",
                                    sykmelding,
                                    loggingMetadata
                                ),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                    status.treeResult.status shouldBeEqualTo Status.MANUAL_PROCESSING
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERT_INNTIL_4_DAGER to false,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_MINDRE_ENN_1_MAANED to true,
                            BEGRUNNELSE_MIN_1_ORD to true,
                            FORLENGELSE to false,
                            ARBEIDSGIVERPERIODE to false,
                            SPESIALISTHELSETJENESTEN to false,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "begrunnelse" to
                                "${getNumberOfWords(sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt)} ord",
                            "syketilfelletStartdato" to sykmelding.perioder.first().fom,
                            "tom" to sykmelding.perioder.first().tom,
                            "dagerForArbeidsgiverperiode" to
                                sykmeldingService.allDaysBetween(
                                    LocalDate.now(),
                                    LocalDate.now().plusDays(16)
                                ),
                            "arbeidsgiverperiode" to false,
                            "diagnosesystem" to sykmelding.medisinskVurdering.hovedDiagnose?.system,
                            "spesialisthelsetjenesten" to false,
                        )
                }

                test("Utenfor arbeidsgiverperioden andre sykmelding, MANUELL") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(2),
                            behandletTidspunkt = LocalDate.now().plusDays(10).atStartOfDay(),
                            kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmnopq"),
                        )
                    coEvery { smregisterClient.getSykmeldinger("11234") } returns
                        listOf(
                            generateSykmeldingDTO(
                                LocalDate.now().minusDays(20),
                                LocalDate.now().minusDays(3)
                            )
                        )

                    val actualSykmeldingMeta =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "11234",
                            sykmelding,
                            loggingMetadata,
                        )

                    val sykmeldingMetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            actualSykmeldingMeta,
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    LocalDate.now().minusDays(15),
                                ),
                        )

                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                    status.treeResult.status shouldBeEqualTo Status.MANUAL_PROCESSING
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERT_INNTIL_4_DAGER to false,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_MINDRE_ENN_1_MAANED to true,
                            BEGRUNNELSE_MIN_1_ORD to true,
                            FORLENGELSE to false,
                            ARBEIDSGIVERPERIODE to false,
                            SPESIALISTHELSETJENESTEN to false,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "begrunnelse" to
                                "${getNumberOfWords(sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt)} ord",
                            "syketilfelletStartdato" to
                                sykmeldingMetadata.behandlerOgStartdato.startdato,
                            "tom" to sykmelding.perioder.first().tom,
                            "dagerForArbeidsgiverperiode" to
                                actualSykmeldingMeta.dagerForArbeidsgiverperiodeCheck.sorted(),
                            "arbeidsgiverperiode" to false,
                            "diagnosesystem" to sykmelding.medisinskVurdering.hovedDiagnose?.system,
                            "spesialisthelsetjenesten" to false,
                        )
                }

                test("Fra spesialisthelsetjenesten, OK") {
                    val sykmelding =
                        generateSykmelding(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(19),
                            behandletTidspunkt = LocalDate.now().plusDays(10).atStartOfDay(),
                            kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmnopq"),
                            medisinskVurdering = fraSpesialhelsetjenesten(),
                        )
                    coEvery { smregisterClient.getSykmeldinger("11234") } returns listOf()
                    val info =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "11234",
                            sykmelding,
                            loggingMetadata
                        )
                    val sykmeldingMetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            info,
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                    status.treeResult.status shouldBeEqualTo Status.OK
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERT_INNTIL_4_DAGER to false,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_MINDRE_ENN_1_MAANED to true,
                            BEGRUNNELSE_MIN_1_ORD to true,
                            FORLENGELSE to false,
                            ARBEIDSGIVERPERIODE to false,
                            SPESIALISTHELSETJENESTEN to true,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "begrunnelse" to
                                "${getNumberOfWords(sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt)} ord",
                            "syketilfelletStartdato" to sykmelding.perioder.first().fom,
                            "tom" to sykmelding.perioder.first().tom,
                            "dagerForArbeidsgiverperiode" to
                                sykmeldingService
                                    .allDaysBetween(
                                        sykmelding.perioder.sortedFOMDate().first(),
                                        sykmelding.perioder.sortedTOMDate().last()
                                    )
                                    .take(17),
                            "arbeidsgiverperiode" to false,
                            "diagnosesystem" to sykmelding.medisinskVurdering.hovedDiagnose?.system,
                            "spesialisthelsetjenesten" to true,
                        )
                }
            }
        }

        context("Over 1 mnd") {
            test("Med begrunnelse, MANUELL") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(19),
                        behandletTidspunkt =
                            LocalDate.now().plusMonths(1).plusDays(1).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, "abc defgij kmnopq"),
                    )
                val sykmeldingMetadata =
                    RuleMetadataSykmelding(
                        ruleMetadata = sykmelding.toRuleMetadata(),
                        SykmeldingMetadataInfo(null, null, LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                status.treeResult.status shouldBeEqualTo Status.MANUAL_PROCESSING
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to false,
                        TILBAKEDATERT_INNTIL_4_DAGER to false,
                        TILBAKEDATERT_INNTIL_8_DAGER to false,
                        TILBAKEDATERT_MINDRE_ENN_1_MAANED to false,
                        BEGRUNNELSE_MIN_3_ORD to true,
                    )

                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                        "begrunnelse" to
                            "${getNumberOfWords(sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt)} ord",
                    )
            }

            test("Ikke god nok begrunnelse, INVALID") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(19),
                        behandletTidspunkt =
                            LocalDate.now().plusMonths(1).plusDays(1).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, "abc defghijklmno"),
                    )
                val sykmeldingMetadata =
                    RuleMetadataSykmelding(
                        ruleMetadata = sykmelding.toRuleMetadata(),
                        SykmeldingMetadataInfo(null, null, LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to false,
                        TILBAKEDATERT_INNTIL_4_DAGER to false,
                        TILBAKEDATERT_INNTIL_8_DAGER to false,
                        TILBAKEDATERT_MINDRE_ENN_1_MAANED to false,
                        BEGRUNNELSE_MIN_3_ORD to false,
                        SPESIALISTHELSETJENESTEN to false,
                    )

                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                        "begrunnelse" to
                            "${getNumberOfWords(sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt)} ord",
                        "diagnosesystem" to sykmelding.medisinskVurdering.hovedDiagnose?.system,
                        "spesialisthelsetjenesten" to false,
                    )
            }

            test("Fra spesialisthelsetjenesten, OK") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(19),
                        behandletTidspunkt =
                            LocalDate.now().plusMonths(1).plusDays(1).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmno"),
                        medisinskVurdering = fraSpesialhelsetjenesten(),
                    )
                val sykmeldingMetadata =
                    RuleMetadataSykmelding(
                        ruleMetadata = sykmelding.toRuleMetadata(),
                        SykmeldingMetadataInfo(null, null, LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                status.treeResult.status shouldBeEqualTo Status.OK
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to false,
                        TILBAKEDATERT_INNTIL_4_DAGER to false,
                        TILBAKEDATERT_INNTIL_8_DAGER to false,
                        TILBAKEDATERT_MINDRE_ENN_1_MAANED to false,
                        BEGRUNNELSE_MIN_3_ORD to false,
                        SPESIALISTHELSETJENESTEN to true,
                    )

                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                        "begrunnelse" to
                            "${getNumberOfWords(sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt)} ord",
                        "diagnosesystem" to sykmelding.medisinskVurdering.hovedDiagnose?.system,
                        "spesialisthelsetjenesten" to true,
                    )
            }

            test("meir enn 1 måned og 32 dager") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.of(2024, 7, 30),
                        tom = LocalDate.of(2024, 7, 31),
                        behandletTidspunkt = LocalDate.of(2024, 8, 31).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, "abcghgfgh"),
                    )
                val sykmeldingMetadata =
                    RuleMetadataSykmelding(
                        ruleMetadata = sykmelding.toRuleMetadata(),
                        SykmeldingMetadataInfo(null, null, LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                status.treeResult.status shouldBeEqualTo Status.INVALID

                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to false,
                        TILBAKEDATERT_INNTIL_4_DAGER to false,
                        TILBAKEDATERT_INNTIL_8_DAGER to false,
                        TILBAKEDATERT_MINDRE_ENN_1_MAANED to false,
                        BEGRUNNELSE_MIN_3_ORD to false,
                        SPESIALISTHELSETJENESTEN to false,
                    )

                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                        "begrunnelse" to
                            "${getNumberOfWords(sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt)} ord",
                        "diagnosesystem" to sykmelding.medisinskVurdering.hovedDiagnose?.system,
                        "spesialisthelsetjenesten" to false,
                    )
            }

            test("mindre enn én måned, men 31 dager") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.of(2024, 7, 30),
                        tom = LocalDate.of(2024, 7, 31),
                        behandletTidspunkt = LocalDate.of(2024, 8, 30).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, "abcghgfgh"),
                    )
                coEvery { smregisterClient.getSykmeldinger("11234") } returns listOf()
                val sykmeldingMetadataInfo =
                    sykmeldingService.getSykmeldingMetadataInfo(
                        "11234",
                        sykmelding,
                        loggingMetadata
                    )

                val sykmeldingMetadata =
                    RuleMetadataSykmelding(
                        ruleMetadata = sykmelding.toRuleMetadata(),
                        sykmeldingMetadataInfo,
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                status.treeResult.status shouldBeEqualTo Status.OK

                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to false,
                        TILBAKEDATERT_INNTIL_4_DAGER to false,
                        TILBAKEDATERT_INNTIL_8_DAGER to false,
                        TILBAKEDATERT_MINDRE_ENN_1_MAANED to true,
                        BEGRUNNELSE_MIN_1_ORD to true,
                        FORLENGELSE to false,
                        ARBEIDSGIVERPERIODE to true,
                    )

                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                        "begrunnelse" to
                            "${getNumberOfWords(sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt)} ord",
                        "syketilfelletStartdato" to sykmelding.perioder.first().fom,
                        "tom" to sykmelding.perioder.first().tom,
                        "dagerForArbeidsgiverperiode" to
                            listOf(LocalDate.of(2024, 7, 30), LocalDate.of(2024, 7, 31)),
                        "arbeidsgiverperiode" to true,
                    )
            }

            test("ikke mindre enn én måned, men 29 dager") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.of(2024, 1, 28),
                        tom = LocalDate.of(2024, 2, 1),
                        behandletTidspunkt = LocalDate.of(2024, 2, 29).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, "abcxcxzc"),
                        medisinskVurdering = fraSpesialhelsetjenesten(),
                    )
                val sykmeldingMetadata =
                    RuleMetadataSykmelding(
                        ruleMetadata = sykmelding.toRuleMetadata(),
                        SykmeldingMetadataInfo(null, null, LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata)

                status.treeResult.status shouldBeEqualTo Status.OK

                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to false,
                        TILBAKEDATERT_INNTIL_4_DAGER to false,
                        TILBAKEDATERT_INNTIL_8_DAGER to false,
                        TILBAKEDATERT_MINDRE_ENN_1_MAANED to false,
                        BEGRUNNELSE_MIN_3_ORD to false,
                        SPESIALISTHELSETJENESTEN to true,
                    )

                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                        "begrunnelse" to
                            "${getNumberOfWords(sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt)} ord",
                        "diagnosesystem" to sykmelding.medisinskVurdering.hovedDiagnose?.system,
                        "spesialisthelsetjenesten" to true,
                    )
            }
        }
    })

private fun fraSpesialhelsetjenesten() =
    MedisinskVurdering(
        hovedDiagnose = Diagnosekoder.icd10.values.first().toDiagnose(),
        biDiagnoser = emptyList(),
        svangerskap = false,
        yrkesskade = false,
        yrkesskadeDato = null,
        annenFraversArsak = null,
    )

fun Sykmelding.toRuleMetadata() =
    RuleMetadata(
        signatureDate = signaturDato,
        receivedDate = signaturDato,
        behandletTidspunkt = behandletTidspunkt,
        patientPersonNumber = "1",
        rulesetVersion = null,
        legekontorOrgnr = null,
        tssid = null,
        avsenderFnr = "2",
        pasientFodselsdato = LocalDate.now(),
    )
