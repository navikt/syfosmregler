package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.features.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.util.KtorExperimentalAPI
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.LoggingMeta
import no.nav.syfo.VaultCredentials
import no.nav.syfo.api.log
import java.io.IOException

@KtorExperimentalAPI
class LegeSuspensjonClient(private val endpointUrl: String, private val credentials: VaultCredentials, private val stsClient: StsOidcClient, private val httpClient: HttpClient) {

    suspend fun checkTherapist(therapistId: String, ediloggid: String, oppslagsdato: String, loggingMeta: LoggingMeta): Suspendert {
        try {
            return httpClient.get<Suspendert>("$endpointUrl/api/v1/suspensjon/status") {
                accept(ContentType.Application.Json)
                val oidcToken = stsClient.oidcToken()
                headers {
                    append("Nav-Call-Id", ediloggid)
                    append("Nav-Consumer-Id", credentials.serviceuserUsername)
                    append("Nav-Personident", therapistId)

                    append("Authorization", "Bearer ${oidcToken.access_token}")
                }
                parameter("oppslagsdato", oppslagsdato)
            }.also { log.info("Hentet supensjonstatus for ediloggId {}, {}", ediloggid, fields(loggingMeta)) }
        } catch (e: ResponseException) {
            log.error("Btsys svarte med kode {} for ediloggId {}, {}, {}", e.response.status, ediloggid, fields(loggingMeta))
            throw IOException("Btsys svarte med uventet kode ${e.response.status} for $ediloggid")
        }
    }
}

data class Suspendert(val suspendert: Boolean)
