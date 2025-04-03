package no.nav.syfo.rules.pdl

import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.metrics.PDL_HISTOGRAM
import no.nav.syfo.rules.pdl.client.PdlClient
import no.nav.syfo.rules.pdl.error.PersonNotFoundInPdl
import no.nav.syfo.utils.LoggingMeta
import org.slf4j.LoggerFactory

class PdlPersonService(
    private val pdlClient: PdlClient,
    private val accessTokenClientV2: AzureAdV2Client,
    private val pdlScope: String,
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    private val cache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
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

        val timer = PDL_HISTOGRAM.labels("get_person").startTimer()
        val pdlResponse = pdlClient.getPerson(fnr, token.accessToken)
        timer.observeDuration()
        if (pdlResponse.errors != null) {
            pdlResponse.errors.forEach { log.error("PDL kastet error: {} ", it) }
        }
        if (pdlResponse.data.hentPerson == null) {
            log.error(
                "Klarte ikke hente ut person fra PDL {}",
                StructuredArguments.fields(loggingMeta)
            )
            throw PersonNotFoundInPdl("Klarte ikke hente ut person fra PDL")
        }
        if (
            pdlResponse.data.hentIdenter == null ||
                pdlResponse.data.hentIdenter.identer.isNullOrEmpty()
        ) {
            log.error("Fant ikke ident i PDL {}", StructuredArguments.fields(loggingMeta))
            throw PersonNotFoundInPdl("Fant ikke ident i PDL")
        }

        return putValue(
            fnr,
            PdlPerson(
                pdlResponse.data.hentIdenter.identer,
                foedselsdato = pdlResponse.data.hentPerson.foedselsdato
            )
        )
    }

    private fun putValue(fnr: String, pdlPerson: PdlPerson): PdlPerson {
        cache.put(fnr, pdlPerson)
        return pdlPerson
    }
}
