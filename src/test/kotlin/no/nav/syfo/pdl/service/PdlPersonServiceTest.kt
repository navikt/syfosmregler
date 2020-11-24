package no.nav.syfo.pdl.service

import io.ktor.util.KtorExperimentalAPI
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import no.nav.syfo.LoggingMeta
import no.nav.syfo.client.OidcToken
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.client.model.Adressebeskyttelse
import no.nav.syfo.pdl.client.model.GetPersonResponse
import no.nav.syfo.pdl.client.model.HentPerson
import no.nav.syfo.pdl.client.model.ResponseData
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@KtorExperimentalAPI
object PdlPersonServiceTest : Spek({
    val pdlClient = mockk<PdlClient>()
    val stsOidcClient = mockk<StsOidcClient>()
    val pdlService = PdlPersonService(pdlClient, stsOidcClient)
    val loggingMeta = LoggingMeta("mottakId", "orgNr", "msgId", "sykmeldingId")

    beforeEachTest {
        clearAllMocks()
        coEvery { stsOidcClient.oidcToken() } returns OidcToken("Token", "JWT", 1L)
    }

    describe("Hent diskresjonskode fra PDL") {
        it("Hent diskresjonskode fra pdl uten fortrolig adresse") {
            coEvery { pdlClient.getPerson(any(), any()) } returns GetPersonResponse(ResponseData(hentPerson = HentPerson(adressebeskyttelse = null)), errors = null)

            runBlocking {
                val diskresjonskode = pdlService.hentDiskresjonskode("01245678901", loggingMeta)
                diskresjonskode shouldEqual ""
            }
        }
        it("Hent diskresjonskode fra pdl med strengt fortrolig adresse") {
            coEvery { pdlClient.getPerson(any(), any()) } returns GetPersonResponse(ResponseData(hentPerson = HentPerson(adressebeskyttelse = listOf(Adressebeskyttelse("STRENGT_FORTROLIG")))), errors = null)

            runBlocking {
                val diskresjonskode = pdlService.hentDiskresjonskode("01245678901", loggingMeta)
                diskresjonskode shouldEqual "6"
            }
        }
        it("Skal feile når person ikke finnes") {
            coEvery { pdlClient.getPerson(any(), any()) } returns GetPersonResponse(ResponseData(null), errors = null)

            assertFailsWith<RuntimeException> {
                runBlocking {
                    pdlService.hentDiskresjonskode("123", loggingMeta)
                }
            }
        }
    }
})

fun getPdlResponse(): GetPersonResponse {
    return GetPersonResponse(ResponseData(
        hentPerson = HentPerson(adressebeskyttelse = null)
    ), errors = null)
}
