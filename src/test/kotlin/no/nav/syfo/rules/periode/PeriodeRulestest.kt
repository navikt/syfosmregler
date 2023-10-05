package no.nav.syfo.rules.periode

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Status
import no.nav.syfo.rules.tilbakedatering.toRuleMetadata
import no.nav.syfo.rules.validation.ruleMetadataSykmelding
import no.nav.syfo.services.sortedFOMDate
import no.nav.syfo.services.sortedTOMDate
import org.amshove.kluent.shouldBeEqualTo

class PeriodeRulestest :
    FunSpec({
        val periodeRules = PeriodeRulesExecution()
        context("test periode regler") {
            test("OK") {
                val sykmelding = generateSykmelding()
                val ruleMetadata = ruleMetadataSykmelding(sykmelding.toRuleMetadata())
                val status = periodeRules.runRules(sykmelding, ruleMetadata).first
                status.treeResult.status shouldBeEqual Status.OK
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodeRules.FREMDATERT to false,
                        PeriodeRules.TILBAKEDATERT_MER_ENN_3_AR to false,
                        PeriodeRules.TOTAL_VARIGHET_OVER_ETT_AAR to false
                    )
                mapOf(
                        "genereringsTidspunkt" to ruleMetadata.ruleMetadata.signatureDate,
                        "fom" to sykmelding.perioder.sortedFOMDate().first(),
                        "tom" to sykmelding.perioder.sortedTOMDate().last(),
                        "fremdatert" to false,
                        "tilbakeDatertMerEnn3AAr" to false,
                        "varighetOver1AAr" to false,
                    )
                    .toSortedMap() shouldBeEqualTo status.ruleInputs.toSortedMap()
            }

            test("Fremdater over 30 dager, Status INVALID") {
                val sykmelding =
                    generateSykmelding(
                        perioder =
                            listOf(
                                generatePeriode(
                                    fom = LocalDate.now().plusDays(31),
                                    tom = LocalDate.now().plusDays(37),
                                ),
                            ),
                        behandletTidspunkt = LocalDateTime.now(),
                    )

                val ruleMetadata = sykmelding.toRuleMetadata()

                val status =
                    periodeRules.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodeRules.FREMDATERT to true,
                    )

                mapOf(
                    "genereringsTidspunkt" to ruleMetadata.signatureDate,
                    "fom" to sykmelding.perioder.sortedFOMDate().first(),
                    "fremdatert" to true
                ) shouldBeEqualTo status.ruleInputs

                status.treeResult.ruleHit shouldBeEqualTo PeriodeRuleHit.FREMDATERT.ruleHit
            }

            test("Varighet over 1 år, Status INVALID") {
                val sykmelding =
                    generateSykmelding(
                        perioder =
                            listOf(
                                generatePeriode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now(),
                                ),
                                generatePeriode(
                                    fom = LocalDate.now().plusDays(1),
                                    tom = LocalDate.now().plusDays(366),
                                ),
                            ),
                        behandletTidspunkt = LocalDateTime.now(),
                    )

                val ruleMetadata = sykmelding.toRuleMetadata()

                val status =
                    periodeRules.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodeRules.FREMDATERT to false,
                        PeriodeRules.TILBAKEDATERT_MER_ENN_3_AR to false,
                        PeriodeRules.TOTAL_VARIGHET_OVER_ETT_AAR to true,
                    )

                mapOf(
                        "genereringsTidspunkt" to ruleMetadata.signatureDate,
                        "fom" to sykmelding.perioder.sortedFOMDate().first(),
                        "tom" to sykmelding.perioder.sortedTOMDate().last(),
                        "fremdatert" to false,
                        "tilbakeDatertMerEnn3AAr" to false,
                        "varighetOver1AAr" to true,
                    )
                    .toSortedMap() shouldBeEqualTo status.ruleInputs.toSortedMap()

                status.treeResult.ruleHit shouldBeEqualTo
                    PeriodeRuleHit.TOTAL_VARIGHET_OVER_ETT_AAR.ruleHit
            }
        }
    })
