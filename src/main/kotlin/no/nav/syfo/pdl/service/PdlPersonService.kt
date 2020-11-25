package no.nav.syfo.pdl.service

import io.ktor.util.KtorExperimentalAPI
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.LoggingMeta
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.log
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.model.PdlPerson
import no.nav.syfo.pdl.model.getDiskresjonskode

@KtorExperimentalAPI
class PdlPersonService(private val pdlClient: PdlClient, private val stsOidcClient: StsOidcClient) {

    suspend fun hentDiskresjonskode(ident: String, loggingMeta: LoggingMeta): String {
        log.info("Henter diskresjonskode fra PDL {}", fields(loggingMeta))
        return getPdlPerson(ident, loggingMeta).getDiskresjonskode() ?: ""
    }

    private suspend fun getPdlPerson(ident: String, loggingMeta: LoggingMeta): PdlPerson {
        val stsToken = stsOidcClient.oidcToken().access_token
        val pdlResponse = pdlClient.getPerson(ident, stsToken)

        if (pdlResponse.errors != null) {
            pdlResponse.errors.forEach {
                log.error("PDL returnerte error {}, {}", it, fields(loggingMeta))
            }
        }
        if (pdlResponse.data.hentPerson == null) {
            log.error("Fant ikke person i PDL {}", fields(loggingMeta))
            throw RuntimeException("Fant ikke person i PDL")
        }

        return PdlPerson(adressebeskyttelse = pdlResponse.data.hentPerson.adressebeskyttelse?.firstOrNull()?.gradering)
    }
}
