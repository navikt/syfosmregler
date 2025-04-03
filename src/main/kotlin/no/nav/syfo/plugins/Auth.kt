package no.nav.syfo.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeUnit
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.utils.Environment
import no.nav.syfo.utils.logger

fun Application.configureAuth() {
    val env = Environment()

    val jwkProviderAad =
        JwkProviderBuilder(URI.create(env.jwkKeysUrl).toURL())
            .cached(10, Duration.ofHours(24))
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    install(Authentication) {
        jwt(name = "servicebrukerAAD") {
            verifier(jwkProviderAad, env.jwtIssuer)
            validate { credentials ->
                when {
                    harTilgang(credentials, env.clientIdV2) -> JWTPrincipal(credentials.payload)
                    else -> unauthorized(credentials)
                }
            }
        }
    }
}

private fun harTilgang(credentials: JWTCredential, clientId: String): Boolean {
    val appid: String = credentials.payload.getClaim("azp").asString()
    logger.debug("authorization attempt for $appid")
    return credentials.payload.audience.contains(clientId)
}

private fun unauthorized(credentials: JWTCredential): Unit? {
    logger.warn(
        "Auth: Unexpected audience for jwt {}, {}",
        StructuredArguments.keyValue("issuer", credentials.payload.issuer),
        StructuredArguments.keyValue("audience", credentials.payload.audience),
    )
    return null
}
