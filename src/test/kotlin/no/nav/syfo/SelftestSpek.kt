package no.nav.syfo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import no.nav.syfo.api.registerNaisApi
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nhn.schemas.reg.hprv2.IHPR2Service
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotEqual
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SelftestSpek : Spek({
    val applicationState = ApplicationState()

    afterGroup {
        applicationState.running = false
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

            application.initRouting(applicationState, personV3, helsepersonellv1)

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
