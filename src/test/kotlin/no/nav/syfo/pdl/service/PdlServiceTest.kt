package no.nav.syfo.pdl.service

import io.mockk.coEvery
import io.mockk.mockkClass
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.AccessTokenClientV2
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.client.model.GraphQLResponse
import no.nav.syfo.pdl.client.model.HentPerson
import no.nav.syfo.pdl.client.model.IdentInformasjon
import no.nav.syfo.pdl.client.model.Identliste
import no.nav.syfo.pdl.client.model.PdlResponse
import no.nav.syfo.pdl.error.PersonNotFoundInPdl
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import kotlin.test.assertFailsWith

internal class PdlServiceTest {

    private val pdlClient = mockkClass(PdlClient::class)
    private val accessTokenClientV2 = mockkClass(AccessTokenClientV2::class)
    private val pdlService = PdlPersonService(pdlClient, accessTokenClientV2, "scope")

    @Test
    internal fun `Skal feile når person ikke finnes`() {
        coEvery { accessTokenClientV2.getAccessTokenV2(any()) } returns "token"

        coEvery { pdlClient.getPerson(any(), any()) } returns GraphQLResponse<PdlResponse>(
            PdlResponse(null, null),
            errors = null
        )

        val exception = assertFailsWith<PersonNotFoundInPdl> {
            runBlocking {
                pdlService.getPdlPerson("123", "callId")
            }
        }
        exception.message shouldBeEqualTo "Klarte ikke hente ut person fra PDL"
    }

    @Test
    internal fun `Skal feile når fodesel er tom liste`() {
        coEvery { accessTokenClientV2.getAccessTokenV2(any()) } returns "token"

        coEvery { pdlClient.getPerson(any(), any()) } returns GraphQLResponse<PdlResponse>(
            PdlResponse(
                hentPerson = HentPerson(
                    foedsel = emptyList()
                ),
                hentIdenter = Identliste(emptyList())
            ),
            errors = null
        )
        val exception = assertFailsWith<PersonNotFoundInPdl> {
            runBlocking {
                pdlService.getPdlPerson("123", "callId")
            }
        }
        exception.message shouldBeEqualTo "Fant ikke navn på person i PDL"
    }

    @Test
    internal fun `Skal feile når fodesel ikke finnes`() {
        coEvery { accessTokenClientV2.getAccessTokenV2(any()) } returns "token"

        coEvery { pdlClient.getPerson(any(), any()) } returns GraphQLResponse<PdlResponse>(
            PdlResponse(
                hentPerson = HentPerson(
                    foedsel = null
                ),
                hentIdenter = Identliste(
                    listOf(
                        IdentInformasjon(
                            ident = "987654321",
                            gruppe = "foo",
                            historisk = false
                        )
                    )
                )
            ),
            errors = null
        )
        val exception = assertFailsWith<PersonNotFoundInPdl> {
            runBlocking {
                pdlService.getPdlPerson("123", "callId")
            }
        }
        exception.message shouldBeEqualTo "Fant ikke navn på person i PDL"
    }
}
