package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import no.nav.syfo.api.LegeSuspensjonClient
import no.nav.syfo.api.SyketilfelleClient
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.api.registerRuleApi
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.ws.createPort
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nhn.schemas.reg.hprv2.IHPR2Service
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor
import org.apache.cxf.message.Message
import org.apache.cxf.phase.Phase
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smregler")

data class ApplicationState(var ready: Boolean = false, var running: Boolean = true)

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

@KtorExperimentalAPI
fun main() {
    val env = Environment()
    val credentials = objectMapper.readValue<VaultCredentials>(Paths.get("/var/run/secrets/nais.io/vault/credentials.json").toFile())
    val applicationState = ApplicationState()

    if (Diagnosekoder.icd10.isEmpty() || Diagnosekoder.icpc2.isEmpty()) {
        throw RuntimeException("ICD10 or ICPC2 diagnose codes failed to load.")
    }

    DefaultExports.initialize()
    val personV3 = createPort<PersonV3>(env.personV3EndpointURL) {
        port { withSTS(credentials.serviceuserUsername, credentials.serviceuserPassword, env.securityTokenServiceURL) }
    }

    val helsepersonellV1 = createPort<IHPR2Service>(env.helsepersonellv1EndpointURL) {
        proxy {
            // TODO: Contact someone about this hacky workaround
            // talk to HDIR about HPR about they claim to send a ISO-8859-1 but its really UTF-8 payload
            val interceptor = object : AbstractSoapInterceptor(Phase.RECEIVE) {
                override fun handleMessage(message: SoapMessage?) {
                    if (message != null)
                        message[Message.ENCODING] = "utf-8"
                }
            }

            inInterceptors.add(interceptor)
            inFaultInterceptors.add(interceptor)
            features.add(WSAddressingFeature())
        }

        port { withSTS(credentials.serviceuserUsername, credentials.serviceuserPassword, env.securityTokenServiceURL) }
    }

    val oidcClient = StsOidcClient(credentials.serviceuserUsername, credentials.serviceuserPassword)
    val legeSuspensjonClient = LegeSuspensjonClient(env.legeSuspensjonEndpointURL, credentials, oidcClient)
    val syketilfelleClient = SyketilfelleClient(env.syketilfelleEndpointURL, oidcClient)

    val applicationServer = embeddedServer(Netty, env.applicationPort) {
        initRouting(applicationState, personV3, helsepersonellV1, legeSuspensjonClient, syketilfelleClient)
        applicationState.ready = true
    }.start(wait = true)

    Runtime.getRuntime().addShutdownHook(Thread {
        // Kubernetes polls every 5 seconds for liveness, mark as not ready and wait 5 seconds for a new readyness check
        applicationState.ready = false
        Thread.sleep(10000)
        applicationServer.stop(10, 10, TimeUnit.SECONDS)
    })
}

@KtorExperimentalAPI
fun Application.initRouting(applicationState: ApplicationState, personV3: PersonV3, helsepersonellv1: IHPR2Service, legeSuspensjonClient: LegeSuspensjonClient, syketilfelleClient: SyketilfelleClient) {
    routing {
        registerNaisApi(readynessCheck = { applicationState.ready }, livenessCheck = { applicationState.running })
        registerRuleApi(personV3, helsepersonellv1, legeSuspensjonClient, syketilfelleClient)
    }
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
        }
    }
    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")

            log.error("Caught exception while trying to validate against rules", cause)
            throw cause
        }
    }
}
