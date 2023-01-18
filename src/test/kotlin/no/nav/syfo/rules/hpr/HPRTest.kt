package no.nav.syfo.rules.hpr

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.client.Behandler
import no.nav.syfo.client.Godkjenning
import no.nav.syfo.client.Kode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Status
import no.nav.syfo.rules.BehandlerOgStartdato
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate

class HPRTest : FunSpec({
    val ruleTree = HPRRulesExecution()

    context("Test hpr regler har aktiv autorisasjon") {
        test("har aktiv autorisasjon, Status OK") {
            val sykmelding = generateSykmelding(
                fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay()
            )
            val behandler = Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon = Kode(
                            aktiv = true,
                            oid = 7702,
                            verdi = "1"
                        )
                    )
                )
            )

            val behandlerOgStartdato = BehandlerOgStartdato(behandler, null)

            val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger

            val aktivAutorisasjon = behandlerGodkjenninger.any {
                it.autorisasjon?.aktiv != null && it.autorisasjon!!.aktiv
            }

            val status = ruleTree.runRules(sykmelding, behandlerOgStartdato)

            status.treeResult.status shouldBeEqualTo Status.OK
            // status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR to false)
            // mapOf(
            //    "aktivAutorisasjon" to aktivAutorisasjon
            // ) shouldBeEqualTo status.ruleInputs
            // status.treeResult.ruleHit shouldBeEqualTo null
        }

        test("har ikke aktiv autorisasjon, Status INVALID") {
            val sykmelding = generateSykmelding(
                fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay()
            )
            val behandler = Behandler(
                listOf(
                    Godkjenning(
                        autorisasjon = Kode(
                            aktiv = false,
                            oid = 7702,
                            verdi = "1"
                        )
                    )
                )
            )

            val behandlerOgStartdato = BehandlerOgStartdato(behandler, null)

            val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger

            val aktivAutorisasjon = behandlerGodkjenninger.any {
                it.autorisasjon?.aktiv != null && it.autorisasjon!!.aktiv
            }

            val status = ruleTree.runRules(sykmelding, behandlerOgStartdato)

            status.treeResult.status shouldBeEqualTo Status.INVALID
            // status.rulePath.map { it.rule to it.ruleResult } shouldBeEqualTo listOf(HPRRules.BEHANDLER_IKKE_GYLDIG_I_HPR to false)
            // mapOf(
            //    "aktivAutorisasjon" to aktivAutorisasjon
            // ) shouldBeEqualTo status.ruleInputs
            status.treeResult.ruleHit shouldBeEqualTo RuleHit.BEHANDLER_IKKE_GYLDIG_I_HPR
        }
    }
})
