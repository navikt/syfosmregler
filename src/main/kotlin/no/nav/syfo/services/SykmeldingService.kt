package no.nav.syfo.services

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.MerknadType
import no.nav.syfo.client.PeriodetypeDTO
import no.nav.syfo.client.RegelStatusDTO
import no.nav.syfo.client.SmregisterClient
import no.nav.syfo.client.SykmeldingDTO
import no.nav.syfo.client.sortedFOMDate
import no.nav.syfo.client.sortedTOMDate
import no.nav.syfo.client.tilPeriodetypeDTO
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.utils.LoggingMeta
import org.slf4j.LoggerFactory

data class Forlengelse(val sykmeldingId: String, val fom: LocalDate, val tom: LocalDate)

data class SykmeldingMetadataInfo(
    val ettersendingAv: String?,
    val forlengelseAv: List<Forlengelse> = emptyList(),
    val agpStartdato: LocalDate
)

class SykmeldingService(private val syfosmregisterClient: SmregisterClient) {
    private val logger = LoggerFactory.getLogger(SykmeldingService::class.java)

    suspend fun getSykmeldingMetadataInfo(
        fnr: String,
        sykmelding: Sykmelding,
        loggingMetadata: LoggingMeta
    ): SykmeldingMetadataInfo {
        val sykmeldingerFromRegister = syfosmregisterClient.getSykmeldinger(fnr)
        logger.info(
            "getting sykmeldinger for ${sykmelding.id}: match from smregister ${sykmeldingerFromRegister.map { it.id }}",
        )
        val startdatoAgp = getStartDatoAgp(sykmeldingerFromRegister, sykmelding)
        val tidligereSykmeldinger =
            sykmeldingerFromRegister
                .filter { it.behandlingsutfall.status != RegelStatusDTO.INVALID }
                .filterNot { harTilbakedatertMerknad(it) }
                .filter { it.medisinskVurdering?.hovedDiagnose?.kode != null }
                .filter {
                    it.medisinskVurdering?.hovedDiagnose?.kode ==
                        sykmelding.medisinskVurdering.hovedDiagnose?.kode
                }
        logger.info(
            "tidligere sykmeldinger for ${sykmelding.id}, after filter ${tidligereSykmeldinger.map { it.id }}",
        )
        return SykmeldingMetadataInfo(
            ettersendingAv = erEttersending(sykmelding, tidligereSykmeldinger, loggingMetadata),
            forlengelseAv = erForlengelse(sykmelding, tidligereSykmeldinger),
            agpStartdato = startdatoAgp
        )
    }

    private fun getStartDatoAgp(
        sykmeldingerFromRegister: List<SykmeldingDTO>,
        sykmelding: Sykmelding
    ): LocalDate {
        var startdato = sykmelding.perioder.sortedFOMDate().first()
        val datoer = filterDates(startdato, sykmeldingerFromRegister)
        datoer.forEach {
            if (ChronoUnit.DAYS.between(it, startdato) > 16) {
                return startdato
            } else {
                startdato = it
            }
        }
        return startdato
    }

    private fun filterDates(
        startdato: LocalDate,
        sykmeldingerFromRegister: List<SykmeldingDTO>
    ): List<LocalDate> {
        return sykmeldingerFromRegister
            .filter {
                it.sykmeldingsperioder.sortedTOMDate().last() >
                    startdato.minusWeeks(12).minusDays(16)
            }
            .filter { it.sykmeldingsperioder.sortedFOMDate().first() < startdato }
            .filter { it.behandlingsutfall.status != RegelStatusDTO.INVALID }
            .filterNot {
                !it.merknader.isNullOrEmpty() &&
                    it.merknader.any { merknad ->
                        merknad.type == MerknadType.UGYLDIG_TILBAKEDATERING.toString() ||
                            merknad.type ==
                                MerknadType.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER.toString()
                    }
            }
            .filter { it.sykmeldingStatus.statusEvent != "AVBRUTT" }
            .map { it ->
                it.sykmeldingsperioder
                    .filter { it.type != PeriodetypeDTO.AVVENTENDE }
                    .flatMap { allDaysBetween(it.fom, it.tom) }
            }
            .flatten()
            .distinct()
            .sortedDescending()
    }

