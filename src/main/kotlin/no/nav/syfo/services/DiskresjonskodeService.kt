package no.nav.syfo.services

import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.LoggingMeta
import no.nav.syfo.log
import no.nav.tjeneste.pip.diskresjonskode.DiskresjonskodePortType
import no.nav.tjeneste.pip.diskresjonskode.meldinger.WSHentDiskresjonskodeRequest

class DiskresjonskodeService(private val diskresjonskodePortType: DiskresjonskodePortType) {

    fun hentDiskresjonskode(ident: String, loggingMeta: LoggingMeta): String {
        log.info("Henter diskresjonskode {}", fields(loggingMeta))
        try {
            return diskresjonskodePortType.hentDiskresjonskode(WSHentDiskresjonskodeRequest().withIdent(ident)).diskresjonskode
        } catch (e: Exception) {
            log.error("Noe gikk galt ved henting av diskresjonskode {}", fields(loggingMeta))
            throw e
        }
    }
}
