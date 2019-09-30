package no.nav.syfo.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.receive
import io.ktor.client.engine.mock.respond
import io.ktor.client.response.DefaultHttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkClass
import io.mockk.mockkStatic
import java.io.IOException
import kotlin.test.assertFailsWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

private val fnr = "12345678912"
@InternalAPI
@KtorExperimentalAPI
internal class NorskHelsenettClientTest : Spek({

    val accessTokenClient = mockkClass(AccessTokenClient::class)
    val httpClient = mockkClass(HttpClient::class)
    val httpClientCall = mockkClass(HttpClientCall::class)

    mockkStatic("kotlinx.coroutines.DelayKt")
    coEvery { delay(any()) } returns Unit

    beforeEachTest {
        clearMocks(httpClient, httpClientCall)
        coEvery { httpClient.execute(any()) } returns httpClientCall
        coEvery { httpClientCall.response.receive<Behandler>() } returns Behandler(listOf(Godkjenning()))
    }

    coEvery { accessTokenClient.hentAccessToken(any()) } returns "token"

    val norskHelsenettClient = NorskHelsenettClient("url", accessTokenClient, "resource", httpClient)

    describe("Test NorskHelsenettClient") {
        it("Should get behandler") {
            coEvery { httpClientCall.receive(any()) } returns getDefaultResponse(httpClientCall)
            coEvery { httpClientCall.response.receive<Behandler>() } returns Behandler(listOf(Godkjenning()))

            runBlocking {
                val behandler = norskHelsenettClient.finnBehandler(fnr, "123")
                behandler shouldEqual Behandler(listOf(Godkjenning()))
            }
        }
        it("Should receive null when 404") {
            coEvery { httpClientCall.receive(any()) } returns(
                    DefaultHttpResponse(httpClientCall,
                            respond("Not found", HttpStatusCode.NotFound)))
            runBlocking {
                val behandler = norskHelsenettClient.finnBehandler(fnr, "1")
                behandler shouldEqual null
            }
        }
    }

    describe("Test retry") {
        it("Should retry when getting internal server error") {
            coEvery { httpClientCall.receive(any()) } returns(
                    DefaultHttpResponse(httpClientCall, respond("Internal Server Error",
                            HttpStatusCode.InternalServerError))) andThen getDefaultResponse(httpClientCall)

            runBlocking {
                val behandler = norskHelsenettClient.finnBehandler(fnr, "1")
                behandler shouldEqual Behandler(listOf(Godkjenning()))
                coVerify(exactly = 2) { httpClient.execute(any()) }
            }
        }

        it("Should throw exeption when exceeds max retries") {
            coEvery { httpClientCall.receive(any()) } returns(
                    DefaultHttpResponse(httpClientCall, respond("InternalServerErrror", HttpStatusCode.InternalServerError)))
            runBlocking {
                assertFailsWith<IOException> { norskHelsenettClient.finnBehandler(fnr, "1") }
                coVerify(exactly = 3) { httpClient.execute(any()) }
            }
        }
    }
})

@InternalAPI
private fun getDefaultResponse(httpClientCall: HttpClientCall) =
        DefaultHttpResponse(
                httpClientCall, respond(
                jacksonObjectMapper().writeValueAsString(
                        Behandler(
                                listOf(Godkjenning())
                        )
                ), HttpStatusCode.OK)
        )
