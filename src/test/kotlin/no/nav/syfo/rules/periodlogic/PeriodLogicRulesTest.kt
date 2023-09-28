package no.nav.syfo.rules.periodlogic

import io.kotest.core.spec.style.FunSpec
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.syfo.generateGradert
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Periode
import no.nav.syfo.model.Status
import no.nav.syfo.rules.periodvalidering.PeriodLogicRuleHit
import no.nav.syfo.rules.periodvalidering.PeriodLogicRules
import no.nav.syfo.rules.periodvalidering.PeriodLogicRulesExecution
import no.nav.syfo.rules.tilbakedatering.toRuleMetadata
import no.nav.syfo.rules.validation.ruleMetadataSykmelding
import org.amshove.kluent.shouldBeEqualTo

class PeriodLogicRulesTest :
    FunSpec({
        val ruleTree = PeriodLogicRulesExecution()

        context("Testing periodLogic rules and checking the rule outcomes") {
            test("Alt er ok, Status OK") {
                val sykmelding =
                    generateSykmelding(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(15),
                        behandletTidspunkt = LocalDate.now().atStartOfDay(),
                    )
                val ruleMetadata = sykmelding.toRuleMetadata()

                val status =
                    ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.OK
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodLogicRules.PERIODER_MANGLER to false,
                        PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                        PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                        PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                        PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                        PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                        PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to false,
                        PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER to false,
                        PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER to false,
                        PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE to false,
                        PeriodLogicRules.GRADERT_SYKMELDING_OVER_99_PROSENT to false,
                        PeriodLogicRules.SYKMELDING_MED_BEHANDLINGSDAGER to false,
                    )

                mapOf(
                    "perioder" to sykmelding.perioder,
                    "periodeRanges" to
                        sykmelding.perioder.sortedBy { it.fom }.map { it.fom to it.tom },
                    "behandslingsDatoEtterMottatDato" to false,
                    "avventendeKombinert" to false,
                    "manglendeInnspillArbeidsgiver" to false,
                    "avventendeOver16Dager" to false,
                    "forMangeBehandlingsDagerPrUke" to false,
                    "gradertOver99Prosent" to false,
                    "inneholderBehandlingsDager" to false,
                ) shouldBeEqualTo status.ruleInputs

                status.treeResult.ruleHit shouldBeEqualTo null
            }

            test("Periode mangler, Status INVALID") {
                val sykmelding =
                    generateSykmelding(
                        perioder = listOf(),
                    )
                val ruleMetadata = sykmelding.toRuleMetadata()

                val status =
                    ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodLogicRules.PERIODER_MANGLER to true,
                    )

                mapOf(
                    "perioder" to sykmelding.perioder,
                ) shouldBeEqualTo status.ruleInputs

                status.treeResult.ruleHit shouldBeEqualTo
                    PeriodLogicRuleHit.PERIODER_MANGLER.ruleHit
            }

            test("Fra dato er etter til dato, Status INVALID") {
                val sykmelding =
                    generateSykmelding(
                        perioder =
                            listOf(
                                generatePeriode(
                                    fom = LocalDate.of(2018, 1, 9),
                                    tom = LocalDate.of(2018, 1, 7),
                                ),
                            ),
                    )

                val ruleMetadata = sykmelding.toRuleMetadata()

                val status =
                    ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodLogicRules.PERIODER_MANGLER to false,
                        PeriodLogicRules.FRADATO_ETTER_TILDATO to true,
                    )

                mapOf(
                    "perioder" to sykmelding.perioder,
                ) shouldBeEqualTo status.ruleInputs

                status.treeResult.ruleHit shouldBeEqualTo
                    PeriodLogicRuleHit.FRADATO_ETTER_TILDATO.ruleHit
            }

            test("Overlapp i perioder, Status INVALID") {
                val sykmelding =
                    generateSykmelding(
                        perioder =
                            listOf(
                                generatePeriode(
                                    fom = LocalDate.of(2018, 1, 7),
                                    tom = LocalDate.of(2018, 1, 9),
                                ),
                                generatePeriode(
                                    fom = LocalDate.of(2018, 1, 8),
                                    tom = LocalDate.of(2018, 1, 12),
                                ),
                            ),
                    )

                val ruleMetadata = sykmelding.toRuleMetadata()

                val status =
                    ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodLogicRules.PERIODER_MANGLER to false,
                        PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                        PeriodLogicRules.OVERLAPPENDE_PERIODER to true,
                    )

                mapOf(
                    "perioder" to sykmelding.perioder,
                ) shouldBeEqualTo status.ruleInputs

                status.treeResult.ruleHit shouldBeEqualTo
                    PeriodLogicRuleHit.OVERLAPPENDE_PERIODER.ruleHit
            }

            test("Opphold mellom perioder, Status INVALID") {
                val sykmelding =
                    generateSykmelding(
                        perioder =
                            listOf(
                                generatePeriode(
                                    fom = LocalDate.of(2018, 1, 1),
                                    tom = LocalDate.of(2018, 1, 3),
                                ),
                                generatePeriode(
                                    fom = LocalDate.of(2018, 1, 4),
                                    tom = LocalDate.of(2018, 1, 9),
                                ),
                                generatePeriode(
                                    fom = LocalDate.of(2018, 1, 10),
                                    tom = LocalDate.of(2018, 1, 11),
                                ),
                                generatePeriode(
                                    fom = LocalDate.of(2018, 1, 15),
                                    tom = LocalDate.of(2018, 1, 20),
                                ),
                            ),
                    )

                val ruleMetadata = sykmelding.toRuleMetadata()

                val status =
                    ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodLogicRules.PERIODER_MANGLER to false,
                        PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                        PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                        PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to true,
                    )

                mapOf(
                    "perioder" to sykmelding.perioder,
                    "periodeRanges" to
                        sykmelding.perioder.sortedBy { it.fom }.map { it.fom to it.tom },
                ) shouldBeEqualTo status.ruleInputs

                status.treeResult.ruleHit shouldBeEqualTo
                    PeriodLogicRuleHit.OPPHOLD_MELLOM_PERIODER.ruleHit
            }
            test("Ikke definert periode, Status INVALID") {
                val sykmelding =
                    generateSykmelding(
                        perioder =
                            listOf(
                                Periode(
                                    fom = LocalDate.of(2018, 2, 1),
                                    tom = LocalDate.of(2018, 2, 2),
                                    aktivitetIkkeMulig = null,
                                    gradert = null,
                                    avventendeInnspillTilArbeidsgiver = null,
                                    reisetilskudd = false,
                                    behandlingsdager = 0,
                                ),
                            ),
                    )

                val ruleMetadata = sykmelding.toRuleMetadata()

                val status =
                    ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodLogicRules.PERIODER_MANGLER to false,
                        PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                        PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                        PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                        PeriodLogicRules.IKKE_DEFINERT_PERIODE to true,
                    )

                mapOf(
                    "perioder" to sykmelding.perioder,
                    "periodeRanges" to
                        sykmelding.perioder.sortedBy { it.fom }.map { it.fom to it.tom },
                    "perioder" to sykmelding.perioder,
                ) shouldBeEqualTo status.ruleInputs

                status.treeResult.ruleHit shouldBeEqualTo
                    PeriodLogicRuleHit.IKKE_DEFINERT_PERIODE.ruleHit
            }

            test("BehandlingsDato etter mottatDato, Status INVALID") {
                val sykmelding =
                    generateSykmelding(
                        behandletTidspunkt = LocalDateTime.now().plusDays(2),
                    )

                val ruleMetadata =
                    sykmelding
                        .toRuleMetadata()
                        .copy(receivedDate = sykmelding.behandletTidspunkt.minusDays(2))

                val status =
                    ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodLogicRules.PERIODER_MANGLER to false,
                        PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                        PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                        PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                        PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                        PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to true,
                    )

                mapOf(
                    "perioder" to sykmelding.perioder,
                    "periodeRanges" to
                        sykmelding.perioder.sortedBy { it.fom }.map { it.fom to it.tom },
                    "perioder" to sykmelding.perioder,
                    "behandslingsDatoEtterMottatDato" to true,
                ) shouldBeEqualTo status.ruleInputs

                status.treeResult.ruleHit shouldBeEqualTo
                    PeriodLogicRuleHit.BEHANDLINGSDATO_ETTER_MOTTATTDATO.ruleHit
            }

            test("Avvendte kombinert med annen type periode, Status INVALID") {
                val sykmelding =
                    generateSykmelding(
                        perioder =
                            listOf(
                                generatePeriode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusDays(5),
                                    avventendeInnspillTilArbeidsgiver =
                                        "Bør gå minst mulig på jobb",
                                ),
                                generatePeriode(
                                    fom = LocalDate.now().plusDays(6),
                                    tom = LocalDate.now().plusDays(10),
                                ),
                            ),
                    )

                val ruleMetadata = sykmelding.toRuleMetadata()

                val status =
                    ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodLogicRules.PERIODER_MANGLER to false,
                        PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                        PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                        PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                        PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                        PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                        PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to true,
                    )

                mapOf(
                    "perioder" to sykmelding.perioder,
                    "periodeRanges" to
                        sykmelding.perioder.sortedBy { it.fom }.map { it.fom to it.tom },
                    "perioder" to sykmelding.perioder,
                    "behandslingsDatoEtterMottatDato" to false,
                    "avventendeKombinert" to true,
                ) shouldBeEqualTo status.ruleInputs

                status.treeResult.ruleHit shouldBeEqualTo
                    PeriodLogicRuleHit.AVVENTENDE_SYKMELDING_KOMBINERT.ruleHit
            }

            test("Manglende innstill til arbeidsgiver, Status INVALID") {
                val sykmelding =
                    generateSykmelding(
                        perioder =
                            listOf(
                                generatePeriode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusDays(5),
                                    avventendeInnspillTilArbeidsgiver = "      ",
                                ),
                            ),
                    )

                val ruleMetadata = sykmelding.toRuleMetadata()

                val status =
                    ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodLogicRules.PERIODER_MANGLER to false,
                        PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                        PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                        PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                        PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                        PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                        PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to false,
                        PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER to true,
                    )

                mapOf(
                    "perioder" to sykmelding.perioder,
                    "perioder" to sykmelding.perioder,
                    "perioder" to sykmelding.perioder,
                    "periodeRanges" to
                        sykmelding.perioder.sortedBy { it.fom }.map { it.fom to it.tom },
                    "perioder" to sykmelding.perioder,
                    "behandslingsDatoEtterMottatDato" to false,
                    "avventendeKombinert" to false,
                    "manglendeInnspillArbeidsgiver" to true,
                ) shouldBeEqualTo status.ruleInputs

                status.treeResult.ruleHit shouldBeEqualTo
                    PeriodLogicRuleHit.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER.ruleHit
            }

            test("Avventende over 16 dager, Status INVALID") {
                val sykmelding =
                    generateSykmelding(
                        perioder =
                            listOf(
                                generatePeriode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusDays(17),
                                    avventendeInnspillTilArbeidsgiver =
                                        "Bør gå minst mulig på jobb",
                                ),
                            ),
                    )

                val ruleMetadata = sykmelding.toRuleMetadata()

                val status =
                    ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodLogicRules.PERIODER_MANGLER to false,
                        PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                        PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                        PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                        PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                        PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                        PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to false,
                        PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER to false,
                        PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER to true,
                    )

                mapOf(
                    "perioder" to sykmelding.perioder,
                    "periodeRanges" to
                        sykmelding.perioder.sortedBy { it.fom }.map { it.fom to it.tom },
                    "perioder" to sykmelding.perioder,
                    "behandslingsDatoEtterMottatDato" to false,
                    "avventendeKombinert" to false,
                    "manglendeInnspillArbeidsgiver" to false,
                    "avventendeOver16Dager" to true,
                ) shouldBeEqualTo status.ruleInputs

                status.treeResult.ruleHit shouldBeEqualTo
                    PeriodLogicRuleHit.AVVENTENDE_SYKMELDING_OVER_16_DAGER.ruleHit
            }

            test("For mange behandlingsdager pr uke, Status INVALID") {
                val sykmelding =
                    generateSykmelding(
                        perioder =
                            listOf(
                                generatePeriode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now(),
                                    behandlingsdager = 2,
                                ),
                            ),
                    )

                val ruleMetadata = sykmelding.toRuleMetadata()

                val status =
                    ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodLogicRules.PERIODER_MANGLER to false,
                        PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                        PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                        PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                        PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                        PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                        PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to false,
                        PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER to false,
                        PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER to false,
                        PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE to true,
                    )

                mapOf(
                    "perioder" to sykmelding.perioder,
                    "periodeRanges" to
                        sykmelding.perioder.sortedBy { it.fom }.map { it.fom to it.tom },
                    "perioder" to sykmelding.perioder,
                    "behandslingsDatoEtterMottatDato" to false,
                    "avventendeKombinert" to false,
                    "manglendeInnspillArbeidsgiver" to false,
                    "avventendeOver16Dager" to false,
                    "forMangeBehandlingsDagerPrUke" to true,
                ) shouldBeEqualTo status.ruleInputs

                status.treeResult.ruleHit shouldBeEqualTo
                    PeriodLogicRuleHit.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE.ruleHit
            }

            test("Gradert over 99 prosent, Status INVALID") {
                val sykmelding =
                    generateSykmelding(
                        perioder =
                            listOf(
                                generatePeriode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now(),
                                    gradert = generateGradert(grad = 100),
                                ),
                            ),
                    )

                val ruleMetadata = sykmelding.toRuleMetadata()

                val status =
                    ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.INVALID
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodLogicRules.PERIODER_MANGLER to false,
                        PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                        PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                        PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                        PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                        PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                        PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to false,
                        PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER to false,
                        PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER to false,
                        PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE to false,
                        PeriodLogicRules.GRADERT_SYKMELDING_OVER_99_PROSENT to true,
                    )

                mapOf(
                    "perioder" to sykmelding.perioder,
                    "periodeRanges" to
                        sykmelding.perioder.sortedBy { it.fom }.map { it.fom to it.tom },
                    "perioder" to sykmelding.perioder,
                    "behandslingsDatoEtterMottatDato" to false,
                    "avventendeKombinert" to false,
                    "manglendeInnspillArbeidsgiver" to false,
                    "avventendeOver16Dager" to false,
                    "forMangeBehandlingsDagerPrUke" to false,
                    "gradertOver99Prosent" to true,
                ) shouldBeEqualTo status.ruleInputs

                status.treeResult.ruleHit shouldBeEqualTo
                    PeriodLogicRuleHit.GRADERT_SYKMELDING_OVER_99_PROSENT.ruleHit
            }

            test("Inneholder behandlingsdager, Status MANUAL_PROCESSING") {
                val sykmelding =
                    generateSykmelding(
                        perioder =
                            listOf(
                                generatePeriode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now(),
                                    behandlingsdager = 1,
                                ),
                            ),
                    )

                val ruleMetadata = sykmelding.toRuleMetadata()

                val status =
                    ruleTree.runRules(sykmelding, ruleMetadataSykmelding(ruleMetadata)).first

                status.treeResult.status shouldBeEqualTo Status.MANUAL_PROCESSING
                status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo
                    listOf(
                        PeriodLogicRules.PERIODER_MANGLER to false,
                        PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                        PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                        PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                        PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                        PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                        PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to false,
                        PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER to false,
                        PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER to false,
                        PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE to false,
                        PeriodLogicRules.GRADERT_SYKMELDING_OVER_99_PROSENT to false,
                        PeriodLogicRules.SYKMELDING_MED_BEHANDLINGSDAGER to true,
                    )

                mapOf(
                    "perioder" to sykmelding.perioder,
                    "periodeRanges" to
                        sykmelding.perioder.sortedBy { it.fom }.map { it.fom to it.tom },
                    "perioder" to sykmelding.perioder,
                    "behandslingsDatoEtterMottatDato" to false,
                    "avventendeKombinert" to false,
                    "manglendeInnspillArbeidsgiver" to false,
                    "avventendeOver16Dager" to false,
                    "forMangeBehandlingsDagerPrUke" to false,
                    "gradertOver99Prosent" to false,
                    "inneholderBehandlingsDager" to true,
                ) shouldBeEqualTo status.ruleInputs

                status.treeResult.ruleHit shouldBeEqualTo
                    PeriodLogicRuleHit.SYKMELDING_MED_BEHANDLINGSDAGER.ruleHit
            }
        }
    })
