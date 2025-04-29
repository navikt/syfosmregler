package no.nav.syfo.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.prometheus.client.hotspot.DefaultExports
import no.nav.syfo.ApplicationState
import no.nav.syfo.metrics.monitorHttpRequests
import no.nav.syfo.nais.naisIsAliveRoute
import no.nav.syfo.nais.naisIsReadyRoute
import no.nav.syfo.nais.naisPrometheusRoute
import no.nav.syfo.utils.logger

fun Application.configureInternalRouting(
    applicationState: ApplicationState,
) {
    routing {
        naisIsAliveRoute(applicationState)
        naisIsReadyRoute(applicationState)
        naisPrometheusRoute()
        swaggerUI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
            logger.error("Caught exception while trying to validate against rules", cause)
            throw cause
        }
    }
    intercept(ApplicationCallPipeline.Monitoring, monitorHttpRequests())

    DefaultExports.initialize()
}
