package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.LoggingMeta
import no.nav.syfo.api.log
import no.nav.syfo.model.Periode
import no.nav.syfo.model.Syketilfelle
import no.nav.syfo.rules.sortedFOMDate
import no.nav.syfo.rules.sortedTOMDate

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

    suspend fun finnStartdatoForSammenhengendeSyketilfelle(aktorId: String, periodeliste: List<Periode>, loggingMeta: LoggingMeta): LocalDate? {
        log.info("Sjekker om nytt syketilfelle mot syfosyketilfelle {}", fields(loggingMeta))
        val sykeforloep = hentSykeforloep(aktorId)

        return finnStartdato(sykeforloep, periodeliste, loggingMeta)
    }

    fun finnStartdato(sykeforloep: List<Sykeforloep>, periodeliste: List<Periode>, loggingMeta: LoggingMeta): LocalDate? {
        if (sykeforloep.isEmpty()) {
            return null
        }
        val forsteFomIMottattSykmelding = periodeliste.sortedFOMDate().firstOrNull()
        val sisteTomIMottattSykmelding = periodeliste.sortedTOMDate().lastOrNull()
        if (forsteFomIMottattSykmelding == null || sisteTomIMottattSykmelding == null) {
            log.warn("Mangler fom eller tom for sykmeldingsperioder: {}", fields(loggingMeta))
            return null
        }
        val periodeRange = forsteFomIMottattSykmelding.rangeTo(sisteTomIMottattSykmelding)
        val sammeSykeforloep = sykeforloep.firstOrNull {
            it.sykmeldinger.any { simpleSykmelding -> simpleSykmelding.erSammeOppfolgingstilfelle(periodeRange) }
        }
        return sammeSykeforloep?.oppfolgingsdato
    }

    private fun SimpleSykmelding.erSammeOppfolgingstilfelle(periodeRange: ClosedRange<LocalDate>): Boolean {
        if (fom.minusDays(16) in periodeRange || tom.plusDays(16) in periodeRange) {
            return true
        }
        return false
    }

    private suspend fun hentSykeforloep(aktorId: String): List<Sykeforloep> =
        httpClient.get<List<Sykeforloep>>("$endpointUrl/sparenaproxy/$aktorId/sykeforloep") {
            accept(ContentType.Application.Json)
            val oidcToken = stsClient.oidcToken()
            headers {
                append("Authorization", "Bearer ${oidcToken.access_token}")
            }
        }
}

data class Sykeforloep(
    var oppfolgingsdato: LocalDate,
    val sykmeldinger: List<SimpleSykmelding>
)

data class SimpleSykmelding(
    val id: String,
    val fom: LocalDate,
    val tom: LocalDate
)
