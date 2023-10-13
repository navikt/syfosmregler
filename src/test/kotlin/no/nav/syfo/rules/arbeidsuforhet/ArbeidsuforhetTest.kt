package no.nav.syfo.rules.arbeidsuforhet

import io.kotest.core.spec.style.FunSpec
import java.time.LocalDate
import no.nav.syfo.client.Behandler
import no.nav.syfo.generateMedisinskVurdering
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.rules.validation.generatePersonNumber
import no.nav.syfo.services.BehandlerOgStartdato
import no.nav.syfo.services.RuleMetadataSykmelding
import no.nav.syfo.services.SykmeldingMetadataInfo
import no.nav.syfo.sm.Diagnosekoder
import no.nav.syfo.toDiagnose
import org.amshove.kluent.shouldBeEqualTo

class ArbeidsuforhetTest :
    FunSpec({
        val ruleTree = ArbeidsuforhetRulesExecution()

        test("Ukjent diagnoseKodeType, Status INVALID") {
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
                    sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                    doctorSuspensjon = false,
                    behandlerOgStartdato = BehandlerOgStartdato(Behandler(emptyList(), null), null),
                )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

            status.first.treeResult.status shouldBeEqualTo Status.INVALID
            status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                listOf(
                    ArbeidsuforhetRules.UKJENT_DIAGNOSEKODETYPE to true,
                )

            mapOf(
                "ukjentDiagnoseKodeType" to true,
            ) shouldBeEqualTo status.first.ruleInputs

            status.first.treeResult.ruleHit shouldBeEqualTo
                ArbeidsuforhetRuleHit.UKJENT_DIAGNOSEKODETYPE.ruleHit
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
                    sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                    doctorSuspensjon = false,
                    behandlerOgStartdato = BehandlerOgStartdato(Behandler(emptyList(), null), null),
                )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

            status.first.treeResult.status shouldBeEqualTo Status.INVALID
            status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                listOf(
                    ArbeidsuforhetRules.UKJENT_DIAGNOSEKODETYPE to false,
                    ArbeidsuforhetRules.ICPC_2_Z_DIAGNOSE to true,
                )

            mapOf(
                "ukjentDiagnoseKodeType" to false,
                "icpc2ZDiagnose" to true,
            ) shouldBeEqualTo status.first.ruleInputs

            status.first.treeResult.ruleHit shouldBeEqualTo
                ArbeidsuforhetRuleHit.ICPC_2_Z_DIAGNOSE.ruleHit
        }
        test("HouvedDiagnose eller fraversgrunn mangler, Status INVALID") {
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
                    sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                    doctorSuspensjon = false,
                    behandlerOgStartdato = BehandlerOgStartdato(Behandler(emptyList(), null), null),
                )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

            status.first.treeResult.status shouldBeEqualTo Status.INVALID
            status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                listOf(
                    ArbeidsuforhetRules.UKJENT_DIAGNOSEKODETYPE to false,
                    ArbeidsuforhetRules.ICPC_2_Z_DIAGNOSE to false,
                    ArbeidsuforhetRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER to true,
                )

            mapOf(
                "ukjentDiagnoseKodeType" to false,
                "icpc2ZDiagnose" to false,
                "houvedDiagnoseEllerFraversgrunnMangler" to true,
            ) shouldBeEqualTo status.first.ruleInputs

            status.first.treeResult.ruleHit shouldBeEqualTo
                ArbeidsuforhetRuleHit.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER.ruleHit
        }
        test("Ugyldig KodeVerk for houvedDiagnose, Status INVALID") {
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
                    sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                    doctorSuspensjon = false,
                    behandlerOgStartdato = BehandlerOgStartdato(Behandler(emptyList(), null), null),
                )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

            status.first.treeResult.status shouldBeEqualTo Status.INVALID
            status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                listOf(
                    ArbeidsuforhetRules.UKJENT_DIAGNOSEKODETYPE to false,
                    ArbeidsuforhetRules.ICPC_2_Z_DIAGNOSE to false,
                    ArbeidsuforhetRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER to false,
                    ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to true,
                )

            mapOf(
                "ukjentDiagnoseKodeType" to false,
                "icpc2ZDiagnose" to false,
                "houvedDiagnoseEllerFraversgrunnMangler" to false,
                "ugyldigKodeVerkHouvedDiagnose" to true,
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
                            hovedDiagnose = Diagnose(
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
                    sykmeldingMetadataInfo = SykmeldingMetadataInfo(null, emptyList()),
                    doctorSuspensjon = false,
                    behandlerOgStartdato = BehandlerOgStartdato(Behandler(emptyList(), null), null),
                )

            val status = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

            status.first.treeResult.status shouldBeEqualTo Status.INVALID
            status.first.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                listOf(
                    ArbeidsuforhetRules.UKJENT_DIAGNOSEKODETYPE to false,
                    ArbeidsuforhetRules.ICPC_2_Z_DIAGNOSE to false,
                    ArbeidsuforhetRules.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER to false,
                    ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE to false,
                    ArbeidsuforhetRules.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE to true,
                )

            mapOf(
                "ukjentDiagnoseKodeType" to false,
                "icpc2ZDiagnose" to false,
                "houvedDiagnoseEllerFraversgrunnMangler" to false,
                "ugyldigKodeVerkHouvedDiagnose" to false,
                "ugyldigKodeVerkBiDiagnose" to true,
            ) shouldBeEqualTo status.first.ruleInputs

            status.first.treeResult.ruleHit shouldBeEqualTo
                ArbeidsuforhetRuleHit.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE.ruleHit
        }
    })
