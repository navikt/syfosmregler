package no.nav.syfo.services

import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.client.Behandler
import no.nav.syfo.client.Godkjenning
import no.nav.syfo.client.Kode
import no.nav.syfo.client.LegeSuspensjonClient
import no.nav.syfo.client.NorskHelsenettClient
import no.nav.syfo.client.SmregisterClient
import no.nav.syfo.client.Suspendert
import no.nav.syfo.client.SyketilfelleClient
import no.nav.syfo.generateReceivedSykmelding
import no.nav.syfo.model.KontaktMedPasient
import no.nav.syfo.model.Status
import no.nav.syfo.pdl.client.model.Foedsel
import no.nav.syfo.pdl.client.model.IdentInformasjon
import no.nav.syfo.pdl.model.FOLKEREGISTERIDENT
import no.nav.syfo.pdl.model.PdlPerson
import no.nav.syfo.pdl.service.PdlPersonService
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate

class RuleServiceTest : FunSpec({
    val legeSuspensjonsClient = mockk<LegeSuspensjonClient>()
    val syketilfelleClient = mockk<SyketilfelleClient>(relaxed = true)
    val norskHelsenettClient = mockk<NorskHelsenettClient>()
    val smregisterClient = mockk<SmregisterClient>()
    val pdlService = mockk<PdlPersonService>()
    val juridiskVurderingService = mockk<JuridiskVurderingService>(relaxed = true)

    val ruleService = RuleService(
        legeSuspensjonClient = legeSuspensjonsClient,
        syketilfelleClient = syketilfelleClient,
        norskHelsenettClient = norskHelsenettClient,
        smregisterClient = smregisterClient,
        pdlService = pdlService,
        juridiskVurderingService = juridiskVurderingService
    )

    beforeEach {

        coEvery { legeSuspensjonsClient.checkTherapist(any(), any(), any(), any()) } returns Suspendert(false)
        coEvery { norskHelsenettClient.finnBehandler(any(), any(), any()) } returns Behandler(
            godkjenninger = listOf(
                Godkjenning(
                    autorisasjon = Kode(true, 7704, "1"),
                    helsepersonellkategori = Kode(aktiv = true, oid = 0, verdi = "LE")
                )
            ),
            1
        )
        coEvery { smregisterClient.finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert(any(), any(), any(), any()) } returns false
        coEvery { pdlService.getPdlPerson(any(), any()) } returns PdlPerson(
            identer = listOf(IdentInformasjon("1", false, FOLKEREGISTERIDENT)),
            foedsel = listOf(Foedsel(LocalDate.of(2000, 1, 1).toString()))
        )
    }
    context("Test rulechain") {
        test("Test OK") {
            val sykmelding = generateReceivedSykmelding(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 1, 2),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = LocalDate.of(2020, 1, 1), begrunnelseIkkeKontakt = null)
            )
            val rules = ruleService.executeRuleChains(sykmelding, currentDate = LocalDate.of(2020, 1, 1))
            rules.ruleHits shouldBeEqualTo emptyList()
            rules.status shouldBeEqualTo Status.OK
        }

        test("Test 30 dager tilbakedatert OK") {
            val sykmelding = generateReceivedSykmelding(
                fom = LocalDate.of(2020, 1, 1),
                tom = LocalDate.of(2020, 1, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 1).atStartOfDay().plusDays(30),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "kan ikke")
            )
            val rules = ruleService.executeRuleChains(sykmelding, LocalDate.of(2020, 1, 1))
            rules.ruleHits shouldBeEqualTo emptyList()
            rules.status shouldBeEqualTo Status.OK
        }

        test("Test 30 dager uten KontaktMedPasient skal bli avvist") {
            val sykmelding = generateReceivedSykmelding(
                fom = LocalDate.of(2020, 1, 1),
                tom = LocalDate.of(2020, 1, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 1).atStartOfDay().plusDays(30),
            )
            val rules = ruleService.executeRuleChains(sykmelding, LocalDate.of(2020, 1, 1))
            rules.ruleHits.size shouldBeEqualTo 1
            rules.ruleHits.first().ruleName shouldBeEqualTo "TILBAKEDATERT_FORLENGELSE_UNDER_1_MND"
            rules.status shouldBeEqualTo Status.INVALID
        }

        test("Over 31 dager skal til manuell") {
            val sykmelding = generateReceivedSykmelding(
                fom = LocalDate.of(2020, 1, 1),
                tom = LocalDate.of(2020, 1, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 1).atStartOfDay().plusDays(41),
                kontaktMedPasient = KontaktMedPasient(
                    kontaktDato = LocalDate.of(2020, 1, 9),
                    begrunnelseIkkeKontakt = ""
                )
            )
            val rules = ruleService.executeRuleChains(sykmelding, LocalDate.of(2020, 1, 1))
            rules.ruleHits.size shouldBeEqualTo 1
            rules.ruleHits.first().ruleName shouldBeEqualTo "TILBAKEDATERT_FORLENGELSE_OVER_1_MND"
            rules.status shouldBeEqualTo Status.MANUAL_PROCESSING
        }
    }
})
