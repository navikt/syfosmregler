package no.nav.syfo.api

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.util.KtorExperimentalAPI
import java.io.IOException
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.LoggingMeta
import no.nav.syfo.VaultCredentials
import no.nav.syfo.client.StsOidcClient

@KtorExperimentalAPI
class LegeSuspensjonClient(private val endpointUrl: String, private val credentials: VaultCredentials, private val stsClient: StsOidcClient, private val httpClient: HttpClient) {

    suspend fun checkTherapist(therapistId: String, ediloggid: String, oppslagsdato: String, loggingMeta: LoggingMeta): Suspendert {
        val httpResponse = httpClient.get<HttpStatement>("$endpointUrl/api/v1/suspensjon/status") {
            accept(ContentType.Application.Json)
            val oidcToken = stsClient.oidcToken()
            headers {
                append("Nav-Call-Id", ediloggid)
                append("Nav-Consumer-Id", credentials.serviceuserUsername)
                append("Nav-Personident", therapistId)

                append("Authorization", "Bearer ${oidcToken.access_token}")
            }
            parameter("oppslagsdato", oppslagsdato)
        }.execute()
        when (httpResponse.status) {
            HttpStatusCode.OK -> {
                log.info("Hentet supensjonstatus for ediloggId {}, {}", ediloggid, fields(loggingMeta))
                return httpResponse.call.response.receive<Suspendert>()
            }
            else -> {
                log.error("Btsys svarte med kode {} for ediloggId {}, {}, {}", httpResponse.status, ediloggid, fields(loggingMeta))
                throw IOException("Btsys svarte med uventet kode ${httpResponse.status} for $ediloggid")
            }
        }
    }
}

data class Suspendert(val suspendert: Boolean)
