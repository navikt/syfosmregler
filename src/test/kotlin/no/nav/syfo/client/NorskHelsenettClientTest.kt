package no.nav.syfo.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FunSpec
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.azuread.v2.AzureAdV2Token
import no.nav.syfo.utils.LoggingMeta
import org.amshove.kluent.shouldBeEqualTo
import java.io.IOException
import java.net.ServerSocket
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith

object NorskHelsenettClientTest : FunSpec({
    val fnr = "12345678912"
    val accessTokenClientMock = mockk<AzureAdV2Client>()
    val httpClient = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    }

    val mockHttpServerPort = ServerSocket(0).use { it.localPort }
    val mockHttpServerUrl = "http://localhost:$mockHttpServerPort"
    val mockServer = embeddedServer(Netty, mockHttpServerPort) {
        install(ContentNegotiation) {
            jackson {}
        }
        routing {
            get("/api/v2/behandler") {
                when {
                    call.request.headers["behandlerFnr"] == fnr -> call.respond(Behandler(listOf(Godkjenning(helsepersonellkategori = Kode(true, 1, "verdi"), autorisasjon = Kode(true, 2, "annenVerdi")))))
                    call.request.headers["behandlerFnr"] == "behandlerFinnesIkke" -> call.respond(HttpStatusCode.NotFound, "Behandler finnes ikke")
                    else -> call.respond(HttpStatusCode.InternalServerError, "Noe gikk galt")
                }
            }
        }
    }.start()

    val norskHelsenettClient = NorskHelsenettClient(mockHttpServerUrl, accessTokenClientMock, "resourceId", httpClient)

    afterSpec {
        mockServer.stop(TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(1))
    }

    beforeSpec {
        coEvery { accessTokenClientMock.getAccessToken(any()) } returns AzureAdV2Token("accessToken", OffsetDateTime.now().plusHours(1))
    }

    context("Test NorskHelsenettClient") {
        test("Should get behandler") {
            val behandler = norskHelsenettClient.finnBehandler(
                fnr,
                "123",
                LoggingMeta("", "", "123", "")
            )

            behandler shouldBeEqualTo Behandler(
                listOf(
                    Godkjenning(
                        helsepersonellkategori = Kode(true, 1, "verdi"),
                        autorisasjon = Kode(true, 2, "annenVerdi")
                    )
                )
            )
        }

        test("Should receive null when 404") {
            val behandler = norskHelsenettClient.finnBehandler(
                "behandlerFinnesIkke",
                "1",
                LoggingMeta("", "", "1", "")
            )

            behandler shouldBeEqualTo null
        }
    }

    context("Test retry") {
        test("Should retry when getting internal server error") {
            val behandler = norskHelsenettClient.finnBehandler(
                fnr,
                "1",
                LoggingMeta("", "", "1", "")
            )

            behandler shouldBeEqualTo Behandler(
                listOf(
                    Godkjenning(
                        helsepersonellkategori = Kode(true, 1, "verdi"),
                        autorisasjon = Kode(true, 2, "annenVerdi")
                    )
                )
            )
        }

        test("Should throw exeption when exceeds max retries") {
            assertFailsWith<IOException> {
                norskHelsenettClient.finnBehandler(
                    "1234",
                    "1",
                    LoggingMeta("", "", "123", "")
                )
            }
        }
    }
})
