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
import no.nav.syfo.sykmeldingRespons
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate

class RuleServiceTest : FunSpec({
    val legeSuspensjonsClient = mockk<LegeSuspensjonClient>()
    val syketilfelleClient = mockk<SyketilfelleClient>(relaxed = true)
    val norskHelsenettClient = mockk<NorskHelsenettClient>()
    val smregisterClient = mockk<SmregisterClient>()
    val pdlService = mockk<PdlPersonService>()
    val juridiskVurderingService = mockk<JuridiskVurderingService>(relaxed = true)
    val sykmeldingService = SykmeldingService(smregisterClient)
    val ruleService = RuleService(
        legeSuspensjonClient = legeSuspensjonsClient,
        syketilfelleClient = syketilfelleClient,
        norskHelsenettClient = norskHelsenettClient,
        sykmeldingService = sykmeldingService,
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
        coEvery { smregisterClient.hentSykmeldinger(any()) } returns emptyList()
        coEvery { pdlService.getPdlPerson(any(), any()) } returns PdlPerson(
            identer = listOf(IdentInformasjon("1", false, FOLKEREGISTERIDENT)),
            foedsel = listOf(Foedsel(LocalDate.of(2000, 1, 1).toString()))
        )
    }
    context("Tilbakedaterte sykmeldinger") {
        test("Test OK") {
            val sykmelding = generateReceivedSykmelding(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 1, 2),
                kontaktMedPasient = KontaktMedPasient(
                    kontaktDato = LocalDate.of(2020, 1, 1),
                    begrunnelseIkkeKontakt = null
                )
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

        test("Over 31 dager skal avvises") {
            val sykmelding = generateReceivedSykmelding(
                fom = LocalDate.of(2020, 1, 1),
                tom = LocalDate.of(2020, 1, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 1).atStartOfDay().plusDays(41),
                kontaktMedPasient = KontaktMedPasient(
                    kontaktDato = LocalDate.of(2020, 1, 9),
                    begrunnelseIkkeKontakt = ""
                )
            )
            val rules =
                ruleService.executeRuleChains(sykmelding, sykmelding.sykmelding.behandletTidspunkt.toLocalDate())
            rules.ruleHits.size shouldBeEqualTo 1
            rules.ruleHits.first().ruleName shouldBeEqualTo "TILBAKEDATERT_FORLENGELSE_OVER_1_MND"
            rules.status shouldBeEqualTo Status.INVALID
        }
    }

    context("Test tilbakedaterte symeldinger som dekker et hull") {
        context("case 1:") {
            val forsteSykmelding = sykmeldingRespons(
                fom = LocalDate.of(2020, 9, 1),
                tom = LocalDate.of(2020, 9, 14)
            )
            val andreSykmelding = sykmeldingRespons(
                fom = LocalDate.of(2020, 9, 18),
                tom = LocalDate.of(2020, 9, 30)
            )

            val tilbakedatertSykmelding = generateReceivedSykmelding(
                fom = LocalDate.of(2020, 9, 15),
                tom = LocalDate.of(2020, 9, 17),
                behandletTidspunkt = LocalDate.of(2020, 10, 12).atStartOfDay(),
                kontaktMedPasient = KontaktMedPasient(LocalDate.of(2020, 10, 17), "")
            )
            coEvery { smregisterClient.hentSykmeldinger(any()) } returns forsteSykmelding + andreSykmelding

            test("< 31 dager tilbakedatert, < 31 dager hull") {

                val rules = ruleService.executeRuleChains(tilbakedatertSykmelding, LocalDate.of(2020, 10, 15))

                rules.ruleHits shouldBeEqualTo emptyList()
                rules.status shouldBeEqualTo Status.OK
            }

            test(">= 31 dager tilbakedaterst, < 31 dager hull") {
                val sykmelding = tilbakedatertSykmelding.copy(
                    sykmelding = tilbakedatertSykmelding.sykmelding.copy(
                        behandletTidspunkt = LocalDate.of(
                            2020,
                            10,
                            20
                        ).atStartOfDay()
                    )
                )
                val rules = ruleService.executeRuleChains(sykmelding, sykmelding.sykmelding.behandletTidspunkt.toLocalDate())
                rules.ruleHits.size shouldBeEqualTo 1
                rules.ruleHits.first().ruleName shouldBeEqualTo "TILBAKEDATERT_FORLENGELSE_OVER_1_MND"
                rules.status shouldBeEqualTo Status.MANUAL_PROCESSING
            }
        }
    }
})
