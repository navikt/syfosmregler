package no.nav.syfo.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.receive
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.client.statement.DefaultHttpResponse
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.response
import io.ktor.http.Headers
import io.ktor.http.HttpMessageBuilder
import io.ktor.http.HttpProtocolVersion.Companion.HTTP_2_0
import io.ktor.http.HttpStatusCode
import io.ktor.routing.get
import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.date.GMTDate
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockkClass
import io.mockk.mockkStatic
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.syfo.LoggingMeta
import org.amshove.kluent.any
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.coroutines.CoroutineContext

private val fnr = "12345678912"
@InternalAPI
@KtorExperimentalAPI
internal class NorskHelsenettClientTest : Spek({

    val accessTokenClient = mockkClass(AccessTokenClient::class)
    val httpClient = mockkClass(HttpClient::class)
    val httpClientCall = mockkClass(HttpClientCall::class)
    val httpResponse = mockkClass(HttpResponse::class)

    val httpStatement = mockkClass(HttpStatement::class)
    val httpClientCallResponse = mockkClass(HttpResponse::class)
    val coroutineContext = mockkClass(CoroutineContext::class)

    mockkStatic("kotlinx.coroutines.DelayKt")
    coEvery { delay(any()) } returns Unit
    mockkStatic("io.ktor.http.URLParserKt")
    mockkStatic("io.ktor.client.request.HttpRequestKt")

    beforeEachTest {
        clearMocks(httpClient, httpResponse)
        coEvery { httpClient.request<Any>(any(String::class), {})} returns Behandler(listOf(Godkjenning()))
        coEvery { httpClient.get<HttpStatement>(any(String::class), {})} returns httpStatement
        coEvery { httpStatement.execute() } returns httpResponse
        coEvery { httpResponse.call } returns httpClientCall
        coEvery { httpClientCall.response } returns httpClientCallResponse
        coEvery { httpClient.coroutineContext } returns coroutineContext
    }

    coEvery { accessTokenClient.hentAccessToken(any()) } returns "token"

    val norskHelsenettClient = NorskHelsenettClient("url", accessTokenClient, "resource", httpClient)

    describe("Test NorskHelsenettClient") {
        it("Should get behandler") {
            coEvery { httpClientCall.receive(any()) } returns getDefaultResponse(httpClientCall,coroutineContext )
            coEvery { httpClientCallResponse.receive<Behandler>() } returns Behandler(listOf(Godkjenning()))

            runBlocking {
                val behandler = norskHelsenettClient.finnBehandler(fnr, "123",
                        LoggingMeta("", "", "123", ""))
                behandler shouldEqual Behandler(listOf(Godkjenning()))
            }
        }
    }
        /*
        it("Should receive null when 404") {
            coEvery { httpClientCall.receive(any()) } returns(
            DefaultHttpResponse(httpClientCall,
                    HttpResponseData(
                            HttpStatusCode.NotFound,
                            GMTDate(ZonedDateTime.now().toInstant().toEpochMilli()),
                            Headers.Empty,
                            HTTP_2_0,
                            any(),
                            httpClientCall.coroutineContext
                        )
                    )
             )
            runBlocking {
                val behandler = norskHelsenettClient.finnBehandler(fnr, "1",
                        LoggingMeta("", "", "1", ""))
                behandler shouldEqual null
            }
        }
    }
         */

    /*
    describe("Test retry") {
        it("Should retry when getting internal server error") {
            coEvery { httpClientCall.receive(any()) } returns(
                    DefaultHttpResponse(httpResponse, respond("Internal Server Error",
                            HttpStatusCode.InternalServerError))) andThen getDefaultResponse(httpResponse)

            runBlocking {
                val behandler = norskHelsenettClient.finnBehandler(fnr, "1",
                        LoggingMeta("", "", "1", ""))
                behandler shouldEqual Behandler(listOf(Godkjenning()))
                coVerify(exactly = 2) { httpClient.execute(any()) }
            }
        }

        it("Should throw exeption when exceeds max retries") {
            coEvery { httpClientCall.receive(any()) } returns(
                    DefaultHttpResponse(httpClientCall,
                            HttpResponseData(
                                    HttpStatusCode.InternalServerError,
                                    GMTDate( ZonedDateTime.now().toInstant().toEpochMilli()),
                                    Headers.Empty,
                                    HTTP_2_0,
                                    any(),
                                    httpClientCall.coroutineContext

                            )))
            runBlocking {
                assertFailsWith<IOException> { norskHelsenettClient.finnBehandler(fnr, "1",
                        LoggingMeta("", "", "123", "")) }
                coVerify(exactly = 4) { httpClient.execute(any()).ex }
            }
        }
    }
     */
})

@InternalAPI
private fun getDefaultResponse(httpClientCall: HttpClientCall, coroutineContext: CoroutineContext) =
        DefaultHttpResponse(httpClientCall,
                HttpResponseData(
                        HttpStatusCode.OK,
                        GMTDate(ZonedDateTime.now().toInstant().toEpochMilli()),
                        Headers.Empty,
                        HTTP_2_0,
                        jacksonObjectMapper().writeValueAsString(
                                Behandler(
                                        listOf(Godkjenning())
                                )
                        ),
                        coroutineContext

                ))
