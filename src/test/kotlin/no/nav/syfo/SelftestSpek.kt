package no.nav.syfo

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.api.LegeSuspensjonClient
import no.nav.syfo.api.OppfolgingstilfelleDTO
import no.nav.syfo.api.PeriodeDTO
import no.nav.syfo.api.StsOidcClient
import no.nav.syfo.api.SyketilfelleClient
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.model.OidcToken
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nhn.schemas.reg.hprv2.IHPR2Service
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotEqual
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.ServerSocket
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
object SelftestSpek : Spek({
    val applicationState = ApplicationState()
    val mockHttpServerPort = ServerSocket(0).use { it.localPort }
    val mockHttpServerUrl = "http://localhost:$mockHttpServerPort"
    suspend fun ApplicationCall.respondJson(input: Any) {
        respondTextWriter(ContentType.Application.Json) {
            objectMapper.writeValue(this, input)
        }
    }
    val mockServer = embeddedServer(Netty, mockHttpServerPort) {
        routing {
            get("/rest/v1/sts/token") {
                call.respondJson(OidcToken("", "", 1L))
            }

            get("/api/v1/suspensjon/status") {
                call.respondJson(true)
            }

            post("syfosoknad/oppfolgingstilfelle/beregn/") {
                call.respondJson(OppfolgingstilfelleDTO(PeriodeDTO(LocalDate.now(), LocalDate.now())))
            }
        }
    }.start()

    afterGroup {
        applicationState.running = false
        mockServer.stop(1L, 10L, TimeUnit.SECONDS)
    }
    describe("Calling selftest with successful liveness and readyness tests") {
        with(TestApplicationEngine()) {
            start()
            val personV3 = JaxWsProxyFactoryBean().apply {
            address = "http://personv3/api"
            features.add(LoggingFeature())
            serviceClass = PersonV3::class.java
        }.create() as PersonV3

            val helsepersonellv1 = JaxWsProxyFactoryBean().apply {
                address = "http://helsepersnolellv1/api"
                features.add(LoggingFeature())
                features.add(WSAddressingFeature())
                serviceClass = IHPR2Service::class.java
            }.create() as IHPR2Service

            val credentials = VaultCredentials("", "")
            val oidcClient = StsOidcClient(mockHttpServerUrl, "username", "password")
            val legeSuspensjonClient = LegeSuspensjonClient(mockHttpServerUrl, credentials, oidcClient)
            val syketilfelleClient = SyketilfelleClient(mockHttpServerUrl, oidcClient)

            application.initRouting(applicationState, personV3, helsepersonellv1, legeSuspensjonClient, syketilfelleClient)

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
