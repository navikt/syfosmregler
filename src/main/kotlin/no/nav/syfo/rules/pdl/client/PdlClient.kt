package no.nav.syfo.rules.pdl.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import no.nav.syfo.rules.pdl.client.model.GetPersonRequest
import no.nav.syfo.rules.pdl.client.model.GetPersonVeriables
import no.nav.syfo.rules.pdl.client.model.GraphQLResponse
import no.nav.syfo.rules.pdl.client.model.PdlResponse

class PdlClient(
    private val httpClient: HttpClient,
    private val basePath: String,
) {
    private val temaHeader = "TEMA"
    private val tema = "SYM"

    suspend fun getPerson(fnr: String, token: String): GraphQLResponse<PdlResponse> {
        val getPersonRequest =
            GetPersonRequest(
                variables = GetPersonVeriables(ident = fnr),
            )

        return httpClient
            .post(basePath) {
                setBody(getPersonRequest)
                header(HttpHeaders.Authorization, "Bearer $token")
                header("Behandlingsnummer", "B229")
                header(temaHeader, tema)
                header(HttpHeaders.ContentType, "application/json")
            }
            .body()
    }
}
