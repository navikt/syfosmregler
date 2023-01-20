package no.nav.syfo.rules.periodlogic

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Status
import no.nav.syfo.rules.tilbakedatering.toRuleMetadata
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate

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
    }
})
