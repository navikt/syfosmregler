package no.nav.syfo.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.config
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.model.Syketilfelle
import java.time.LocalDate

@KtorExperimentalAPI
class SyketilfelleClient(private val endpointUrl: String, private val stsClient: StsOidcClient) {
    private val client = HttpClient(CIO.config {
        maxConnectionsCount = 1000 // Maximum number of socket connections.
        endpoint.apply {
            maxConnectionsPerRoute = 100
            pipelineMaxSize = 20
            keepAliveTime = 5000
            connectTimeout = 5000
            connectRetryAttempts = 5
        }
    }) {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
    }

    suspend fun fetchSyketilfelle(syketilfelleList: List<Syketilfelle>): Oppfolgingstilfelle =
            client.post("$endpointUrl/syfosoknad/oppfolgingstilfelle/beregn/") {
                accept(ContentType.Application.Json)
                val oidcToken = stsClient.oidcToken()
                headers {
                    append("Authorization", "Bearer ${oidcToken.access_token}")
                }
                body = syketilfelleList
            }
}

data class Oppfolgingstilfelle(val arbeidsgiverPeriode: Periode?)

data class Periode(val fom: LocalDate, val tom: LocalDate)