    private fun allDaysBetween(fom: LocalDate, tom: LocalDate): List<LocalDate> {
        return (0..ChronoUnit.DAYS.between(fom, tom)).map { fom.plusDays(it) }
    }

    private fun erEttersending(
        sykmelding: Sykmelding,
        sykmeldingerFraRegister: List<SykmeldingDTO>,
        loggingMeta: LoggingMeta
    ): String? {
        if (sykmelding.perioder.size > 1) {
            logger.info(
                "Flere perioder i periodelisten returnerer false {}",
                StructuredArguments.fields(loggingMeta),
            )
            return null
        }
        if (sykmelding.medisinskVurdering.hovedDiagnose?.kode.isNullOrEmpty()) {
            logger.info("Diagnosekode mangler {}", StructuredArguments.fields(loggingMeta))
            return null
        }
        val periode = sykmelding.perioder.first()

        val tidligereSykmelding =
            sykmeldingerFraRegister.firstOrNull { sykmeldingFraRegister ->
                sykmeldingFraRegister.sykmeldingsperioder.any { tidligerePeriode ->
                    tidligerePeriode.fom == periode.fom &&
                        tidligerePeriode.tom == periode.tom &&
                        tidligerePeriode.gradert?.grad == periode.gradert?.grad &&
                        tidligerePeriode.type == periode.tilPeriodetypeDTO()
                }
            }

        if (tidligereSykmelding != null) {
            logger.info(
                "Sykmelding ${sykmelding.id} er ettersending av ${tidligereSykmelding.id} {}",
                StructuredArguments.fields(loggingMeta),
            )
        } else {
            logger.info(
                "Could not find ettersending for ${sykmelding.id} from ${sykmeldingerFraRegister.map { it.id }}",
            )
        }
        return tidligereSykmelding?.id
    }

    private fun erForlengelse(
        sykmelding: Sykmelding,
        sykmeldinger: List<SykmeldingDTO>
    ): List<Forlengelse> {
        val firstFom = sykmelding.perioder.sortedFOMDate().first()
        val lastTom = sykmelding.perioder.sortedTOMDate().last()
        val tidligerePerioderFomTom =
            sykmeldinger
                .filter {
                    it.medisinskVurdering?.hovedDiagnose?.kode ==
                        sykmelding.medisinskVurdering.hovedDiagnose?.kode
                }
                .filter { it.sykmeldingsperioder.size == 1 }
                .map { it.id to it.sykmeldingsperioder.first() }
                .filter { (_, periode) ->
                    periode.type == PeriodetypeDTO.AKTIVITET_IKKE_MULIG ||
                        periode.type == PeriodetypeDTO.GRADERT
                }
                .map { (id, periode) -> Forlengelse(id, fom = periode.fom, tom = periode.tom) }

        val forlengelserAv =
            tidligerePerioderFomTom.filter { periode ->
                !isWorkingDaysBetween(firstFom, periode.tom) ||
                    isOverlappendeAndForlengelse(periode.tom, periode.fom, firstFom, lastTom)
            }

        return forlengelserAv
    }

    private fun isOverlappendeAndForlengelse(
        periodeTom: LocalDate,
        periodeFom: LocalDate,
        firstFom: LocalDate,
        lastTom: LocalDate
    ) =
        (firstFom.isAfter(periodeFom.minusDays(1)) &&
            firstFom.isBefore(periodeTom.plusDays(1)) &&
            lastTom.isAfter(periodeTom.minusDays(1)))

    private fun isWorkingDaysBetween(firstFom: LocalDate, periodeTom: LocalDate): Boolean {
        val daysBetween = ChronoUnit.DAYS.between(periodeTom, firstFom).toInt()
        if (daysBetween < 0) return true
        return when (firstFom.dayOfWeek) {
            DayOfWeek.MONDAY -> daysBetween > 3
            DayOfWeek.SUNDAY -> daysBetween > 2
            else -> daysBetween > 1
        }
    }

    private fun harTilbakedatertMerknad(sykmelding: SykmeldingDTO): Boolean {
        return sykmelding.merknader?.any { MerknadType.contains(it.type) } ?: false
    }
}
