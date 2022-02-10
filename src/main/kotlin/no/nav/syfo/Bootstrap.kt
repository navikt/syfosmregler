package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.features.HttpResponseValidator
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.network.sockets.SocketTimeoutException
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.DelicateCoroutinesApi
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.application.exception.ServiceUnavailableException
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.client.LegeSuspensjonClient
import no.nav.syfo.client.NorskHelsenettClient
import no.nav.syfo.client.SmregisterClient
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.client.SyketilfelleClient
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.toProducerConfig
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.services.JuridiskVurderingService
import no.nav.syfo.services.RuleService
import no.nav.syfo.sm.Diagnosekoder
import no.nav.syfo.utils.JacksonKafkaSerializer
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ProxySelector
import java.net.URL
import java.util.concurrent.TimeUnit

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smregler")

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

@DelicateCoroutinesApi
fun main() {
    val env = Environment()
    val credentials = VaultCredentials()
    if (Diagnosekoder.icd10.isEmpty() || Diagnosekoder.icpc2.isEmpty()) {
        throw RuntimeException("ICD10 or ICPC2 diagnose codes failed to load.")
    }

    val applicationState = ApplicationState()
    DefaultExports.initialize()

    val jwkProviderAad = JwkProviderBuilder(URL(env.jwkKeysUrl))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    val config: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        HttpResponseValidator {
            handleResponseException { exception ->
                when (exception) {
                    is SocketTimeoutException -> throw ServiceUnavailableException(exception.message)
                }
            }
        }
    }

    val proxyConfig: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
        config()
        engine {
            customizeClient {
                setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
            }
        }
    }

    val httpClientWithProxy = HttpClient(Apache, proxyConfig)
    val httpClient = HttpClient(Apache, config)

    val oidcClient =
        StsOidcClient(credentials.serviceuserUsername, credentials.serviceuserPassword, env.securityTokenServiceURL)
    val legeSuspensjonClient = LegeSuspensjonClient(env.legeSuspensjonEndpointURL, credentials, oidcClient, httpClient)
    val azureAdV2Client = AzureAdV2Client(env, httpClientWithProxy)
    val syketilfelleClient = SyketilfelleClient(env.syketilfelleEndpointURL, azureAdV2Client, env.syketilfelleScope, httpClient)
    val norskHelsenettClient =
        NorskHelsenettClient(env.norskHelsenettEndpointURL, azureAdV2Client, env.helsenettproxyScope, httpClient)
    val smregisterClient = SmregisterClient(env.smregisterEndpointURL, azureAdV2Client, env.smregisterScope, httpClient)

    val pdlClient = PdlClient(
        httpClient, env.pdlGraphqlPath,
        PdlClient::class.java.getResource("/graphql/getPerson.graphql").readText().replace(Regex("[\n\t]"), "")
    )
    val pdlService = PdlPersonService(pdlClient, accessTokenClientV2 = azureAdV2Client, env.pdlScope)

    val kafkaBaseConfig = KafkaUtils.getAivenKafkaConfig()
    val kafkaProperties = kafkaBaseConfig.toProducerConfig(
        env.applicationName,
        valueSerializer = JacksonKafkaSerializer::class
    )

    val juridiskVurderingService = JuridiskVurderingService(
        KafkaProducer(kafkaProperties),
        env.etterlevelsesTopic
    )
    val ruleService = RuleService(
        legeSuspensjonClient,
        syketilfelleClient,
        norskHelsenettClient,
        smregisterClient,
        pdlService,
        juridiskVurderingService,
    )

    val applicationEngine = createApplicationEngine(
        ruleService,
        env,
        applicationState,
        jwkProviderAad
    )

    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()

    applicationState.ready = true
}
