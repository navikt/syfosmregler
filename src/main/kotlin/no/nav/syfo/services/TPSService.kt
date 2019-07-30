package no.nav.syfo.services

import com.ctc.wstx.exc.WstxException
import java.io.IOException
import no.nav.syfo.Environment
import no.nav.syfo.VaultCredentials
import no.nav.syfo.helpers.retry
import no.nav.syfo.ws.createPort
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest

class TPSService(env: Environment, credentials: VaultCredentials) {
    private val personV3: PersonV3 = createPort(env.personV3EndpointURL) {
        port { withSTS(credentials.serviceuserUsername, credentials.serviceuserPassword, env.securityTokenServiceURL) }
    }

    suspend fun fetchPerson(ident: String): no.nav.tjeneste.virksomhet.person.v3.informasjon.Person = retry(
            callName = "tps_hent_person",
            retryIntervals = arrayOf(500L, 1000L, 3000L, 5000L, 10000L, 60000L),
            legalExceptions = *arrayOf(IOException::class, WstxException::class)
    ) {
        personV3.hentPerson(HentPersonRequest()
                .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(ident)))
        ).person
    }
}
