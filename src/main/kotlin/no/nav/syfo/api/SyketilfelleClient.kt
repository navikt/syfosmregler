package no.nav.syfo.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Deferred
import no.nav.syfo.model.Syketilfelle
import no.nav.syfo.retryAsync
import java.time.LocalDate

@KtorExperimentalAPI
class SyketilfelleClient(private val endpointUrl: String, private val stsClient: StsOidcClient) {
    private val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
    }

    suspend fun fetchSyketilfelle(syketilfelleList: List<Syketilfelle>, aktorId: String): Deferred<Oppfolgingstilfelle> = client.retryAsync("syketilfelle") {
        client.post<Oppfolgingstilfelle>("$endpointUrl/oppfolgingstilfelle/beregn/$aktorId") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            val oidcToken = stsClient.oidcToken()
            headers {
                append("Authorization", "Bearer ${oidcToken.access_token}")
            }
            body = syketilfelleList
        }
    }
}

data class Oppfolgingstilfelle(val antallBrukteDager: Int, val oppbruktArbeidsgvierperiode: Boolean, val arbeidsgiverPeriode: Periode?)

data class Periode(val fom: LocalDate, val tom: LocalDate)
