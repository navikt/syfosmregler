package no.nav.syfo.services

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.RegelStatusDTO
import no.nav.syfo.client.SmregisterClient
import no.nav.syfo.client.periodelisteInneholderSammeType
import no.nav.syfo.client.sortedFOMDate
import no.nav.syfo.client.sortedTOMDate
import no.nav.syfo.log
import no.nav.syfo.model.Periode
import no.nav.syfo.rules.sortedFOMDate
import no.nav.syfo.rules.sortedTOMDate
import no.nav.syfo.utils.LoggingMeta

class SykmeldingService(private val smregisterClient: SmregisterClient) {
    suspend fun isOkTilbakedatert(fnr: String, periodeliste: List<Periode>, diagnosekode: String?, loggingMeta: LoggingMeta) : Boolean {
        log.info("Sjekker om finnes sykmeldinger med samme fom som ikke er tilbakedatert {}",
            StructuredArguments.fields(loggingMeta)
        )
        val forsteFomIMottattSykmelding = periodeliste.sortedFOMDate().firstOrNull() ?: return false
        val sisteTomIMottattSykmelding = periodeliste.sortedTOMDate().lastOrNull() ?: return false
        val mottattSykmeldingRange = forsteFomIMottattSykmelding.rangeTo(sisteTomIMottattSykmelding)
        try {
            val sykmeldinger = smregisterClient.hentSykmeldinger(fnr)
            sykmeldinger.filter {
                it.behandlingsutfall.status != RegelStatusDTO.INVALID && it.behandletTidspunkt.toLocalDate() <= forsteFomIMottattSykmelding.plusDays(
                    8
                )
            }.forEach {
                if (it.sykmeldingsperioder.sortedFOMDate().first() == forsteFomIMottattSykmelding) {
                    log.info("Fant sykmelding med samme fom som ikke er tilbakedatert {}",
                        StructuredArguments.fields(loggingMeta)
                    )
                    return true
                } else if (it.medisinskVurdering?.hovedDiagnose?.kode == diagnosekode &&
                    (
                            (
                                    it.sykmeldingsperioder.sortedFOMDate().first() in mottattSykmeldingRange &&
                                            it.sykmeldingsperioder.minByOrNull { it.fom }!!
                                                .periodelisteInneholderSammeType(periodeliste)
                                    ) ||
                                    (
                                            it.sykmeldingsperioder.sortedTOMDate().last() in mottattSykmeldingRange &&
                                                    it.sykmeldingsperioder.maxByOrNull { it.tom }!!
                                                        .periodelisteInneholderSammeType(periodeliste)
                                            )
                            )
                ) {
                    log.info(
                        "Fant sykmelding med delvis overlappende periode og samme diagnose og type som ikke er tilbakedatert, " +
                                "sykmeldingsID {} {}",
                        it.id,
                        StructuredArguments.fields(loggingMeta)
                    )
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            log.error("Feil ved henting av tidligere sykmeldinger {}", StructuredArguments.fields(loggingMeta))
            throw e
        }
    }
}