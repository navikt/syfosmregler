package no.nav.syfo.services

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.MerknadType
import no.nav.syfo.client.PeriodetypeDTO
import no.nav.syfo.client.RegelStatusDTO
import no.nav.syfo.client.SmregisterClient
import no.nav.syfo.client.SykmeldingDTO
import no.nav.syfo.client.tilPeriodetypeDTO
import no.nav.syfo.log
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.utils.LoggingMeta
import java.time.LocalDate

data class Forlengelse(val sykmeldingId: String, val fom: LocalDate, val tom: LocalDate)

data class SykmeldingMetadataInfo(
    val ettersendingAv: String?,
    val forlengelseAv: List<Forlengelse> = emptyList(),
)
class SykmeldingService(private val syfosmregisterClient: SmregisterClient) {
    suspend fun getSykmeldingMetadataInfo(fnr: String, sykmelding: Sykmelding, loggingMetadata: LoggingMeta): SykmeldingMetadataInfo {
        val tidligereSykmeldinger = syfosmregisterClient.getSykmeldinger(fnr)
            .filter { it.behandlingsutfall.status != RegelStatusDTO.INVALID }
            .filterNot { harTilbakedatertMerknad(it) }
            .filter { it.medisinskVurdering?.hovedDiagnose?.kode != null }
            .filter { it.medisinskVurdering?.hovedDiagnose?.kode == sykmelding.medisinskVurdering.hovedDiagnose?.kode }
        return SykmeldingMetadataInfo(
            ettersendingAv = erEttersending(sykmelding, tidligereSykmeldinger, loggingMetadata),
            forlengelseAv = erForlengelse(sykmelding, tidligereSykmeldinger, loggingMetadata),
        )
    }

    private fun erEttersending(sykmelding: Sykmelding, tidligereSykemldinger: List<SykmeldingDTO>, loggingMeta: LoggingMeta): String? {
        if (sykmelding.perioder.size > 1) {
            log.info("Flere perioder i periodelisten returnerer false {}", StructuredArguments.fields(loggingMeta))
            return null
        }
        if (sykmelding.medisinskVurdering.hovedDiagnose?.kode.isNullOrEmpty()) {
            log.info("Diagnosekode mangler {}", StructuredArguments.fields(loggingMeta))
            return null
        }
        val periode = sykmelding.perioder.first()
        val tidligereSykmelding = tidligereSykemldinger.firstOrNull { tidligereSykmelding ->
            tidligereSykmelding.sykmeldingsperioder.any { tidligerePeriode ->
                tidligerePeriode.fom == periode.fom &&
                    tidligerePeriode.tom == periode.tom &&
                    tidligerePeriode.gradert?.grad == periode.gradert?.grad &&
                    tidligerePeriode.type == periode.tilPeriodetypeDTO()
            }
        }
        if (tidligereSykmelding != null) {
            log.info("Sykmelding ${sykmelding.id} er ettersending av ${tidligereSykmelding.id} {}", StructuredArguments.fields(loggingMeta))
        }
        return tidligereSykmelding?.id
    }

    private fun erForlengelse(sykmelding: Sykmelding, sykmeldinger: List<SykmeldingDTO>, loggingMeta: LoggingMeta): List<Forlengelse> {
        val firstFom = sykmelding.perioder.sortedFOMDate().first()
        val tidligerePerioderFomTom = sykmeldinger
            .filter { it.medisinskVurdering?.hovedDiagnose?.kode == sykmelding.medisinskVurdering.hovedDiagnose?.kode }
            .filter { it.sykmeldingsperioder.size == 1 }
            .map { it.id to it.sykmeldingsperioder.first() }
            .filter { (_, periode) -> periode.type == PeriodetypeDTO.AKTIVITET_IKKE_MULIG || periode.type == PeriodetypeDTO.GRADERT }
            .map { (id, periode) -> Forlengelse(id, fom = periode.fom, tom = periode.tom) }

        val forlengelserAv = tidligerePerioderFomTom.filter { periode ->
            firstFom.isAfter(periode.fom.minusDays(1)) &&
                firstFom.isBefore(periode.tom.plusDays(17))
        }

        return forlengelserAv
    }

    private fun harTilbakedatertMerknad(sykmelding: SykmeldingDTO): Boolean {
        return sykmelding.merknader?.any {
            MerknadType.contains(it.type)
        } ?: false
    }
}
