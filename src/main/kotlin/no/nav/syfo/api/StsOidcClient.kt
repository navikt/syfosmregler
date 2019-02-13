package no.nav.syfo.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.auth.basic.BasicAuth
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import no.nav.syfo.model.OidcToken
import no.nav.syfo.retryAsync

@KtorExperimentalAPI
class StsOidcClient(private val endpointUrl: String, username: String, password: String) {
    private var tokenExpires: Long = 0
    private val oidcClient = HttpClient(CIO) {
        install(BasicAuth) {
            this.username = username
            this.password = password
        }
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
    }

    private var oidcToken: OidcToken = runBlocking { oidcToken() }

    suspend fun oidcToken(): OidcToken {
        if (tokenExpires < System.currentTimeMillis()) {
            oidcToken = newOidcToken().await()
            tokenExpires = System.currentTimeMillis() + (oidcToken.expires_in - 600) * 1000
        }
        return oidcToken
    }

    private suspend fun newOidcToken(): Deferred<OidcToken> = oidcClient.retryAsync("oidc") {
        oidcClient.get<OidcToken>("$endpointUrl/rest/v1/sts/token") {
            parameter("grant_type", "client_credentials")
            parameter("scope", "openid")
        }
    }
}
