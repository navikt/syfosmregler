package no.nav.syfo.rules.legesuspensjon

import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.validation.ruleMetadataSykmelding
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate
import java.util.UUID

class LegeSuspensjonTest : FunSpec({
    val ruleTree = LegeSuspensjonRulesExecution()
    val ruleMetadata = RuleMetadata(
        signatureDate = LocalDate.now().atStartOfDay(),
        receivedDate = LocalDate.now().atStartOfDay(),
        behandletTidspunkt = LocalDate.now().atStartOfDay(),
        patientPersonNumber = "12345678901",
        rulesetVersion = null,
        legekontorOrgnr = null,
        tssid = null,
        avsenderFnr = "2",
        pasientFodselsdato = LocalDate.now().minusYears(31),
    )
    val sykmeldingRuleMetadata = ruleMetadataSykmelding(ruleMetadata)
    val sykmelding = mockk<Sykmelding>(relaxed = true)
    every { sykmelding.id } returns UUID.randomUUID().toString()
    context("Testing legesuspensjon rules and checking the rule outcomes") {
        test("Er ikkje suspendert, Status OK") {
            val status = ruleTree.runRules(sykmelding, sykmeldingRuleMetadata).first
            status.treeResult.status shouldBeEqualTo Status.OK
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                LegeSuspensjonRules.BEHANDLER_SUSPENDERT to false,
            )

            mapOf(
                "suspendert" to false,
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo null
        }

        test("Er suspendert, Status INVALID") {
            val status = ruleTree.runRules(sykmelding, sykmeldingRuleMetadata.copy(doctorSuspensjon = true)).first

            status.treeResult.status shouldBeEqualTo Status.INVALID
            status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(
                LegeSuspensjonRules.BEHANDLER_SUSPENDERT to true,
            )
            mapOf(
                "suspendert" to true,
            ) shouldBeEqualTo status.ruleInputs

            status.treeResult.ruleHit shouldBeEqualTo LegeSuspensjonRuleHit.BEHANDLER_SUSPENDERT.ruleHit
        }
    }
})
