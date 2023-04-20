package no.nav.syfo.pdl.service

import com.github.benmanes.caffeine.cache.Caffeine
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.error.PersonNotFoundInPdl
import no.nav.syfo.pdl.model.PdlPerson
import no.nav.syfo.utils.LoggingMeta
import org.slf4j.LoggerFactory
import java.time.Duration

class PdlPersonService(
    private val pdlClient: PdlClient,
    private val accessTokenClientV2: AzureAdV2Client,
    private val pdlScope: String,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PdlPersonService::class.java)
    }

    private val cache = Caffeine
        .newBuilder().expireAfterWrite(Duration.ofMinutes(10))
        .maximumSize(500)
        .build<String, PdlPerson>()

    suspend fun getPdlPerson(fnr: String, loggingMeta: LoggingMeta): PdlPerson {
        val cachedPerson = cache.getIfPresent(fnr)
        if (cachedPerson != null) {
            log.info("Fant person i PDL cache")
            return cachedPerson
        }

        val token = accessTokenClientV2.getAccessToken(pdlScope)
        if (token?.accessToken == null) {
            throw RuntimeException("Klarte ikke hente accesstoken for PDL")
        }

        val pdlResponse = pdlClient.getPerson(fnr, token.accessToken)
        if (pdlResponse.errors != null) {
            pdlResponse.errors.forEach {
                log.error("PDL kastet error: {} ", it)
            }
        }
        if (pdlResponse.data.hentPerson == null) {
            log.error("Klarte ikke hente ut person fra PDL {}", StructuredArguments.fields(loggingMeta))
            throw PersonNotFoundInPdl("Klarte ikke hente ut person fra PDL")
        }
        if (pdlResponse.data.hentIdenter == null || pdlResponse.data.hentIdenter.identer.isNullOrEmpty()) {
            log.error("Fant ikke ident i PDL {}", StructuredArguments.fields(loggingMeta))
            throw PersonNotFoundInPdl("Fant ikke ident i PDL")
        }

        return putValue(fnr, PdlPerson(pdlResponse.data.hentIdenter.identer, foedsel = pdlResponse.data.hentPerson.foedsel))
    }

    private fun putValue(fnr: String, pdlPerson: PdlPerson): PdlPerson {
        cache.put(fnr, pdlPerson)
        return pdlPerson
    }
}
