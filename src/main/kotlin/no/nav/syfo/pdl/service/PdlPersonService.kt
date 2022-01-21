package no.nav.syfo.pdl.service

import no.nav.syfo.client.AccessTokenClientV2
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.error.AktoerNotFoundException
import no.nav.syfo.pdl.error.PersonNotFoundInPdl
import no.nav.syfo.pdl.model.PdlPerson
import org.slf4j.LoggerFactory

class PdlPersonService(
    private val pdlClient: PdlClient,
    private val accessTokenClientV2: AccessTokenClientV2,
    private val pdlScope: String
) {
    companion object {
        private val log = LoggerFactory.getLogger(PdlPersonService::class.java)
    }

    suspend fun getPdlPerson(fnr: String, callId: String): PdlPerson {
        val token = accessTokenClientV2.getAccessTokenV2(pdlScope)
        val pdlResponse = pdlClient.getPerson(fnr, token)

        if (pdlResponse.errors != null) {
            pdlResponse.errors.forEach {
                log.error("PDL kastet error: {} ", it)
            }
        }
        if (pdlResponse.data.hentPerson == null) {
            log.warn("Klarte ikke hente ut person fra PDL {}", callId)
            throw PersonNotFoundInPdl("Klarte ikke hente ut person fra PDL")
        }
        if (pdlResponse.data.hentIdenter == null || pdlResponse.data.hentIdenter.identer.isNullOrEmpty()) {
            log.warn("Fant ikke aktørid i PDL {}", callId)
            throw AktoerNotFoundException("Fant ikke aktørId i PDL")
        }
        return PdlPerson(pdlResponse.data.hentIdenter.identer, foedsel = pdlResponse.data.hentPerson.foedsel)
    }

}
