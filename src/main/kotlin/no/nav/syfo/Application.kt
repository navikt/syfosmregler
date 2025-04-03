package no.nav.syfo

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.util.concurrent.TimeUnit
import no.nav.syfo.plugins.configureAuth
import no.nav.syfo.plugins.configureContentNegotiation
import no.nav.syfo.plugins.configureInternalRouting
import no.nav.syfo.plugins.configureRouting
import no.nav.syfo.utils.Environment

fun main() {
    val env = Environment()
    val embeddedServer = embeddedServer(Netty, port = env.port, module = Application::module)

    Runtime.getRuntime()
        .addShutdownHook(
            Thread {
                embeddedServer.stop(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10))
            },
        )
    embeddedServer.start(true)
}

fun Application.module() {
    val applicationState = ApplicationState()

    configureContentNegotiation()
    configureAuth()
    configureInternalRouting(applicationState)
    configureRouting()
}

data class ApplicationState(
    var alive: Boolean = true,
    var ready: Boolean = true,
)
