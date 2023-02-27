package no.nav.syfo.rules.gradert
import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.client.Behandler
import no.nav.syfo.generateGradert
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Status
import no.nav.syfo.rules.tilbakedatering.toRuleMetadata
import no.nav.syfo.services.BehandlerOgStartdato
import no.nav.syfo.services.RuleMetadataSykmelding
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate

class GradertTest : FunSpec({

    val ruleTree = GradertRulesExecution()

    context("Testing gradert rules and checking the rule outcomes") {
        test("Sick leave is 21 procent, should be OK") {
            val sykmelding = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        gradert = generateGradert(grad = 21)
                    )
                )
            )

            val ruleMetadata = sykmelding.toRuleMetadata()

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = ruleMetadata,
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false,
                doctorSuspensjon = false,
                behandlerOgStartdato = BehandlerOgStartdato(Behandler(emptyList(), null), null)
            )

            val result = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

            result.first.treeResult.status shouldBeEqualTo Status.OK
        }

        test("Sick leave is 19 procent, should be INVALID") {
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

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = ruleMetadata,
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false,
                doctorSuspensjon = false,
                behandlerOgStartdato = BehandlerOgStartdato(Behandler(emptyList(), null), null)
            )

            val result = ruleTree.runRules(sykmelding, ruleMetadataSykmelding)

            result.first.treeResult.status shouldBeEqualTo Status.INVALID
        }
    }
})
