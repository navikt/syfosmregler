package no.nav.syfo.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.auth.basic.BasicAuth
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.syfo.REST_CALL_SUMMARY
import no.nav.syfo.model.OidcToken

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
            oidcToken = newOidcToken()
            tokenExpires = System.currentTimeMillis() + (oidcToken.expires_in - 600) * 1000
        }
        return oidcToken
    }

    private suspend fun newOidcToken(): OidcToken = REST_CALL_SUMMARY.labels("oidc").startTimer().use {
        oidcClient.get("$endpointUrl/rest/v1/sts/token") {
            parameter("grant_type", "client_credentials")
            parameter("scope", "openid")
        }
    }
}
