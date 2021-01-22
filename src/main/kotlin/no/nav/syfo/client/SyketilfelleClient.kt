package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.LoggingMeta
import no.nav.syfo.api.log
import no.nav.syfo.model.Syketilfelle

@KtorExperimentalAPI
class SyketilfelleClient(private val endpointUrl: String, private val stsClient: StsOidcClient, private val httpClient: HttpClient) {

    suspend fun fetchErNytttilfelle(syketilfelleList: List<Syketilfelle>, aktorId: String, loggingMeta: LoggingMeta): Boolean {
        log.info("Sjekker om nytt syketilfelle mot syfosyketilfelle {}", fields(loggingMeta))
        try {
            return httpClient.post<Boolean>("$endpointUrl/oppfolgingstilfelle/ernytttilfelle/$aktorId") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                val oidcToken = stsClient.oidcToken()
                headers {
                    append("Authorization", "Bearer ${oidcToken.access_token}")
                }
                body = syketilfelleList
            }
        } catch (e: Exception) {
            log.error("Kall mot syfosyketilfelle feilet {}", fields(loggingMeta))
            throw e
        }
    }
}

data class Oppfolgingstilfelle(val antallBrukteDager: Int, val oppbruktArbeidsgvierperiode: Boolean, val arbeidsgiverperiode: Periode?)

data class Periode(val fom: LocalDate, val tom: LocalDate)
