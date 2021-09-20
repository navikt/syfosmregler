package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.util.KtorExperimentalAPI
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.LoggingMeta
import no.nav.syfo.log
import no.nav.syfo.model.Periode
import no.nav.syfo.rules.sortedFOMDate
import java.time.LocalDate
import java.time.OffsetDateTime

@KtorExperimentalAPI
class SmregisterClient(
    private val smregisterEndpointURL: String,
    private val accessTokenClientV2: AccessTokenClientV2,
    private val resourceId: String,
    private val httpClient: HttpClient
) {

    suspend fun finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert(fnr: String, periodeliste: List<Periode>, loggingMeta: LoggingMeta): Boolean {
        log.info("Sjekker om finnes sykmeldinger med samme fom som ikke er tilbakedatert {}", fields(loggingMeta))
        val forsteFomIMottattSykmelding = periodeliste.sortedFOMDate().firstOrNull() ?: return false
        try {
            val sykmeldinger = hentSykmeldinger(fnr)
            sykmeldinger.filter {
                it.behandlingsutfall.status != RegelStatusDTO.INVALID && it.sykmeldingsperioder.sortedFOMDate().firstOrNull() == forsteFomIMottattSykmelding
            }.forEach {
                if (it.behandletTidspunkt.toLocalDate() <= forsteFomIMottattSykmelding.plusDays(8)) {
                    log.info("Fant sykmelding med samme fom som ikke er tilbakedatert {}", fields(loggingMeta))
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            log.error("Feil ved henting av tidligere sykmeldinger {}", fields(loggingMeta))
            throw e
        }
    }

    private suspend fun hentSykmeldinger(fnr: String): List<SykmeldingDTO> =
        httpClient.get<List<SykmeldingDTO>>("$smregisterEndpointURL/api/v2/sykmelding/sykmeldinger") {
            accept(ContentType.Application.Json)
            val accessToken = accessTokenClientV2.getAccessTokenV2(resourceId)
            headers {
                append("Authorization", "Bearer $accessToken")
                append("fnr", fnr)
            }
        }
}

data class SykmeldingDTO(
    val id: String,
    val behandlingsutfall: BehandlingsutfallDTO,
    val sykmeldingsperioder: List<SykmeldingsperiodeDTO>,
    val behandletTidspunkt: OffsetDateTime
)

data class SykmeldingsperiodeDTO(
    val fom: LocalDate,
    val tom: LocalDate
)

fun List<SykmeldingsperiodeDTO>.sortedFOMDate(): List<LocalDate> =
    map { it.fom }.sorted()

data class BehandlingsutfallDTO(
    val status: RegelStatusDTO
)

enum class RegelStatusDTO {
    OK, MANUAL_PROCESSING, INVALID
}
