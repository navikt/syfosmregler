package no.nav.syfo.rules.arbeidsuforhet

import io.kotest.core.spec.style.FunSpec
import java.time.LocalDate
import no.nav.helse.diagnosekoder.Diagnosekoder
import no.nav.syfo.client.Behandler
import no.nav.syfo.generateMedisinskVurdering
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.AnnenFraversArsak
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.objectMapper
import no.nav.syfo.rules.validation.generatePersonNumber
import no.nav.syfo.services.BehandlerOgStartdato
import no.nav.syfo.services.RuleMetadataSykmelding
import no.nav.syfo.services.SykmeldingMetadataInfo
import no.nav.syfo.toDiagnose
import org.amshove.kluent.shouldBeEqualTo

class ArbeidsuforhetTest :
    FunSpec(
        {
            val ruleTree = ArbeidsuforhetRulesExecution()
            val person31Years = LocalDate.now().minusYears(31)
            context("test diagnoser") {
                val ruleMetadata =
                    RuleMetadata(
                        signatureDate = LocalDate.now().atStartOfDay(),
                        receivedDate = LocalDate.now().atStartOfDay(),
                        behandletTidspunkt = LocalDate.now().atStartOfDay(),
                        patientPersonNumber = generatePersonNumber(person31Years, false),
                        rulesetVersion = null,
                        legekontorOrgnr = null,
                        tssid = null,
                        avsenderFnr = "2",
                        pasientFodselsdato = person31Years,
                    )
                val ruleMetadataSykmelding =
                    RuleMetadataSykmelding(
                        ruleMetadata = ruleMetadata,
                        sykmeldingMetadataInfo =
                            SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(Behandler(emptyList(), null), null),
                    )

                test("Hoveddiagnose is null and annen Fraværsårsak is null") {
                    val sykmelding =
                        generateSykmelding(
                            medisinskVurdering =
                                generateMedisinskVurdering(
                                    hovedDiagnose = null,
                                    annenFraversArsak = null,
                                ),
                        )

                    val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                    status.first.treeResult.status shouldBeEqualTo Status.INVALID
                    status.first.treeResult.ruleHit shouldBeEqualTo
                        ArbeidsuforhetRuleHit.FRAVAERSGRUNN_MANGLER.ruleHit
                }

                test("Hoveddiagnose is null and annen Fraværsårsak is not null") {
                    val sykmelding =
                        generateSykmelding(
                            medisinskVurdering =
                                generateMedisinskVurdering(
                                    hovedDiagnose = null,
                                    annenFraversArsak =
                                        AnnenFraversArsak(
                                            beskrivelse = "beskrivelse",
                                            grunn = emptyList(),
                                        ),
                                ),
                        )

                    val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                    status.first.treeResult.status shouldBeEqualTo Status.OK
                    status.first.treeResult.ruleHit shouldBeEqualTo null
                }
                test(
                    "Hoveddiagnose is null and annen Fraværsårsak beskrivelse is null and grunn is empty"
                ) {
                    val sykmelding =
                        generateSykmelding(
                            medisinskVurdering =
                                generateMedisinskVurdering(
                                    hovedDiagnose = null,
                                    annenFraversArsak =
                                        AnnenFraversArsak(
                                            beskrivelse = null,
                                            grunn = emptyList(),
                                        ),
                                ),
                        )

                    val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                    status.first.treeResult.status shouldBeEqualTo Status.INVALID
                    status.first.treeResult.ruleHit shouldBeEqualTo
                        ArbeidsuforhetRuleHit.FRAVAERSGRUNN_MANGLER.ruleHit
                }

                test("Mangler hoveddiagnose, annen fravarsgrunn, ugylidg diagnose") {
                    val sykmelding =
                        generateSykmelding(
                            medisinskVurdering =
                                generateMedisinskVurdering(
                                    hovedDiagnose = null,
                                    annenFraversArsak =
                                        AnnenFraversArsak(
                                            beskrivelse = "beskrivelse",
                                            grunn = emptyList(),
                                        ),
                                    bidiagnoser =
                                        listOf(
                                            Diagnose(
                                                system = "2.16.578.1.12.4.1.1.7170",
                                                kode = "R222222",
                                                tekst = "Blodig oppspytt/hemoptyse",
                                            )
                                        )
                                ),
                        )

                    val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                    status.first.treeResult.status shouldBeEqualTo Status.INVALID
                    status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            ArbeidsuforhetRules.HOVEDDIAGNOSE_MANGLER to true,
                            ArbeidsuforhetRules.FRAVAERSGRUNN_MANGLER to false,
                            ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE to true,
                        )

                    mapOf(
                        "hovedDiagnose" to EmptyObject,
                        "annenFraversArsak" to sykmelding.medisinskVurdering.annenFraversArsak,
                        "biDiagnoser" to sykmelding.medisinskVurdering.biDiagnoser,
                    ) shouldBeEqualTo status.first.ruleInputs

                    status.first.treeResult.ruleHit shouldBeEqualTo
                        ArbeidsuforhetRuleHit.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE.ruleHit
                }

                test("All OK") {
                    val sykmelding =
                        generateSykmelding(
                            medisinskVurdering =
                                generateMedisinskVurdering(
                                    hovedDiagnose =
                                        Diagnose(
                                            system = "2.16.578.1.12.4.1.1.7170",
                                            kode = "R24",
                                            tekst = "Blodig oppspytt/hemoptyse",
                                        ),
                                    annenFraversArsak = null,
                                    bidiagnoser =
                                        listOf(
                                            Diagnose(
                                                system = "2.16.578.1.12.4.1.1.7170",
                                                kode = "R24",
                                                tekst = "Blodig oppspytt/hemoptyse",
                                            )
                                        )
                                ),
                        )

                    val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                    status.first.treeResult.status shouldBeEqualTo Status.OK
                    status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                        listOf(
                            ArbeidsuforhetRules.HOVEDDIAGNOSE_MANGLER to false,
                            ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to false,
                            ArbeidsuforhetRules.ICPC_2_Z_DIAGNOSE to false,
                            ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE to false,
                        )

                    mapOf(
                        "hovedDiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
                        "ugyldigKodeverkHovedDiagnose" to false,
                        "icpc2ZDiagnose" to false,
                        "biDiagnoser" to sykmelding.medisinskVurdering.biDiagnoser,
                    ) shouldBeEqualTo status.first.ruleInputs

                    status.first.treeResult.ruleHit shouldBeEqualTo null
                }
            }

            test("Ugyldig kodeverk for hoveddiagnose, Status INVALID") {
                val person31Years = LocalDate.now().minusYears(31)

                val sykmelding =
                    generateSykmelding(
                        medisinskVurdering =
                            generateMedisinskVurdering(
                                hovedDiagnose =
                                    Diagnose(
                                        system = "2.16.578.1.12.4.1.1.9999",
                                        kode = "A09",
                                        tekst = "Brudd legg/ankel",
                                    ),
                            ),
                    )

                val ruleMetadata =
                    RuleMetadata(
                        signatureDate = LocalDate.now().atStartOfDay(),
                        receivedDate = LocalDate.now().atStartOfDay(),
                        behandletTidspunkt = LocalDate.now().atStartOfDay(),
                        patientPersonNumber = generatePersonNumber(person31Years, false),
                        rulesetVersion = null,
                        legekontorOrgnr = null,
                        tssid = null,
                        avsenderFnr = "2",
                        pasientFodselsdato = person31Years,
                    )

                val ruleMetadataSykmelding =
                    RuleMetadataSykmelding(
                        ruleMetadata = ruleMetadata,
                        sykmeldingMetadataInfo =
                            SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(Behandler(emptyList(), null), null),
                    )

                val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                status.first.treeResult.status shouldBeEqualTo Status.INVALID
                status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        ArbeidsuforhetRules.HOVEDDIAGNOSE_MANGLER to false,
                        ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to true,
                    )

                mapOf(
                    "hovedDiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
                    "ugyldigKodeverkHovedDiagnose" to true,
                ) shouldBeEqualTo status.first.ruleInputs

                status.first.treeResult.ruleHit shouldBeEqualTo
                    ArbeidsuforhetRuleHit.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE.ruleHit
            }

            test("Diagnosen er icpz 2 z diagnose, Status INVALID") {
                val person31Years = LocalDate.now().minusYears(31)

                val sykmelding =
                    generateSykmelding(
                        medisinskVurdering =
                            generateMedisinskVurdering(
                                hovedDiagnose = Diagnosekoder.icpc2["Z09"]!!.toDiagnose(),
                            ),
                    )

                val ruleMetadata =
                    RuleMetadata(
                        signatureDate = LocalDate.now().atStartOfDay(),
                        receivedDate = LocalDate.now().atStartOfDay(),
                        behandletTidspunkt = LocalDate.now().atStartOfDay(),
                        patientPersonNumber = generatePersonNumber(person31Years, false),
                        rulesetVersion = null,
                        legekontorOrgnr = null,
                        tssid = null,
                        avsenderFnr = "2",
                        pasientFodselsdato = person31Years,
                    )

                val ruleMetadataSykmelding =
                    RuleMetadataSykmelding(
                        ruleMetadata = ruleMetadata,
                        sykmeldingMetadataInfo =
                            SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(Behandler(emptyList(), null), null),
                    )

                val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                status.first.treeResult.status shouldBeEqualTo Status.INVALID
                status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        ArbeidsuforhetRules.HOVEDDIAGNOSE_MANGLER to false,
                        ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to false,
                        ArbeidsuforhetRules.ICPC_2_Z_DIAGNOSE to true,
                    )

                mapOf(
                    "hovedDiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
                    "ugyldigKodeverkHovedDiagnose" to false,
                    "icpc2ZDiagnose" to true,
                ) shouldBeEqualTo status.first.ruleInputs

                status.first.treeResult.ruleHit shouldBeEqualTo
                    ArbeidsuforhetRuleHit.ICPC_2_Z_DIAGNOSE.ruleHit
            }
            test("HovedDiagnose og fraversgrunn mangler, Status INVALID") {
                val person31Years = LocalDate.now().minusYears(31)

                val sykmelding =
                    generateSykmelding(
                        medisinskVurdering =
                            generateMedisinskVurdering(
                                hovedDiagnose = null,
                                annenFraversArsak = null,
                            ),
                    )

                val ruleMetadata =
                    RuleMetadata(
                        signatureDate = LocalDate.now().atStartOfDay(),
                        receivedDate = LocalDate.now().atStartOfDay(),
                        behandletTidspunkt = LocalDate.now().atStartOfDay(),
                        patientPersonNumber = generatePersonNumber(person31Years, false),
                        rulesetVersion = null,
                        legekontorOrgnr = null,
                        tssid = null,
                        avsenderFnr = "2",
                        pasientFodselsdato = person31Years,
                    )

                val ruleMetadataSykmelding =
                    RuleMetadataSykmelding(
                        ruleMetadata = ruleMetadata,
                        sykmeldingMetadataInfo =
                            SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(Behandler(emptyList(), null), null),
                    )

                val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                status.first.treeResult.status shouldBeEqualTo Status.INVALID
                status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        ArbeidsuforhetRules.HOVEDDIAGNOSE_MANGLER to true,
                        ArbeidsuforhetRules.FRAVAERSGRUNN_MANGLER to true,
                    )

                mapOf(
                    "hovedDiagnose" to EmptyObject,
                    "annenFraversArsak" to EmptyObject,
                ) shouldBeEqualTo status.first.ruleInputs
                val string = objectMapper.writeValueAsString(status.first.ruleInputs)
                status.first.treeResult.ruleHit shouldBeEqualTo
                    ArbeidsuforhetRuleHit.FRAVAERSGRUNN_MANGLER.ruleHit
            }
            test("Ugyldig Kodeverk for houvedDiagnose, Status INVALID") {
                val person31Years = LocalDate.now().minusYears(31)

                val sykmelding =
                    generateSykmelding(
                        medisinskVurdering =
                            generateMedisinskVurdering(
                                hovedDiagnose =
                                    Diagnose(
                                        system = "2.16.578.1.12.4.1.1.7110",
                                        kode = "Z09",
                                        tekst = "Brudd legg/ankel",
                                    ),
                            ),
                    )

                val ruleMetadata =
                    RuleMetadata(
                        signatureDate = LocalDate.now().atStartOfDay(),
                        receivedDate = LocalDate.now().atStartOfDay(),
                        behandletTidspunkt = LocalDate.now().atStartOfDay(),
                        patientPersonNumber = generatePersonNumber(person31Years, false),
                        rulesetVersion = null,
                        legekontorOrgnr = null,
                        tssid = null,
                        avsenderFnr = "2",
                        pasientFodselsdato = person31Years,
                    )

                val ruleMetadataSykmelding =
                    RuleMetadataSykmelding(
                        ruleMetadata = ruleMetadata,
                        sykmeldingMetadataInfo =
                            SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(Behandler(emptyList(), null), null),
                    )

                val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                status.first.treeResult.status shouldBeEqualTo Status.INVALID
                status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        ArbeidsuforhetRules.HOVEDDIAGNOSE_MANGLER to false,
                        ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to true
                    )

                mapOf(
                    "hovedDiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
                    "ugyldigKodeverkHovedDiagnose" to true,
                ) shouldBeEqualTo status.first.ruleInputs

                status.first.treeResult.ruleHit shouldBeEqualTo
                    ArbeidsuforhetRuleHit.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE.ruleHit
            }
            test("Ugyldig kodeVerk for biDiagnose, Status INVALID") {
                val person31Years = LocalDate.now().minusYears(31)

                val sykmelding =
                    generateSykmelding(
                        medisinskVurdering =
                            generateMedisinskVurdering(
                                hovedDiagnose =
                                    Diagnose(
                                        system = "2.16.578.1.12.4.1.1.7170",
                                        kode = "R24",
                                        tekst = "Blodig oppspytt/hemoptyse",
                                    ),
                                bidiagnoser =
                                    listOf(
                                        Diagnose(
                                            system = "2.16.578.1.12.4.1.1.7110",
                                            kode = "S09",
                                            tekst = "Brudd legg/ankel",
                                        ),
                                    ),
                            ),
                    )

                val ruleMetadata =
                    RuleMetadata(
                        signatureDate = LocalDate.now().atStartOfDay(),
                        receivedDate = LocalDate.now().atStartOfDay(),
                        behandletTidspunkt = LocalDate.now().atStartOfDay(),
                        patientPersonNumber = generatePersonNumber(person31Years, false),
                        rulesetVersion = null,
                        legekontorOrgnr = null,
                        tssid = null,
                        avsenderFnr = "2",
                        pasientFodselsdato = person31Years,
                    )

                val ruleMetadataSykmelding =
                    RuleMetadataSykmelding(
                        ruleMetadata = ruleMetadata,
                        sykmeldingMetadataInfo =
                            SykmeldingMetadataInfo(null, emptyList(), LocalDate.now()),
                        doctorSuspensjon = false,
                        behandlerOgStartdato =
                            BehandlerOgStartdato(Behandler(emptyList(), null), null),
                    )

                val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

                status.first.treeResult.status shouldBeEqualTo Status.INVALID
                status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        ArbeidsuforhetRules.HOVEDDIAGNOSE_MANGLER to false,
                        ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to false,
                        ArbeidsuforhetRules.ICPC_2_Z_DIAGNOSE to false,
                        ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE to true,
                    )

                mapOf(
                    "hovedDiagnose" to sykmelding.medisinskVurdering.hovedDiagnose,
                    "ugyldigKodeverkHovedDiagnose" to false,
                    "icpc2ZDiagnose" to false,
                    "biDiagnoser" to sykmelding.medisinskVurdering.biDiagnoser,
                ) shouldBeEqualTo status.first.ruleInputs

                status.first.treeResult.ruleHit shouldBeEqualTo
                    ArbeidsuforhetRuleHit.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE.ruleHit
            }
        },
    )
