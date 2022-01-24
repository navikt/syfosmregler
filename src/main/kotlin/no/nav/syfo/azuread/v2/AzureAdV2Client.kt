package no.nav.syfo.azuread.v2

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.ResponseException
import io.ktor.client.features.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import no.nav.syfo.Environment
import org.slf4j.LoggerFactory

class AzureAdV2Client(
    environment: Environment,
    private val httpClient: HttpClient,
    private val azureAdV2Cache: AzureAdV2Cache = AzureAdV2Cache()
) {
    private val azureAppClientId = environment.clientIdV2
    private val azureAppClientSecret = environment.clientSecretV2
    private val azureTokenEndpoint = environment.aadAccessTokenV2Url

    /**
     * Returns a non-obo access token authenticated using app specific client credentials
     */
    suspend fun getAccessToken(
        scope: String
    ): AzureAdV2Token? {
        return azureAdV2Cache.getAccessToken(scope)
            ?: getClientSecretAccessToken(scope)?.let {
                azureAdV2Cache.putValue(scope, it)
            }
    }

    private suspend fun getClientSecretAccessToken(
        scope: String
    ): AzureAdV2Token? {
        return getAccessToken(
            Parameters.build {
                append("client_id", azureAppClientId)
                append("client_secret", azureAppClientSecret)
                append("scope", scope)
                append("grant_type", "client_credentials")
            }
        )?.toAzureAdV2Token()
    }

    private suspend fun getAccessToken(
        formParameters: Parameters
    ): AzureAdV2TokenResponse? {
        return try {
            val response: HttpResponse = httpClient.post(azureTokenEndpoint) {
                accept(ContentType.Application.Json)
                body = FormDataContent(formParameters)
            }
            response.receive<AzureAdV2TokenResponse>()
        } catch (e: ClientRequestException) {
            handleUnexpectedResponseException(e)
        } catch (e: ServerResponseException) {
            handleUnexpectedResponseException(e)
        }
    }

    private fun handleUnexpectedResponseException(
        responseException: ResponseException
    ): AzureAdV2TokenResponse? {
        log.error(
            "Error while requesting AzureAdAccessToken with statusCode=${responseException.response.status.value}",
            responseException
        )
        return null
    }

    companion object {
        private val log = LoggerFactory.getLogger(AzureAdV2Client::class.java)
    }
}
