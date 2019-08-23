package no.nav.syfo.api

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import java.time.LocalDate
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.helpers.retry
import no.nav.syfo.model.Syketilfelle

@KtorExperimentalAPI
class SyketilfelleClient(private val endpointUrl: String, private val stsClient: StsOidcClient, private val httpClient: HttpClient) {

    suspend fun fetchErNytttilfelle(syketilfelleList: List<Syketilfelle>, aktorId: String): Boolean = retry("ernytttilfelle") {
        httpClient.post<Boolean>("$endpointUrl/oppfolgingstilfelle/ernytttilfelle/$aktorId") {
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

data class Oppfolgingstilfelle(val antallBrukteDager: Int, val oppbruktArbeidsgvierperiode: Boolean, val arbeidsgiverperiode: Periode?)

data class Periode(val fom: LocalDate, val tom: LocalDate)
