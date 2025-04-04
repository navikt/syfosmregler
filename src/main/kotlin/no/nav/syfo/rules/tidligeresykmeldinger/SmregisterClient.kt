package no.nav.syfo.rules.tidligeresykmeldinger

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.utils.logger

class SmregisterClient(
    private val smregisterEndpointURL: String,
    private val accessTokenClientV2: AzureAdV2Client,
    private val scope: String,
    private val httpClient: HttpClient,
) {

    suspend fun getSykmeldinger(fnr: String): List<SmregisterSykmelding> {
        val result =
            httpClient.get("$smregisterEndpointURL/api/v2/sykmelding/sykmeldinger") {
                accept(ContentType.Application.Json)
                val accessToken = accessTokenClientV2.getAccessToken(scope)
                if (accessToken?.accessToken == null) {
                    throw RuntimeException("Klarte ikke hente ut accessToken for smregister")
                }
                headers {
                    append("Authorization", "Bearer ${accessToken.accessToken}")
                    append("fnr", fnr)
                }
            }
        when (result.status) {
            HttpStatusCode.OK -> {
                val sykmeldinger = result.body<List<SmregisterSykmelding>>()
                logger.info("Got ${sykmeldinger.size} from smregister")
                return sykmeldinger
            }
            else -> {
                logger.error("Could not get sykmeldinger from smregister")
                throw RuntimeException("Error getting sykmeldinger from smregister")
            }
        }
    }
}
