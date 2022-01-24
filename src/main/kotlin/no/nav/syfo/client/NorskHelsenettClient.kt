package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.LoggingMeta
import no.nav.syfo.api.log
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.helpers.retry
import java.io.IOException

class NorskHelsenettClient(
    private val endpointUrl: String,
    private val accessTokenClient: AzureAdV2Client,
    private val resourceId: String,
    private val httpClient: HttpClient
) {

    suspend fun finnBehandler(behandlerFnr: String, msgId: String, loggingMeta: LoggingMeta): Behandler? = retry(
        callName = "finnbehandler",
        retryIntervals = arrayOf(500L, 1000L, 1000L)
    ) {
        log.info("Henter behandler fra syfohelsenettproxy for msgId {}", msgId)
        try {
            return@retry httpClient.get<Behandler>("$endpointUrl/api/v2/behandler") {
                accept(ContentType.Application.Json)
                val accessToken = accessTokenClient.getAccessToken(resourceId)
                if (accessToken?.accessToken == null) {
                    throw RuntimeException("Klarte ikke hente ut accesstoken for HPR")
                }
                headers {
                    append("Authorization", "Bearer ${accessToken.accessToken}")
                    append("Nav-CallId", msgId)
                    append("behandlerFnr", behandlerFnr)
                }
            }.also { log.info("Hentet behandler for msgId {}, {}", msgId, fields(loggingMeta)) }
        } catch (e: Exception) {
            if (e is ClientRequestException && e.response.status == NotFound) {
                log.warn("BehandlerFnr ikke funnet {}, {}", msgId, fields(loggingMeta))
                return@retry null
            } else if (e is ClientRequestException && e.response.status == BadRequest) {
                log.error("BehandlerFnr mangler i request for msgId {}, {}", msgId, fields(loggingMeta))
                return@retry null
            } else {
                log.error("Syfohelsenettproxy svarte med feilmelding for msgId {}: {}", msgId, e.message)
                throw IOException("Syfohelsenettproxy svarte med feilmelding for $msgId")
            }
        }
    }
}

data class Behandler(
    val godkjenninger: List<Godkjenning>,
    val hprNummer: Int? = null
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
