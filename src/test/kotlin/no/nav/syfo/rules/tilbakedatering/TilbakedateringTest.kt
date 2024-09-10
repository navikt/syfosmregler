package no.nav.syfo.rules.tilbakedatering

import io.kotest.core.spec.style.FunSpec
import java.time.LocalDate
import no.nav.helse.diagnosekoder.Diagnosekoder
import no.nav.syfo.client.Behandler
import no.nav.syfo.generateSykmelding
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
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERING_OVER_4_DAGER
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_INNTIL_1_MAANDE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_INNTIL_8_DAGER
import no.nav.syfo.services.BehandlerOgStartdato
import no.nav.syfo.services.Forlengelse
import no.nav.syfo.services.RuleMetadataSykmelding
import no.nav.syfo.services.SykmeldingMetadataInfo
import no.nav.syfo.toDiagnose
import org.amshove.kluent.shouldBeEqualTo

class TilbakedateringTest :
    FunSpec({
        val ruleTree = TilbakedateringRulesExecution()

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
                        SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first
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
                        SykmeldingMetadataInfo("sykmeldingID", emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first
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
                        "ettersending" to true,
                        "ettersendingAv" to "sykmeldingID",
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
                        SykmeldingMetadataInfo("sykmeldingID", emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first
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
                        "ettersending" to true,
                        "ettersendingAv" to "sykmeldingID",
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
                        SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first
                status.treeResult.status shouldBeEqualTo Status.OK
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to false,
                        TILBAKEDATERING_OVER_4_DAGER to false,
                    )
                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                        "ettersending" to false,
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
                                SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                                doctorSuspensjon = false,
                                behandlerOgStartdato =
                                    BehandlerOgStartdato(
                                        Behandler(emptyList(), null),
                                        null,
                                    ),
                            )
                        val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first
                        status.treeResult.status shouldBeEqualTo Status.OK
                        status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                            listOf(
                                TILBAKEDATERING to true,
                                ETTERSENDING to false,
                                TILBAKEDATERING_OVER_4_DAGER to true,
                                TILBAKEDATERT_INNTIL_8_DAGER to true,
                                BEGRUNNELSE_MIN_1_ORD to true,
                            )
                        status.ruleInputs shouldBeEqualTo
                            mapOf(
                                "fom" to sykmelding.perioder.first().fom,
                                "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                                "ettersending" to false,
                                "begrunnelse" to
                                    sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
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
                                SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                                doctorSuspensjon = false,
                                behandlerOgStartdato =
                                    BehandlerOgStartdato(
                                        Behandler(emptyList(), null),
                                        null,
                                    ),
                            )
                        val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first
                        status.treeResult.status shouldBeEqualTo Status.INVALID
                        status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                            listOf(
                                TILBAKEDATERING to true,
                                ETTERSENDING to false,
                                TILBAKEDATERING_OVER_4_DAGER to true,
                                TILBAKEDATERT_INNTIL_8_DAGER to true,
                                BEGRUNNELSE_MIN_1_ORD to false,
                                FORLENGELSE to false,
                                SPESIALISTHELSETJENESTEN to false,
                            )
                        status.ruleInputs shouldBeEqualTo
                            mapOf(
                                "fom" to sykmelding.perioder.first().fom,
                                "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                                "ettersending" to false,
                                "begrunnelse" to "",
                                "forlengelse" to false,
                                "hoveddiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
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
                            Forlengelse(
                                "sykmeldingId",
                                sykmelding.perioder.first().fom,
                                sykmelding.perioder.first().tom
                            )
                        val sykmeldingMetadata =
                            RuleMetadataSykmelding(
                                ruleMetadata = sykmelding.toRuleMetadata(),
                                SykmeldingMetadataInfo(
                                    null,
                                    listOf(
                                        forlengelse,
                                    ),
                                    LocalDate.now()
                                ),
                                doctorSuspensjon = false,
                                behandlerOgStartdato =
                                    BehandlerOgStartdato(
                                        Behandler(emptyList(), null),
                                        null,
                                    ),
                            )
                        val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first
                        status.treeResult.status shouldBeEqualTo Status.OK
                        status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                            listOf(
                                TILBAKEDATERING to true,
                                ETTERSENDING to false,
                                TILBAKEDATERING_OVER_4_DAGER to true,
                                TILBAKEDATERT_INNTIL_8_DAGER to true,
                                BEGRUNNELSE_MIN_1_ORD to false,
                                FORLENGELSE to true,
                            )
                        status.ruleInputs shouldBeEqualTo
                            mapOf(
                                "fom" to sykmelding.perioder.first().fom,
                                "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                                "ettersending" to false,
                                "begrunnelse" to "",
                                "forlengelse" to true,
                                "forlengelseAv" to listOf(forlengelse),
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
                                SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                                doctorSuspensjon = false,
                                behandlerOgStartdato =
                                    BehandlerOgStartdato(
                                        Behandler(emptyList(), null),
                                        null,
                                    ),
                            )
                        val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                        status.treeResult.status shouldBeEqualTo Status.INVALID
                        status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                            listOf(
                                TILBAKEDATERING to true,
                                ETTERSENDING to false,
                                TILBAKEDATERING_OVER_4_DAGER to true,
                                TILBAKEDATERT_INNTIL_8_DAGER to true,
                                BEGRUNNELSE_MIN_1_ORD to false,
                                FORLENGELSE to false,
                                SPESIALISTHELSETJENESTEN to false,
                            )
                        status.ruleInputs shouldBeEqualTo
                            mapOf(
                                "fom" to sykmelding.perioder.first().fom,
                                "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                                "ettersending" to false,
                                "begrunnelse" to "",
                                "forlengelse" to false,
                                "hoveddiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
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
                                SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                                doctorSuspensjon = false,
                                behandlerOgStartdato =
                                    BehandlerOgStartdato(
                                        Behandler(emptyList(), null),
                                        null,
                                    ),
                            )
                        val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                        status.treeResult.status shouldBeEqualTo Status.OK
                        status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                            listOf(
                                TILBAKEDATERING to true,
                                ETTERSENDING to false,
                                TILBAKEDATERING_OVER_4_DAGER to true,
                                TILBAKEDATERT_INNTIL_8_DAGER to true,
                                BEGRUNNELSE_MIN_1_ORD to false,
                                FORLENGELSE to false,
                                SPESIALISTHELSETJENESTEN to true,
                            )
                        status.ruleInputs shouldBeEqualTo
                            mapOf(
                                "fom" to sykmelding.perioder.first().fom,
                                "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                                "ettersending" to false,
                                "begrunnelse" to "",
                                "forlengelse" to false,
                                "hoveddiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
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
                            SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                    status.treeResult.status shouldBeEqualTo Status.OK
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERING_OVER_4_DAGER to true,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_INNTIL_1_MAANDE to true,
                            BEGRUNNELSE_MIN_1_ORD to false,
                            SPESIALISTHELSETJENESTEN to true,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "ettersending" to false,
                            "begrunnelse" to "",
                            "hoveddiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
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
                            SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                    status.treeResult.status shouldBeEqualTo Status.INVALID
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERING_OVER_4_DAGER to true,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_INNTIL_1_MAANDE to true,
                            BEGRUNNELSE_MIN_1_ORD to false,
                            SPESIALISTHELSETJENESTEN to false,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "ettersending" to false,
                            "begrunnelse" to "",
                            "hoveddiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
                            "spesialisthelsetjenesten" to false,
                        )

                    status.treeResult.ruleHit shouldBeEqualTo
                        TilbakedateringRuleHit.INNTIL_30_DAGER.ruleHit
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
                            SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                    status.treeResult.status shouldBeEqualTo Status.INVALID
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERING_OVER_4_DAGER to true,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_INNTIL_1_MAANDE to true,
                            BEGRUNNELSE_MIN_1_ORD to false,
                            SPESIALISTHELSETJENESTEN to false,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "ettersending" to false,
                            "begrunnelse" to sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
                            "hoveddiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
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
                        Forlengelse(
                            "sykmeldingId",
                            sykmelding.perioder.first().fom,
                            sykmelding.perioder.first().tom
                        )
                    val sykmeldingMetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            SykmeldingMetadataInfo(null, listOf(forlengelse), LocalDate.now()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                    status.treeResult.status shouldBeEqualTo Status.OK
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERING_OVER_4_DAGER to true,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_INNTIL_1_MAANDE to true,
                            BEGRUNNELSE_MIN_1_ORD to true,
                            FORLENGELSE to true,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "ettersending" to false,
                            "begrunnelse" to sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
                            "forlengelse" to true,
                            "forlengelseAv" to listOf(forlengelse),
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
                    val sykmeldingMetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                    status.treeResult.status shouldBeEqualTo Status.MANUAL_PROCESSING
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERING_OVER_4_DAGER to true,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_INNTIL_1_MAANDE to true,
                            BEGRUNNELSE_MIN_1_ORD to true,
                            FORLENGELSE to false,
                            ARBEIDSGIVERPERIODE to false,
                            SPESIALISTHELSETJENESTEN to false,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "ettersending" to false,
                            "begrunnelse" to sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
                            "forlengelse" to false,
                            "syketilfelletStartdato" to sykmelding.perioder.first().fom,
                            "tom" to sykmelding.perioder.first().tom,
                            "arbeidsgiverperiode" to false,
                            "hoveddiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
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
                    val sykmeldingMetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                    status.treeResult.status shouldBeEqualTo Status.OK
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERING_OVER_4_DAGER to true,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_INNTIL_1_MAANDE to true,
                            BEGRUNNELSE_MIN_1_ORD to true,
                            FORLENGELSE to false,
                            ARBEIDSGIVERPERIODE to true,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "ettersending" to false,
                            "begrunnelse" to sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
                            "forlengelse" to false,
                            "syketilfelletStartdato" to sykmelding.perioder.first().fom,
                            "tom" to sykmelding.perioder.first().tom,
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
                    val sykmeldingMetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                    status.treeResult.status shouldBeEqualTo Status.MANUAL_PROCESSING
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERING_OVER_4_DAGER to true,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_INNTIL_1_MAANDE to true,
                            BEGRUNNELSE_MIN_1_ORD to true,
                            FORLENGELSE to false,
                            ARBEIDSGIVERPERIODE to false,
                            SPESIALISTHELSETJENESTEN to false,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "ettersending" to false,
                            "begrunnelse" to sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
                            "forlengelse" to false,
                            "syketilfelletStartdato" to sykmelding.perioder.first().fom,
                            "tom" to sykmelding.perioder.first().tom,
                            "arbeidsgiverperiode" to false,
                            "hoveddiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
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
                    val sykmeldingMetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    LocalDate.now().minusDays(15),
                                ),
                        )

                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                    status.treeResult.status shouldBeEqualTo Status.MANUAL_PROCESSING
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERING_OVER_4_DAGER to true,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_INNTIL_1_MAANDE to true,
                            BEGRUNNELSE_MIN_1_ORD to true,
                            FORLENGELSE to false,
                            ARBEIDSGIVERPERIODE to false,
                            SPESIALISTHELSETJENESTEN to false,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "ettersending" to false,
                            "begrunnelse" to sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
                            "forlengelse" to false,
                            "syketilfelletStartdato" to
                                sykmeldingMetadata.behandlerOgStartdato.startdato,
                            "tom" to sykmelding.perioder.first().tom,
                            "arbeidsgiverperiode" to false,
                            "hoveddiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
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
                    val sykmeldingMetadata =
                        RuleMetadataSykmelding(
                            ruleMetadata = sykmelding.toRuleMetadata(),
                            SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                            doctorSuspensjon = false,
                            behandlerOgStartdato =
                                BehandlerOgStartdato(
                                    Behandler(emptyList(), null),
                                    null,
                                ),
                        )
                    val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                    status.treeResult.status shouldBeEqualTo Status.OK
                    status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            TILBAKEDATERING to true,
                            ETTERSENDING to false,
                            TILBAKEDATERING_OVER_4_DAGER to true,
                            TILBAKEDATERT_INNTIL_8_DAGER to false,
                            TILBAKEDATERT_INNTIL_1_MAANDE to true,
                            BEGRUNNELSE_MIN_1_ORD to true,
                            FORLENGELSE to false,
                            ARBEIDSGIVERPERIODE to false,
                            SPESIALISTHELSETJENESTEN to true,
                        )
                    status.ruleInputs shouldBeEqualTo
                        mapOf(
                            "fom" to sykmelding.perioder.first().fom,
                            "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                            "ettersending" to false,
                            "begrunnelse" to sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
                            "forlengelse" to false,
                            "syketilfelletStartdato" to sykmelding.perioder.first().fom,
                            "tom" to sykmelding.perioder.first().tom,
                            "arbeidsgiverperiode" to false,
                            "hoveddiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
                            "spesialisthelsetjenesten" to true,
                        )
                }
            }
        }

        context("Over 30 dager") {
            test("Med begrunnelse, MANUELL") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(19),
                        behandletTidspunkt = LocalDate.now().plusDays(31).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, "abc defgij kmnopq"),
                    )
                val sykmeldingMetadata =
                    RuleMetadataSykmelding(
                        ruleMetadata = sykmelding.toRuleMetadata(),
                        SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                status.treeResult.status shouldBeEqualTo Status.MANUAL_PROCESSING
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to false,
                        TILBAKEDATERING_OVER_4_DAGER to true,
                        TILBAKEDATERT_INNTIL_8_DAGER to false,
                        TILBAKEDATERT_INNTIL_1_MAANDE to false,
                        BEGRUNNELSE_MIN_3_ORD to true,
                    )

                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                        "ettersending" to false,
                        "begrunnelse" to sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
                    )
            }

            test("Ikke god nok begrunnelse, INVALID") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(19),
                        behandletTidspunkt = LocalDate.now().plusDays(31).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, "abc defghijklmno"),
                    )
                val sykmeldingMetadata =
                    RuleMetadataSykmelding(
                        ruleMetadata = sykmelding.toRuleMetadata(),
                        SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to false,
                        TILBAKEDATERING_OVER_4_DAGER to true,
                        TILBAKEDATERT_INNTIL_8_DAGER to false,
                        TILBAKEDATERT_INNTIL_1_MAANDE to false,
                        BEGRUNNELSE_MIN_3_ORD to false,
                        SPESIALISTHELSETJENESTEN to false,
                    )

                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                        "ettersending" to false,
                        "begrunnelse" to sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
                        "hoveddiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
                        "spesialisthelsetjenesten" to false,
                    )
            }

            test("Fra spesialisthelsetjenesten, MANUELL") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(19),
                        behandletTidspunkt = LocalDate.now().plusDays(31).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmno"),
                        medisinskVurdering = fraSpesialhelsetjenesten(),
                    )
                val sykmeldingMetadata =
                    RuleMetadataSykmelding(
                        ruleMetadata = sykmelding.toRuleMetadata(),
                        SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                status.treeResult.status shouldBeEqualTo Status.MANUAL_PROCESSING
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to false,
                        TILBAKEDATERING_OVER_4_DAGER to true,
                        TILBAKEDATERT_INNTIL_8_DAGER to false,
                        TILBAKEDATERT_INNTIL_1_MAANDE to false,
                        BEGRUNNELSE_MIN_3_ORD to false,
                        SPESIALISTHELSETJENESTEN to true,
                    )

                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                        "ettersending" to false,
                        "begrunnelse" to sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
                        "hoveddiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
                        "spesialisthelsetjenesten" to true,
                    )
            }

            test("mindre enn 1 måned men 30 dager") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.of(2024, 7, 30),
                        tom = LocalDate.of(2024, 7, 31),
                        behandletTidspunkt = LocalDate.of(2024, 8, 29).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, "abcghgfgh"),
                    )
                val sykmeldingMetadata =
                    RuleMetadataSykmelding(
                        ruleMetadata = sykmelding.toRuleMetadata(),
                        SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                status.treeResult.status shouldBeEqualTo Status.OK

                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to false,
                        TILBAKEDATERING_OVER_4_DAGER to true,
                        TILBAKEDATERT_INNTIL_8_DAGER to false,
                        TILBAKEDATERT_INNTIL_1_MAANDE to true,
                        BEGRUNNELSE_MIN_1_ORD to true,
                        FORLENGELSE to false,
                        ARBEIDSGIVERPERIODE to true,
                    )

                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                        "ettersending" to false,
                        "begrunnelse" to sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
                        "forlengelse" to false,
                        "syketilfelletStartdato" to sykmelding.perioder.first().fom,
                        "tom" to sykmelding.perioder.first().tom,
                        "arbeidsgiverperiode" to true,
                    )
            }

            test("Er meir enn 1 måned men 29 dager") {
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
                        SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(
                                Behandler(emptyList(), null),
                                null,
                            ),
                    )
                val status = ruleTree.runRules(sykmelding, sykmeldingMetadata).first

                status.treeResult.status shouldBeEqualTo Status.MANUAL_PROCESSING

                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        TILBAKEDATERING to true,
                        ETTERSENDING to false,
                        TILBAKEDATERING_OVER_4_DAGER to true,
                        TILBAKEDATERT_INNTIL_8_DAGER to false,
                        TILBAKEDATERT_INNTIL_1_MAANDE to false,
                        BEGRUNNELSE_MIN_3_ORD to false,
                        SPESIALISTHELSETJENESTEN to true,
                    )

                status.ruleInputs shouldBeEqualTo
                    mapOf(
                        "fom" to sykmelding.perioder.first().fom,
                        "genereringstidspunkt" to sykmelding.signaturDato.toLocalDate(),
                        "ettersending" to false,
                        "begrunnelse" to sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
                        "hoveddiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
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
