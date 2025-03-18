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

data class SykmeldingInfo(
    val sykmeldingId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val gradert: Int?
)

data class StartdatoOgDager(val startDato: LocalDate, val dager: List<LocalDate>)

data class SykmeldingMetadataInfo(
    val ettersending: SykmeldingInfo?,
    val forlengelse: SykmeldingInfo?,
    val startdato: LocalDate,
    val dagerForArbeidsgiverperiodeCheck: List<LocalDate> = emptyList(),
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
        val startdatoOgDager = getStartDatoOgDager(sykmeldingerFromRegister, sykmelding)
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
            ettersending = erEttersending(sykmelding, tidligereSykmeldinger, loggingMetadata),
            forlengelse = erForlengelse(sykmelding, tidligereSykmeldinger).firstOrNull(),
            startdato = startdatoOgDager.startDato,
            dagerForArbeidsgiverperiodeCheck = startdatoOgDager.dager,
        )
    }

    fun getSykedagerForArbeidsgiverperiode(
        startDato: LocalDate,
        fom: LocalDate,
        tom: LocalDate,
        allDates: List<LocalDate>
    ): List<LocalDate> {
        val datoer = allDates.sortedDescending().filter { it < fom && it >= startDato }
        val antallSykdagerForArbeidsgiverPeriode = allDaysBetween(fom, tom).toMutableList()

        if (antallSykdagerForArbeidsgiverPeriode.size > 16) {
            return antallSykdagerForArbeidsgiverPeriode.subList(0, 17)
        }

        val dager =
            antallSykdagerForArbeidsgiverPeriode.toMutableList().sortedDescending().toMutableList()
        var lastDate = fom

        for (currentDate in datoer) {
            if (!isWorkingDaysBetween(lastDate, currentDate)) {
                dager.addAll(allDaysBetween(currentDate, lastDate.minusDays(1)))
            } else {
                dager.add(currentDate)
            }
            lastDate = currentDate
            if (dager.size > 16) {
                break
            }
        }
        return dager
    }

    private fun getStartDatoOgDager(
        sykmeldingerFromRegister: List<SykmeldingDTO>,
        sykmelding: Sykmelding
    ): StartdatoOgDager {
        var fom = sykmelding.perioder.sortedFOMDate().first()
        val tom = sykmelding.perioder.sortedTOMDate().last()
        val datoer = filterDates(fom, sykmeldingerFromRegister)
        var startdato = fom
        datoer.forEach {
            if (ChronoUnit.DAYS.between(it, startdato) > 16) {
                return StartdatoOgDager(
                    startdato,
                    getSykedagerForArbeidsgiverperiode(startdato, fom, tom, datoer)
                )
            } else {
                startdato = it
            }
        }
        return StartdatoOgDager(
            startdato,
            getSykedagerForArbeidsgiverperiode(startdato, fom, tom, datoer)
        )
    }

    private fun filterDates(
        startdato: LocalDate,
        sykmeldingerFromRegister: List<SykmeldingDTO>
    ): List<LocalDate> {
        return sykmeldingerFromRegister
            .filter {
                it.sykmeldingsperioder.sortedTOMDate().last() >
                    startdato.minusWeeks(40).minusDays(0)
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

    fun allDaysBetween(fom: LocalDate, tom: LocalDate): List<LocalDate> {
        return (0..ChronoUnit.DAYS.between(fom, tom)).map { fom.plusDays(it) }
    }

    private fun erEttersending(
        sykmelding: Sykmelding,
        sykmeldingerFraRegister: List<SykmeldingDTO>,
        loggingMeta: LoggingMeta
    ): SykmeldingInfo? {
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
        return tidligereSykmelding?.let {
            SykmeldingInfo(
                sykmeldingId = it.id,
                fom = it.sykmeldingsperioder.sortedFOMDate().first(),
                tom = it.sykmeldingsperioder.sortedTOMDate().last(),
                gradert = it.sykmeldingsperioder.first().gradert?.grad
            )
        }
    }

    private fun erForlengelse(
        sykmelding: Sykmelding,
        sykmeldinger: List<SykmeldingDTO>
    ): List<SykmeldingInfo> {
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
                .map { (id, periode) ->
                    SykmeldingInfo(
                        id,
                        fom = periode.fom,
                        tom = periode.tom,
                        gradert = periode.gradert?.grad
                    )
                }

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
