package no.nav.syfo.api

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.response.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.helpers.retry

@KtorExperimentalAPI
class NorskHelsenettClient(private val endpointUrl: String, private val stsClient: StsOidcClient) {
    private val httpClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
            expectSuccess = false
        }
    }

    suspend fun finnBehandler(behandlerFnr: String): Behandler? = retry(
        callName = "finnbehandler",
        retryIntervals = arrayOf(500L, 1000L, 3000L, 5000L, 10000L)) {
        val httpResponse = httpClient.get<HttpResponse>("$endpointUrl/api/behandler") {
            accept(ContentType.Application.Json)
            val oidcToken = stsClient.oidcToken()
            headers {
                append("Authorization", "Bearer ${oidcToken.access_token}")
            }
            parameter("behandlerFnr", behandlerFnr)
        }

        if (httpResponse.status == NotFound) {
            log.info("Fant ikke behandler")
            null
        } else {
            httpResponse.call.response.receive<Behandler>()
        }
    }
}

data class Behandler(
    val godkjenninger: List<Godkjenning>
)

data class Godkjenning(
    val helsepersonellkategori: Kode? = null,
    val autorisasjon: Kode? = null
)

data class Kode(
    val aktiv: Boolean,
    val oid: Int,
    val verdi: String?
)
