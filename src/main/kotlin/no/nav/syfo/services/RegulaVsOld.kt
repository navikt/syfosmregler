package no.nav.syfo.services

import no.nav.syfo.model.ValidationResult
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.printRulePath
import no.nav.tsm.regulus.regula.RegulaResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("regula-shadow-test")

fun compareNewVsOld(
    sykmeldingId: String,
    newResult: RegulaResult,
    oldResult: List<TreeOutput<out Enum<*>, RuleResult>>,
    oldValidationResult: ValidationResult,
) {
    val newVsOld: List<Pair<String, String>> =
        oldResult
            .map { it.printRulePath() }
            .zip(
                newResult.results.map {
                    it.rulePath
                        .replace("PAPIRSYKMELDING(no)->", "")
                        .replace("BEHANDLER_FINNES_I_HPR(yes)->", "")
                },
            )

    val allPathsEqual = newVsOld.all { (old, new) -> old == new }

    if (allPathsEqual) {
        log.info(
            """ ✅ REGULA SHADOW TEST Result: OK
                | SykmeldingID: ${sykmeldingId}
                | Outcome: ${newResult.status.name} (${oldValidationResult.status.name})
                | Chains executed: ${oldResult.size} / ${newResult.results.size}
            """
                .trimMargin(),
        )
    } else {
        log.warn(
            """ ❌ REGULA SHADOW TEST Result: DIVERGENCE DETECTED
                    | SykmeldingID: ${sykmeldingId}
                    | Outcome: ${newResult.status.name} (${oldValidationResult.status.name})
                    | Chains executed: ${oldResult.size} / ${newResult.results.size}
                    | Diverging paths count: ${newVsOld.count { (old, new) -> old != new }} 
                    | 
                    | Diverging paths:
                    |${
                newVsOld.filter { (old, new) -> old != new }.joinToString("\n") { (old, new) ->
                    "Old: $old\nNew: $new"
                }
            }
                    """
                .trimMargin(),
        )
    }
}
