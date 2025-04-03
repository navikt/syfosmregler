package no.nav.syfo.plugins

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.network.sockets.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.toProducerConfig
import no.nav.syfo.rules.RuleService
import no.nav.syfo.rules.juridiskvurdering.JuridiskVurderingService
import no.nav.syfo.rules.legesuspensjon.LegeSuspensjonClient
import no.nav.syfo.rules.nhn.NorskHelsenettClient
import no.nav.syfo.rules.pdl.PdlPersonService
import no.nav.syfo.rules.pdl.client.PdlClient
import no.nav.syfo.rules.registerRuleApi
import no.nav.syfo.rules.tidligeresykmeldinger.SmregisterClient
import no.nav.syfo.utils.Environment
import no.nav.syfo.utils.JacksonKafkaSerializer
import no.nav.syfo.utils.logger
import org.apache.kafka.clients.producer.KafkaProducer

@OptIn(DelicateCoroutinesApi::class)
fun Application.configureRouting() {
    val env = Environment()
    val httpClient = createHttpClient()
    val azureAdV2Client = AzureAdV2Client(env, httpClient)

    val legeSuspensjonClient =
        LegeSuspensjonClient(
            endpointUrl = env.legeSuspensjonProxyEndpointURL,
            azureAdV2Client = azureAdV2Client,
            httpClient = httpClient,
            scope = env.legeSuspensjonProxyScope,
        )

    val norskHelsenettClient =
        NorskHelsenettClient(
            env.norskHelsenettEndpointURL,
            azureAdV2Client,
            env.helsenettproxyScope,
            httpClient,
        )
    val smregisterClient =
        SmregisterClient(
            env.smregisterEndpointURL,
            azureAdV2Client,
            env.smregisterAudience,
            httpClient,
        )

    val pdlClient =
        PdlClient(
            httpClient,
            env.pdlGraphqlPath,
        )
    val pdlService =
        PdlPersonService(
            pdlClient,
            accessTokenClientV2 = azureAdV2Client,
            env.pdlScope,
        )

    val kafkaBaseConfig = KafkaUtils.getAivenKafkaConfig("juridisk-producer")
    val kafkaProperties =
        kafkaBaseConfig.toProducerConfig(
            env.applicationName,
            valueSerializer = JacksonKafkaSerializer::class,
        )

    val juridiskVurderingService =
        JuridiskVurderingService(
            KafkaProducer(kafkaProperties),
            env.etterlevelsesTopic,
            env.sourceVersionURL,
        )

    val ruleService =
        RuleService(
            legeSuspensjonClient,
            norskHelsenettClient,
            pdlService,
            juridiskVurderingService,
            smregisterClient,
        )

    routing { authenticate("servicebrukerAAD") { registerRuleApi(ruleService) } }
}

private fun createHttpClient(): HttpClient {
    val config: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, _ ->
                when (exception) {
                    is SocketTimeoutException ->
                        throw ServiceUnavailableException(exception.message)
                }
            }
        }
        install(HttpRequestRetry) {
            constantDelay(50, 0, false)
            retryOnExceptionIf(3) { request, throwable ->
                logger.warn("Caught exception ${throwable.message}, for url ${request.url}")
                true
            }
            retryIf(maxRetries) { request, response ->
                if (response.status.value.let { it in 500..599 }) {
                    logger.warn(
                        "Retrying for statuscode ${response.status.value}, for url ${request.url}",
                    )
                    true
                } else {
                    false
                }
            }
        }
        install(HttpTimeout) {
            socketTimeoutMillis = 20_000
            connectTimeoutMillis = 20_000
            requestTimeoutMillis = 20_000
        }
    }

    return HttpClient(Apache, config)
}

class ServiceUnavailableException(message: String?) : Exception(message)
