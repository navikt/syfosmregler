package no.nav.syfo.rules.legesuspensjon

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import java.io.IOException
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.metrics.SUSPANSJON_HISTOGRAM
import no.nav.syfo.utils.LoggingMeta
import org.slf4j.LoggerFactory

class LegeSuspensjonClient(
    private val endpointUrl: String,
    private val azureAdV2Client: AzureAdV2Client,
    private val httpClient: HttpClient,
    private val scope: String,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    suspend fun checkTherapist(
        therapistId: String,
        ediloggid: String,
        oppslagsdato: String,
        loggingMeta: LoggingMeta,
    ): Suspendert {
        val timer = SUSPANSJON_HISTOGRAM.labels("lege_suspansjon").startTimer()
        val httpResponse =
            httpClient.get("$endpointUrl/api/v1/suspensjon/status") {
                accept(ContentType.Application.Json)
                val accessToken = azureAdV2Client.getAccessToken(scope)
                if (accessToken?.accessToken == null) {
                    throw RuntimeException("Klarte ikke hente ut accesstoken for btsys")
                }
                headers {
                    append("Nav-Call-Id", ediloggid)
                    append("Nav-Consumer-Id", "srvsyfosmregler")
                    append("Nav-Personident", therapistId)

                    append("Authorization", "Bearer ${accessToken.accessToken}")
                }
                parameter("oppslagsdato", oppslagsdato)
            }
        timer.observeDuration()

        log.info(
            "Hentet supensjonstatus for ediloggId {}, {}",
            ediloggid,
            fields(loggingMeta),
        )

        return when (httpResponse.status) {
            HttpStatusCode.OK -> httpResponse.body()
            else -> {
                log.error(
                    "Btsys svarte med kode {} for ediloggId {}, {}",
                    httpResponse.status,
                    ediloggid,
                    fields(loggingMeta),
                )
                throw IOException(
                    "Btsys svarte med uventet kode ${httpResponse.status} for $ediloggid",
                )
            }
        }
    }
}

data class Suspendert(val suspendert: Boolean)
