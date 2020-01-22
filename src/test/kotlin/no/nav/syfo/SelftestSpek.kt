package no.nav.syfo

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
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import java.net.ServerSocket
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import no.nav.syfo.api.AadAccessToken
import no.nav.syfo.api.AccessTokenClient
import no.nav.syfo.api.LegeSuspensjonClient
import no.nav.syfo.api.NorskHelsenettClient
import no.nav.syfo.api.Oppfolgingstilfelle
import no.nav.syfo.api.Periode
import no.nav.syfo.api.SyketilfelleClient
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.client.OidcToken
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.services.DiskresjonskodeService
import no.nav.syfo.services.RuleService
import no.nav.syfo.ws.createPort
import no.nav.tjeneste.pip.diskresjonskode.DiskresjonskodePortType
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@KtorExperimentalAPI
object SelftestSpek : Spek({
    val applicationState = ApplicationState()
    applicationState.ready = true
    val mockHttpServerPort = ServerSocket(0).use { it.localPort }
    val mockHttpServerUrl = "http://localhost:$mockHttpServerPort"
    val mockServer = embeddedServer(Netty, mockHttpServerPort) {
        install(ContentNegotiation) {
            jackson {}
        }
        routing {
            get("/rest/v1/sts/token") {
                call.respond(OidcToken("", "", 1L))
            }

            get("/aad/token") {
                call.respond(AadAccessToken("token", Instant.now().plusSeconds(300)))
            }

            get("/api/v1/suspensjon/status") {
                call.respond(true)
            }

            post("syfosoknad/oppfolgingstilfelle/beregn/") {
                call.respond(Oppfolgingstilfelle(1, false, Periode(LocalDate.now(), LocalDate.now())))
            }
        }
    }.start()

    afterGroup {
        applicationState.running = false
        mockServer.stop(TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(10))
    }
    describe("Calling selftest with successful liveness and readyness tests") {
        with(TestApplicationEngine()) {
            start()

            val credentials = VaultCredentials("", "", "secret")
            val oidcClient = StsOidcClient("username", "password", "$mockHttpServerUrl/rest/v1/sts/token")
            val httpClient = HttpClient(Apache) {
                install(JsonFeature) {
                    serializer = JacksonSerializer {
                        registerKotlinModule()
                        registerModule(JavaTimeModule())
                        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    }
                }
            }
            val accessTokenClient = AccessTokenClient("$mockHttpServerUrl/aad/token", "clientId", "clientsecret", httpClient)
            val legeSuspensjonClient = LegeSuspensjonClient(mockHttpServerUrl, credentials, oidcClient, httpClient)
            val syketilfelleClient = SyketilfelleClient(mockHttpServerUrl, oidcClient, httpClient)
            val norskHelsenettClient = NorskHelsenettClient(mockHttpServerUrl, accessTokenClient, "resourceId", httpClient)

            val env = Environment(
                    diskresjonskodeEndpointUrl = "DISKRESJONSKODE_ENDPOINT_URL",
                    securityTokenServiceURL = "SECURITY_TOKEN_SERVICE_URL",
                    clientId = "clientId",
                    helsenettproxyId = "helsenettproxyId",
                    aadAccessTokenUrl = "aadUrl"
            )

            val diskresjonskodePortType: DiskresjonskodePortType = createPort(env.diskresjonskodeEndpointUrl) {
                port { withSTS(credentials.serviceuserUsername, credentials.serviceuserPassword, env.securityTokenServiceURL) }
            }
            val ruleService = RuleService(legeSuspensjonClient, syketilfelleClient, DiskresjonskodeService(diskresjonskodePortType), norskHelsenettClient)
            application.initRouting(applicationState, ruleService)

            it("Returns ok on is_alive") {
                with(handleRequest(HttpMethod.Get, "/is_alive")) {
                    response.status() shouldEqual HttpStatusCode.OK
                    response.content shouldNotEqual null
                }
            }

            it("Returns ok on is_ready") {
                with(handleRequest(HttpMethod.Get, "/is_ready")) {
                    response.status() shouldEqual HttpStatusCode.OK
                    response.content shouldNotEqual null
                }
            }
        }
    }

    describe("Calling selftests with unsucessful liveness test") {
        with(TestApplicationEngine()) {
            start()
            application.routing {
                registerNaisApi(readynessCheck = { true }, livenessCheck = { false })
            }

            it("Returns internal server error when liveness check fails") {
                with(handleRequest(HttpMethod.Get, "/is_alive")) {
                    response.status() shouldEqual HttpStatusCode.InternalServerError
                    response.content shouldNotEqual null
                }
            }
        }
    }

    describe("Calling selftests with unsucessful readyness test") {
        with(TestApplicationEngine()) {
            start()
            application.routing {
                registerNaisApi(readynessCheck = { false }, livenessCheck = { true })
            }

            it("Returns internal server error when readyness check fails") {
                with(handleRequest(HttpMethod.Get, "/is_ready")) {
                    response.status() shouldEqual HttpStatusCode.InternalServerError
                    response.content shouldNotEqual null
                }
            }
        }
    }
})
