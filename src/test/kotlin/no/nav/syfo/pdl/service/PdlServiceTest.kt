package no.nav.syfo.pdl.service

import io.mockk.coEvery
import io.mockk.mockkClass
import kotlinx.coroutines.runBlocking
import no.nav.syfo.LoggingMeta
import no.nav.syfo.client.AccessTokenClientV2
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.client.model.Foedsel
import no.nav.syfo.pdl.client.model.GraphQLResponse
import no.nav.syfo.pdl.client.model.HentPerson
import no.nav.syfo.pdl.client.model.IdentInformasjon
import no.nav.syfo.pdl.client.model.Identliste
import no.nav.syfo.pdl.client.model.PdlResponse
import no.nav.syfo.pdl.error.PersonNotFoundInPdl
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertFailsWith

class PdlServiceTest : Spek({
    val pdlClient = mockkClass(PdlClient::class)
    val accessTokenClientV2 = mockkClass(AccessTokenClientV2::class)
    val pdlService = PdlPersonService(pdlClient, accessTokenClientV2, "scope")

    val loggingMeta = LoggingMeta("mottakId", "orgNr", "msgId", "sykmeldingId")

    describe("PdlServiceTest") {

        it("hente person fra pdl") {
            coEvery { accessTokenClientV2.getAccessTokenV2(any()) } returns "token"
            coEvery { pdlClient.getPerson(any(), any()) } returns GraphQLResponse(
                PdlResponse(
                    hentPerson = HentPerson(listOf(Foedsel("1900", "1900-01-01"))),
                    hentIdenter = Identliste(listOf(IdentInformasjon(ident = "01245678901", gruppe = "FOLKEREGISTERIDENT", historisk = false)))
                ),
                errors = null
            )

            runBlocking {
                val person = pdlService.getPdlPerson("01245678901", loggingMeta)
                person.fnr shouldBeEqualTo "01245678901"
                person.foedsel?.firstOrNull()?.foedselsaar shouldBeEqualTo "1900"
            }
        }

        it("Skal feile når person ikke finnes") {
            coEvery { accessTokenClientV2.getAccessTokenV2(any()) } returns "token"

            coEvery { pdlClient.getPerson(any(), any()) } returns GraphQLResponse<PdlResponse>(
                PdlResponse(null, null),
                errors = null
            )

            val exception = assertFailsWith<PersonNotFoundInPdl> {
                runBlocking {
                    pdlService.getPdlPerson("123", loggingMeta)
                }
            }
            exception.message shouldBeEqualTo "Klarte ikke hente ut person fra PDL"
        }

        it("Skal feile når ident er tom liste") {
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
                    pdlService.getPdlPerson("123", loggingMeta)
                }
            }
            exception.message shouldBeEqualTo "Fant ikke ident i PDL"
        }
    }
})
