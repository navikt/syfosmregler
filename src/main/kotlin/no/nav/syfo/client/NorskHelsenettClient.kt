package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import java.io.IOException
import java.time.LocalDateTime
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.helpers.retry
import no.nav.syfo.rules.api.log
import no.nav.syfo.utils.LoggingMeta

class NorskHelsenettClient(
    private val endpointUrl: String,
    private val accessTokenClient: AzureAdV2Client,
    private val resourceId: String,
    private val httpClient: HttpClient,
) {

    suspend fun finnBehandler(
        behandlerFnr: String,
        msgId: String,
        loggingMeta: LoggingMeta
    ): Behandler? =
        retry(
            callName = "finnbehandler",
            retryIntervals = arrayOf(500L, 1000L, 1000L),
        ) {
            log.info("Henter behandler fra syfohelsenettproxy for msgId {}", msgId)
            val httpResponse =
                httpClient
                    .get("$endpointUrl/api/v2/behandler") {
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
                    }
                    .also {
                        log.info("Hentet behandler for msgId {}, {}", msgId, fields(loggingMeta))
                    }

            return@retry when (httpResponse.status) {
                NotFound -> {
                    log.warn("BehandlerFnr ikke funnet {}, {}", msgId, fields(loggingMeta))
                    null
                }
                BadRequest -> {
                    log.error(
                        "BehandlerFnr mangler i request for msgId {}, {}",
                        msgId,
                        fields(loggingMeta)
                    )
                    null
                }
                InternalServerError -> {
                    log.error(
                        "Syfohelsenettproxy svarte med feilmelding for msgId {}, {}",
                        msgId,
                        httpResponse.body()
                    )
                    throw IOException("Syfohelsenettproxy svarte med feilmelding for $msgId")
                }
                OK -> httpResponse.body()
                else -> {
                    log.warn("Did not get OK from helsenett for msgid: $msgId", fields(loggingMeta))
                    null
                }
            }
        }
}

data class Behandler(
    val godkjenninger: List<Godkjenning>,
    val hprNummer: Int? = null,
)

data class Godkjenning(
    val helsepersonellkategori: Kode? = null,
    val autorisasjon: Kode? = null,
    val tillegskompetanse: List<Tilleggskompetanse>? = null,
)

data class Tilleggskompetanse(
    val avsluttetStatus: Kode?,
    val eTag: String?,
    val gyldig: Periode?,
    val id: Int?,
    val type: Kode?
)

data class Periode(val fra: LocalDateTime?, val til: LocalDateTime?)

data class Kode(
    val aktiv: Boolean,
    val oid: Int,
    val verdi: String?,
)
