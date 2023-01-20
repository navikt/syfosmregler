package no.nav.syfo.rules.periodlogic

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.generateGradert
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Periode
import no.nav.syfo.model.Status
import no.nav.syfo.rules.tilbakedatering.toRuleMetadata
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate
import java.time.LocalDateTime

class PeriodLogicRulesTest : FunSpec({
    val ruleTree = PeriodLogicRulesExecution()

    context("Testing periodLogic rules and checking the rule outcomes") {
        test("Alt er ok, Status OK") {
            val sykmelding = generateSykmelding(
                fom = LocalDate.now(),
                tom = LocalDate.now().plusDays(15),
                behandletTidspunkt = LocalDate.now().atStartOfDay()
            )
            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.OK
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR to false,
                PeriodLogicRules.FREMDATERT to false,
                PeriodLogicRules.TOTAL_VARIGHET_OVER_ETT_AAR to false,
                PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to false,
                PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER to false,
                PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER to false,
                PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE to false,
                PeriodLogicRules.GRADERT_SYKMELDING_UNDER_20_PROSENT to false,
                PeriodLogicRules.GRADERT_SYKMELDING_OVER_99_PROSENT to false,
                PeriodLogicRules.SYKMELDING_MED_BEHANDLINGSDAGER to false,

            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to false,
                "oppholdMellomPerioder" to false,
                "ikkeDefinertPeriode" to false,
                "tilbakeDatertMerEnn3AAr" to false,
                "fremdatert" to false,
                "varighetOver1AAr" to false,
                "behandslingsDatoEtterMottatDato" to false,
                "avventendeKombinert" to false,
                "manglendeInnspillArbeidsgiver" to false,
                "avventendeOver16Dager" to false,
                "forMangeBehandlingsDagerPrUke" to false,
                "gradertUnder20Prosent" to false,
                "gradertOver99Prosent" to false,
                "inneholderBehandlingsDager" to false
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo null
        }

        test("Periode mangler, Status INVALID") {
            val sykmelding = generateSykmelding(
                perioder = listOf()
            )
            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to true
            )

            mapOf(
                "periodeMangler" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.PERIODER_MANGLER
        }

        test("Fra dato er etter til dato, Status INVALID") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 9),
                        tom = LocalDate.of(2018, 1, 7)
                    )
                )
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.FRADATO_ETTER_TILDATO
        }

        test("Overlapp i perioder, Status INVALID") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 7),
                        tom = LocalDate.of(2018, 1, 9)
                    ),
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 8),
                        tom = LocalDate.of(2018, 1, 12)
                    )
                )
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.OVERLAPPENDE_PERIODER
        }

        test("Opphold mellom perioder, Status INVALID") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 1),
                        tom = LocalDate.of(2018, 1, 3)
                    ),
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 4),
                        tom = LocalDate.of(2018, 1, 9)
                    ),
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 10),
                        tom = LocalDate.of(2018, 1, 11)
                    ),
                    generatePeriode(
                        fom = LocalDate.of(2018, 1, 15),
                        tom = LocalDate.of(2018, 1, 20)
                    )
                )
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to false,
                "oppholdMellomPerioder" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.OPPHOLD_MELLOM_PERIODER
        }
        test("Ikke definert periode, Status INVALID") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    Periode(
                        fom = LocalDate.of(2018, 2, 1),
                        tom = LocalDate.of(2018, 2, 2),
                        aktivitetIkkeMulig = null,
                        gradert = null,
                        avventendeInnspillTilArbeidsgiver = null,
                        reisetilskudd = false,
                        behandlingsdager = 0
                    )
                )
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                PeriodLogicRules.IKKE_DEFINERT_PERIODE to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to false,
                "oppholdMellomPerioder" to false,
                "ikkeDefinertPeriode" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.IKKE_DEFINERT_PERIODE
        }
        test("Tilbakedatert mer enn 3 år, Status INVALID") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    Periode(
                        fom = LocalDate.now().minusYears(3).minusDays(14),
                        tom = LocalDate.now().minusYears(3),
                        aktivitetIkkeMulig = null,
                        avventendeInnspillTilArbeidsgiver = null,
                        behandlingsdager = 1,
                        gradert = null,
                        reisetilskudd = false
                    )
                ),
                behandletTidspunkt = LocalDateTime.now()
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to false,
                "oppholdMellomPerioder" to false,
                "ikkeDefinertPeriode" to false,
                "tilbakeDatertMerEnn3AAr" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.TILBAKEDATERT_MER_ENN_3_AR
        }

        test("Fremdater over 30 dager, Status INVALID") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now().plusDays(31),
                        tom = LocalDate.now().plusDays(37)
                    )
                ),
                behandletTidspunkt = LocalDateTime.now()
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR to false,
                PeriodLogicRules.FREMDATERT to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to false,
                "oppholdMellomPerioder" to false,
                "ikkeDefinertPeriode" to false,
                "tilbakeDatertMerEnn3AAr" to false,
                "fremdatert" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.FREMDATERT
        }

        test("Varighet over 1 år, Status INVALID") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    ),
                    generatePeriode(
                        fom = LocalDate.now().plusDays(1),
                        tom = LocalDate.now().plusDays(366)
                    )
                ),
                behandletTidspunkt = LocalDateTime.now()
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR to false,
                PeriodLogicRules.FREMDATERT to false,
                PeriodLogicRules.TOTAL_VARIGHET_OVER_ETT_AAR to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to false,
                "oppholdMellomPerioder" to false,
                "ikkeDefinertPeriode" to false,
                "tilbakeDatertMerEnn3AAr" to false,
                "fremdatert" to false,
                "varighetOver1AAr" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.TOTAL_VARIGHET_OVER_ETT_AAR
        }

        test("BehandlingsDato etter mottatDato, Status INVALID") {
            val sykmelding = generateSykmelding(
                behandletTidspunkt = LocalDateTime.now().plusDays(2)
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR to false,
                PeriodLogicRules.FREMDATERT to false,
                PeriodLogicRules.TOTAL_VARIGHET_OVER_ETT_AAR to false,
                PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to false,
                "oppholdMellomPerioder" to false,
                "ikkeDefinertPeriode" to false,
                "tilbakeDatertMerEnn3AAr" to false,
                "fremdatert" to false,
                "varighetOver1AAr" to false,
                "behandslingsDatoEtterMottatDato" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.BEHANDLINGSDATO_ETTER_MOTTATTDATO
        }

        test("Avvendte kombinert med annen type periode, Status INVALID") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(5),
                        avventendeInnspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                    ),

                    generatePeriode(
                        fom = LocalDate.now().plusDays(6),
                        tom = LocalDate.now().plusDays(10)
                    )
                )
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR to false,
                PeriodLogicRules.FREMDATERT to false,
                PeriodLogicRules.TOTAL_VARIGHET_OVER_ETT_AAR to false,
                PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to false,
                "oppholdMellomPerioder" to false,
                "ikkeDefinertPeriode" to false,
                "tilbakeDatertMerEnn3AAr" to false,
                "fremdatert" to false,
                "varighetOver1AAr" to false,
                "behandslingsDatoEtterMottatDato" to false,
                "avventendeKombinert" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.AVVENTENDE_SYKMELDING_KOMBINERT
        }

        test("Manglende innstill til arbeidsgiver, Status INVALID") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(5),
                        avventendeInnspillTilArbeidsgiver = "      "
                    )
                )
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR to false,
                PeriodLogicRules.FREMDATERT to false,
                PeriodLogicRules.TOTAL_VARIGHET_OVER_ETT_AAR to false,
                PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to false,
                PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to false,
                "oppholdMellomPerioder" to false,
                "ikkeDefinertPeriode" to false,
                "tilbakeDatertMerEnn3AAr" to false,
                "fremdatert" to false,
                "varighetOver1AAr" to false,
                "behandslingsDatoEtterMottatDato" to false,
                "avventendeKombinert" to false,
                "manglendeInnspillArbeidsgiver" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER
        }

        test("Avventende over 16 dager, Status INVALID") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(17),
                        avventendeInnspillTilArbeidsgiver = "Bør gå minst mulig på jobb"
                    )
                )
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR to false,
                PeriodLogicRules.FREMDATERT to false,
                PeriodLogicRules.TOTAL_VARIGHET_OVER_ETT_AAR to false,
                PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to false,
                PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER to false,
                PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to false,
                "oppholdMellomPerioder" to false,
                "ikkeDefinertPeriode" to false,
                "tilbakeDatertMerEnn3AAr" to false,
                "fremdatert" to false,
                "varighetOver1AAr" to false,
                "behandslingsDatoEtterMottatDato" to false,
                "avventendeKombinert" to false,
                "manglendeInnspillArbeidsgiver" to false,
                "avventendeOver16Dager" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.AVVENTENDE_SYKMELDING_OVER_16_DAGER
        }

        test("For mange behandlingsdager pr uke, Status INVALID") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        behandlingsdager = 2
                    )
                )
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR to false,
                PeriodLogicRules.FREMDATERT to false,
                PeriodLogicRules.TOTAL_VARIGHET_OVER_ETT_AAR to false,
                PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to false,
                PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER to false,
                PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER to false,
                PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to false,
                "oppholdMellomPerioder" to false,
                "ikkeDefinertPeriode" to false,
                "tilbakeDatertMerEnn3AAr" to false,
                "fremdatert" to false,
                "varighetOver1AAr" to false,
                "behandslingsDatoEtterMottatDato" to false,
                "avventendeKombinert" to false,
                "manglendeInnspillArbeidsgiver" to false,
                "avventendeOver16Dager" to false,
                "forMangeBehandlingsDagerPrUke" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE
        }

        test("Gradert under 20 prosent, Status INVALID") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        gradert = generateGradert(grad = 19)
                    )
                )
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR to false,
                PeriodLogicRules.FREMDATERT to false,
                PeriodLogicRules.TOTAL_VARIGHET_OVER_ETT_AAR to false,
                PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to false,
                PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER to false,
                PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER to false,
                PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE to false,
                PeriodLogicRules.GRADERT_SYKMELDING_UNDER_20_PROSENT to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to false,
                "oppholdMellomPerioder" to false,
                "ikkeDefinertPeriode" to false,
                "tilbakeDatertMerEnn3AAr" to false,
                "fremdatert" to false,
                "varighetOver1AAr" to false,
                "behandslingsDatoEtterMottatDato" to false,
                "avventendeKombinert" to false,
                "manglendeInnspillArbeidsgiver" to false,
                "avventendeOver16Dager" to false,
                "forMangeBehandlingsDagerPrUke" to false,
                "gradertUnder20Prosent" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.GRADERT_SYKMELDING_UNDER_20_PROSENT
        }
        test("Gradert over 99 prosent, Status INVALID") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        gradert = generateGradert(grad = 100)
                    )
                )
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR to false,
                PeriodLogicRules.FREMDATERT to false,
                PeriodLogicRules.TOTAL_VARIGHET_OVER_ETT_AAR to false,
                PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to false,
                PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER to false,
                PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER to false,
                PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE to false,
                PeriodLogicRules.GRADERT_SYKMELDING_UNDER_20_PROSENT to false,
                PeriodLogicRules.GRADERT_SYKMELDING_OVER_99_PROSENT to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to false,
                "oppholdMellomPerioder" to false,
                "ikkeDefinertPeriode" to false,
                "tilbakeDatertMerEnn3AAr" to false,
                "fremdatert" to false,
                "varighetOver1AAr" to false,
                "behandslingsDatoEtterMottatDato" to false,
                "avventendeKombinert" to false,
                "manglendeInnspillArbeidsgiver" to false,
                "avventendeOver16Dager" to false,
                "forMangeBehandlingsDagerPrUke" to false,
                "gradertUnder20Prosent" to false,
                "gradertOver99Prosent" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.GRADERT_SYKMELDING_OVER_99_PROSENT
        }

        test("Inneholder behandlingsdager, Status MANUAL_PROCESSING") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        behandlingsdager = 1
                    )
                )
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val status = ruleTree.runRules(sykmelding, ruleMetadata)

            status.treeResult.status shouldBeEqualTo Status.MANUAL_PROCESSING
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                PeriodLogicRules.PERIODER_MANGLER to false,
                PeriodLogicRules.FRADATO_ETTER_TILDATO to false,
                PeriodLogicRules.OVERLAPPENDE_PERIODER to false,
                PeriodLogicRules.OPPHOLD_MELLOM_PERIODER to false,
                PeriodLogicRules.IKKE_DEFINERT_PERIODE to false,
                PeriodLogicRules.TILBAKEDATERT_MER_ENN_3_AR to false,
                PeriodLogicRules.FREMDATERT to false,
                PeriodLogicRules.TOTAL_VARIGHET_OVER_ETT_AAR to false,
                PeriodLogicRules.BEHANDLINGSDATO_ETTER_MOTTATTDATO to false,
                PeriodLogicRules.AVVENTENDE_SYKMELDING_KOMBINERT to false,
                PeriodLogicRules.MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER to false,
                PeriodLogicRules.AVVENTENDE_SYKMELDING_OVER_16_DAGER to false,
                PeriodLogicRules.FOR_MANGE_BEHANDLINGSDAGER_PER_UKE to false,
                PeriodLogicRules.GRADERT_SYKMELDING_UNDER_20_PROSENT to false,
                PeriodLogicRules.GRADERT_SYKMELDING_OVER_99_PROSENT to false,
                PeriodLogicRules.SYKMELDING_MED_BEHANDLINGSDAGER to true
            )

            mapOf(
                "periodeMangler" to false,
                "fraDatoEtterTilDato" to false,
                "overlappendePerioder" to false,
                "oppholdMellomPerioder" to false,
                "ikkeDefinertPeriode" to false,
                "tilbakeDatertMerEnn3AAr" to false,
                "fremdatert" to false,
                "varighetOver1AAr" to false,
                "behandslingsDatoEtterMottatDato" to false,
                "avventendeKombinert" to false,
                "manglendeInnspillArbeidsgiver" to false,
                "avventendeOver16Dager" to false,
                "forMangeBehandlingsDagerPrUke" to false,
                "gradertUnder20Prosent" to false,
                "gradertOver99Prosent" to false,
                "inneholderBehandlingsDager" to true
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo RuleHit.SYKMELDING_MED_BEHANDLINGSDAGER
        }
    }
})
