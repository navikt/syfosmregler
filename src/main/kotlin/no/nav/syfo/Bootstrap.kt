package no.nav.syfo

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.api.registerRuleApi
import no.nav.syfo.ws.configureSTSFor
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nhn.schemas.reg.hprv2.IHPR2Service
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.message.Message
import org.apache.cxf.phase.Phase
import org.apache.cxf.ws.addressing.WSAddressingFeature
import java.util.concurrent.TimeUnit

fun doReadynessCheck(): Boolean {
    // Do validation
    return true
}

data class ApplicationState(var running: Boolean = true)

// TODO: WS calls required
// TSS leger som har mistet rett se https://jira.adeo.no/browse/REG-1397
fun main(args: Array<String>) {
    val env = Environment()
    val applicationState = ApplicationState()

    val personV3 = JaxWsProxyFactoryBean().apply {
        address = env.personV3EndpointURL
        features.add(LoggingFeature())
        serviceClass = PersonV3::class.java
    }.create() as PersonV3
    configureSTSFor(personV3, env.srvsyfosmreglerUsername,
            env.srvsyfosmreglerPassword, env.securityTokenServiceUrl)

    val helsepersonellv1 = JaxWsProxyFactoryBean().apply {
        address = env.helsepersonellv1EndpointUrl
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
        features.add(LoggingFeature())
        features.add(WSAddressingFeature())
        serviceClass = IHPR2Service::class.java
    }.create() as IHPR2Service
    configureSTSFor(helsepersonellv1, env.srvsyfosmreglerUsername,
    env.srvsyfosmreglerPassword, env.securityTokenServiceUrl)

    val applicationServer = embeddedServer(Netty, env.applicationPort) {
        initRouting(applicationState, personV3, helsepersonellv1)
    }.start(wait = true)

    Runtime.getRuntime().addShutdownHook(Thread {
        applicationServer.stop(10, 10, TimeUnit.SECONDS)
    })
}

fun Application.initRouting(applicationState: ApplicationState, personV3: PersonV3, helsepersonellv1: IHPR2Service) {
    routing {
        registerNaisApi(readynessCheck = ::doReadynessCheck, livenessCheck = { applicationState.running })
        registerRuleApi(personV3, helsepersonellv1)
    }
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
        }
    }
}
