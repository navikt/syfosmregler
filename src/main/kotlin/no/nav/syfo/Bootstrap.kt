package no.nav.syfo

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
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import java.util.concurrent.TimeUnit

fun doReadynessCheck(): Boolean {
    // Do validation
    return true
}

data class ApplicationState(var running: Boolean = true)

// TODO: WS calls required
// TPS
// kuhr-sar
// Addresseregister
// Fastlegeregister
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

    val applicationServer = embeddedServer(Netty, env.applicationPort) {
        initRouting(applicationState, personV3)
    }.start(wait = true)

    Runtime.getRuntime().addShutdownHook(Thread {
        applicationServer.stop(10, 10, TimeUnit.SECONDS)
    })
}

fun Application.initRouting(applicationState: ApplicationState, personV3: PersonV3) {
    routing {
        registerNaisApi(readynessCheck = ::doReadynessCheck, livenessCheck = { applicationState.running })
        registerRuleApi(personV3)
    }
    install(ContentNegotiation) {
        jackson {}
    }
}

inline fun <reified T> XMLEIFellesformat.get() = this.any.find { it is T } as T
