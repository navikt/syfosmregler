package no.nav.syfo.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import io.mockk.coEvery
import io.mockk.mockk
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import no.nav.syfo.LoggingMeta
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBe
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@KtorExperimentalAPI
object NorskHelsenettClientSpek : Spek({
    val accessTokenClientMock = mockk<AccessTokenClient>()
    val httpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
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
            get("/syfohelsenettproxy/api/behandler") {
                when {
                    call.request.headers["behandlerFnr"] == "behandler" -> call.respond(Behandler(listOf(Godkjenning(helsepersonellkategori = Kode(true, 1, "verdi"), autorisasjon = Kode(true, 2, "annenVerdi")))))
                    call.request.headers["behandlerFnr"] == "behandlerFinnesIkke" -> call.respond(HttpStatusCode.NotFound, "Behandler finnes ikke")
                    else -> call.respond(HttpStatusCode.InternalServerError, "Noe gikk galt")
                }
            }
        }
    }.start()

    val norskHelsenettClient = NorskHelsenettClient("$mockHttpServerUrl/syfohelsenettproxy", accessTokenClientMock, "resourceId", httpClient)

    afterGroup {
        mockServer.stop(TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(10))
    }

    beforeGroup {
        coEvery { accessTokenClientMock.hentAccessToken(any()) } returns "token"
    }

    describe("Håndtering av respons fra syfohelsenettproxy") {
        it("Får hente behandler som finnes") {
            var behandler: Behandler? = null
            runBlocking {
                behandler = norskHelsenettClient.finnBehandler(
                        "behandler",
                        "sykmeldingsId",
                        LoggingMeta("", "", "sykmeldingsId", ""))
            }

            behandler shouldNotBe null
            behandler?.godkjenninger?.size shouldEqual 1
        }
        it("Returnerer null for behandler som ikke finnes") {
            var behandler: Behandler? = null
            runBlocking {
                behandler = norskHelsenettClient.finnBehandler(
                        "behandlerFinnesIkke",
                        "sykmeldingsId",
                        LoggingMeta("", "", "sykmeldingsId", ""))
            }

            behandler shouldEqual null
        }
    }
})
