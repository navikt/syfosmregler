package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.log
import no.nav.syfo.model.Periode
import no.nav.syfo.rules.sortedFOMDate
import no.nav.syfo.rules.sortedTOMDate
import no.nav.syfo.utils.LoggingMeta
import java.time.LocalDate
import java.time.OffsetDateTime

class SmregisterClient(
    private val smregisterEndpointURL: String,
    private val accessTokenClientV2: AzureAdV2Client,
    private val scope: String,
    private val httpClient: HttpClient
) {

    suspend fun finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert(fnr: String, periodeliste: List<Periode>, diagnosekode: String?, loggingMeta: LoggingMeta): Boolean {
        log.info("Sjekker om finnes sykmeldinger med samme fom som ikke er tilbakedatert {}", fields(loggingMeta))
        val forsteFomIMottattSykmelding = periodeliste.sortedFOMDate().firstOrNull() ?: return false
        val sisteTomIMottattSykmelding = periodeliste.sortedTOMDate().lastOrNull() ?: return false
        val mottattSykmeldingRange = forsteFomIMottattSykmelding.rangeTo(sisteTomIMottattSykmelding)
        try {
            val sykmeldinger = hentSykmeldinger(fnr)
            sykmeldinger.filter {
                it.behandlingsutfall.status != RegelStatusDTO.INVALID && it.behandletTidspunkt.toLocalDate() <= forsteFomIMottattSykmelding.plusDays(8)
            }.forEach {
                if (it.sykmeldingsperioder.sortedFOMDate().first() == forsteFomIMottattSykmelding) {
                    log.info("Fant sykmelding med samme fom som ikke er tilbakedatert {}", fields(loggingMeta))
                    return true
                } else if (it.medisinskVurdering?.hovedDiagnose?.kode == diagnosekode &&
                    (
                        (
                            it.sykmeldingsperioder.sortedFOMDate().first() in mottattSykmeldingRange &&
                                it.sykmeldingsperioder.minByOrNull { it.fom }!!.periodelisteInneholderSammeType(periodeliste)
                            ) ||
                            (
                                it.sykmeldingsperioder.sortedTOMDate().last() in mottattSykmeldingRange &&
                                    it.sykmeldingsperioder.maxByOrNull { it.tom }!!.periodelisteInneholderSammeType(periodeliste)
                                )
                        )
                ) {
                    log.info(
                        "Fant sykmelding med delvis overlappende periode og samme diagnose og type som ikke er tilbakedatert, " +
                            "sykmeldingsID {} {}",
                        it.id,
                        fields(loggingMeta)
                    )
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
        httpClient.get("$smregisterEndpointURL/api/v2/sykmelding/sykmeldinger") {
            accept(ContentType.Application.Json)
            val accessToken = accessTokenClientV2.getAccessToken(scope)
            if (accessToken?.accessToken == null) {
                throw RuntimeException("Klarte ikke hente ut accessToken for smregister")
            }
            headers {
                append("Authorization", "Bearer ${accessToken.accessToken}")
                append("fnr", fnr)
            }
        }.body()
}

data class SykmeldingDTO(
    val id: String,
    val behandlingsutfall: BehandlingsutfallDTO,
    val sykmeldingsperioder: List<SykmeldingsperiodeDTO>,
    val behandletTidspunkt: OffsetDateTime,
    val medisinskVurdering: MedisinskVurderingDTO?
)

data class SykmeldingsperiodeDTO(
    val fom: LocalDate,
    val tom: LocalDate,
    val gradert: GradertDTO?,
    val type: PeriodetypeDTO
)

data class MedisinskVurderingDTO(
    val hovedDiagnose: DiagnoseDTO?
)

data class DiagnoseDTO(
    val kode: String
)

data class GradertDTO(
    val grad: Int,
    val reisetilskudd: Boolean
)

enum class PeriodetypeDTO {
    AKTIVITET_IKKE_MULIG,
    AVVENTENDE,
    BEHANDLINGSDAGER,
    GRADERT,
    REISETILSKUDD
}

data class BehandlingsutfallDTO(
    val status: RegelStatusDTO
)

enum class RegelStatusDTO {
    OK, MANUAL_PROCESSING, INVALID
}

fun List<SykmeldingsperiodeDTO>.sortedFOMDate(): List<LocalDate> =
    map { it.fom }.sorted()

fun List<SykmeldingsperiodeDTO>.sortedTOMDate(): List<LocalDate> =
    map { it.tom }.sorted()

fun SykmeldingsperiodeDTO.periodelisteInneholderSammeType(perioder: List<Periode>): Boolean {
    val periodetyper = perioder.mapNotNull { it.tilPeriodetypeDTO() }.distinct()

    return if (periodetyper.contains(type) && type != PeriodetypeDTO.GRADERT) {
        true
    } else if (periodetyper.contains(type) && type == PeriodetypeDTO.GRADERT) {
        val sykmeldingsgrader = perioder.mapNotNull { it.gradert?.grad }
        sykmeldingsgrader.contains(gradert?.grad)
    } else {
        false
    }
}

fun Periode.tilPeriodetypeDTO(): PeriodetypeDTO? {
    return when {
        aktivitetIkkeMulig != null -> return PeriodetypeDTO.AKTIVITET_IKKE_MULIG
        gradert != null -> return PeriodetypeDTO.GRADERT
        reisetilskudd -> return PeriodetypeDTO.REISETILSKUDD
        avventendeInnspillTilArbeidsgiver != null -> return PeriodetypeDTO.AVVENTENDE
        behandlingsdager != null && behandlingsdager!! > 0 -> return PeriodetypeDTO.BEHANDLINGSDAGER
        else -> null
    }
}
