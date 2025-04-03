package no.nav.syfo.rules.pdl

import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockkClass
import java.time.OffsetDateTime
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.azuread.v2.AzureAdV2Token
import no.nav.syfo.rules.pdl.client.PdlClient
import no.nav.syfo.rules.pdl.client.model.Foedsel
import no.nav.syfo.rules.pdl.client.model.GraphQLResponse
import no.nav.syfo.rules.pdl.client.model.HentPerson
import no.nav.syfo.rules.pdl.client.model.IdentInformasjon
import no.nav.syfo.rules.pdl.client.model.Identliste
import no.nav.syfo.rules.pdl.client.model.PdlResponse
import no.nav.syfo.rules.pdl.error.PersonNotFoundInPdl
import no.nav.syfo.utils.LoggingMeta
import org.amshove.kluent.shouldBeEqualTo

object PdlServiceTest :
    FunSpec({
        val pdlClient = mockkClass(PdlClient::class)
        val accessTokenClientMock = mockkClass(AzureAdV2Client::class)
        val pdlService = PdlPersonService(pdlClient, accessTokenClientMock, "scope")

        val loggingMeta = LoggingMeta("mottakId", "orgNr", "msgId", "sykmeldingId")

        context("PdlServiceTest") {
            test("hente person fra pdl") {
                coEvery { accessTokenClientMock.getAccessToken(any()) } returns
                    AzureAdV2Token("accessToken", OffsetDateTime.now().plusHours(1))
                coEvery { pdlClient.getPerson(any(), any()) } returns
                    GraphQLResponse(
                        PdlResponse(
                            hentPerson = HentPerson(listOf(Foedsel("1900-01-01"))),
                            hentIdenter =
                                Identliste(
                                    listOf(
                                        IdentInformasjon(
                                            ident = "01245678901",
                                            gruppe = "FOLKEREGISTERIDENT",
                                            historisk = false
                                        )
                                    )
                                ),
                        ),
                        errors = null,
                    )

                val person = pdlService.getPdlPerson("01245678901", loggingMeta)
                person.fnr shouldBeEqualTo "01245678901"
                person.foedselsdato?.firstOrNull()?.foedselsdato shouldBeEqualTo "1900-01-01"
            }

            test("Skal feile når person ikke finnes") {
                coEvery { accessTokenClientMock.getAccessToken(any()) } returns
                    AzureAdV2Token("accessToken", OffsetDateTime.now().plusHours(1))
                coEvery { pdlClient.getPerson(any(), any()) } returns
                    GraphQLResponse<PdlResponse>(
                        PdlResponse(null, null),
                        errors = null,
                    )

                val exception =
                    assertFailsWith<PersonNotFoundInPdl> {
                        runBlocking { pdlService.getPdlPerson("123", loggingMeta) }
                    }
                exception.message shouldBeEqualTo "Klarte ikke hente ut person fra PDL"
            }

            test("Skal feile når ident er tom liste") {
                coEvery { accessTokenClientMock.getAccessToken(any()) } returns
                    AzureAdV2Token("accessToken", OffsetDateTime.now().plusHours(1))
                coEvery { pdlClient.getPerson(any(), any()) } returns
                    GraphQLResponse<PdlResponse>(
                        PdlResponse(
                            hentPerson =
                                HentPerson(
                                    foedselsdato = emptyList(),
                                ),
                            hentIdenter = Identliste(emptyList()),
                        ),
                        errors = null,
                    )
                val exception =
                    assertFailsWith<PersonNotFoundInPdl> {
                        runBlocking { pdlService.getPdlPerson("123", loggingMeta) }
                    }
                exception.message shouldBeEqualTo "Fant ikke ident i PDL"
            }
        }
    })
