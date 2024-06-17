package no.nav.syfo

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.network.sockets.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.prometheus.client.hotspot.DefaultExports
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.DelicateCoroutinesApi
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.client.LegeSuspensjonClient
import no.nav.syfo.client.NorskHelsenettClient
import no.nav.syfo.client.SmregisterClient
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.toProducerConfig
import no.nav.syfo.metrics.monitorHttpRequests
import no.nav.syfo.nais.isalive.naisIsAliveRoute
import no.nav.syfo.nais.isready.naisIsReadyRoute
import no.nav.syfo.nais.prometheus.naisPrometheusRoute
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.rules.api.registerRuleApi
import no.nav.syfo.services.JuridiskVurderingService
import no.nav.syfo.services.RuleExecutionService
import no.nav.syfo.services.RuleService
import no.nav.syfo.services.SykmeldingService
import no.nav.syfo.utils.JacksonKafkaSerializer
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("no.nav.syfo.syfosmregler")
val secureLog: Logger = LoggerFactory.getLogger("securelog")
val objectMapper: ObjectMapper =
    ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

fun main() {
    val embeddedServer =
        embeddedServer(
            Netty,
            port = EnvironmentVariables().applicationPort,
            module = Application::module,
        )
    Runtime.getRuntime()
        .addShutdownHook(
            Thread {
                embeddedServer.stop(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10))
            },
        )
    embeddedServer.start(true)
}

@DelicateCoroutinesApi
fun Application.configureRouting(
    ruleService: RuleService,
    env: EnvironmentVariables,
    applicationState: ApplicationState,
    jwkProviderAad: JwkProvider,
) {

    setupAuth(
        environmentVariables = env,
        jwkProviderAadV2 = jwkProviderAad,
    )
    routing {
        naisIsAliveRoute(applicationState)
        naisIsReadyRoute(applicationState)
        naisPrometheusRoute()
        authenticate("servicebrukerAAD") { registerRuleApi(ruleService) }
    }
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
            logger.error("Caught exception while trying to validate against rules", cause)
            throw cause
        }
    }
    intercept(ApplicationCallPipeline.Monitoring, monitorHttpRequests())
}

@OptIn(DelicateCoroutinesApi::class)
fun Application.module() {

    val environmentVariables = EnvironmentVariables()
    val applicationState = ApplicationState()

    val jwkProviderAad =
        JwkProviderBuilder(URI.create(environmentVariables.jwkKeysUrl).toURL())
            .cached(10, Duration.ofHours(24))
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

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
                        "Retrying for statuscode ${response.status.value}, for url ${request.url}"
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

    val httpClient = HttpClient(Apache, config)
    val azureAdV2Client = AzureAdV2Client(environmentVariables, httpClient)

    val legeSuspensjonClient =
        LegeSuspensjonClient(
            endpointUrl = environmentVariables.legeSuspensjonProxyEndpointURL,
            azureAdV2Client = azureAdV2Client,
            httpClient = httpClient,
            scope = environmentVariables.legeSuspensjonProxyScope,
        )

    val norskHelsenettClient =
        NorskHelsenettClient(
            environmentVariables.norskHelsenettEndpointURL,
            azureAdV2Client,
            environmentVariables.helsenettproxyScope,
            httpClient
        )
    val smregisterClient =
        SmregisterClient(
            environmentVariables.smregisterEndpointURL,
            azureAdV2Client,
            environmentVariables.smregisterAudience,
            httpClient
        )

    val pdlClient =
        PdlClient(
            httpClient,
            environmentVariables.pdlGraphqlPath,
            PdlClient::class
                .java
                .getResource("/graphql/getPerson.graphql")!!
                .readText()
                .replace(Regex("[\n\t]"), ""),
        )
    val pdlService =
        PdlPersonService(
            pdlClient,
            accessTokenClientV2 = azureAdV2Client,
            environmentVariables.pdlScope
        )

    val kafkaBaseConfig = KafkaUtils.getAivenKafkaConfig("juridisk-producer")
    val kafkaProperties =
        kafkaBaseConfig.toProducerConfig(
            environmentVariables.applicationName,
            valueSerializer = JacksonKafkaSerializer::class,
        )

    val juridiskVurderingService =
        JuridiskVurderingService(
            KafkaProducer(kafkaProperties),
            environmentVariables.etterlevelsesTopic,
            environmentVariables.sourceVersionURL
        )
    val ruleService =
        RuleService(
            legeSuspensjonClient,
            norskHelsenettClient,
            SykmeldingService(smregisterClient),
            pdlService,
            juridiskVurderingService,
            RuleExecutionService(),
        )

    configureRouting(
        ruleService = ruleService,
        env = environmentVariables,
        applicationState = applicationState,
        jwkProviderAad = jwkProviderAad
    )

    DefaultExports.initialize()
}

fun Application.setupAuth(
    environmentVariables: EnvironmentVariables,
    jwkProviderAadV2: JwkProvider,
) {
    install(Authentication) {
        jwt(name = "servicebrukerAAD") {
            verifier(jwkProviderAadV2, environmentVariables.jwtIssuer)
            validate { credentials ->
                when {
                    harTilgang(credentials, environmentVariables.clientIdV2) ->
                        JWTPrincipal(credentials.payload)
                    else -> unauthorized(credentials)
                }
            }
        }
    }
}

fun harTilgang(credentials: JWTCredential, clientId: String): Boolean {
    val appid: String = credentials.payload.getClaim("azp").asString()
    logger.debug("authorization attempt for $appid")
    return credentials.payload.audience.contains(clientId)
}

fun unauthorized(credentials: JWTCredential): Principal? {
    logger.warn(
        "Auth: Unexpected audience for jwt {}, {}",
        StructuredArguments.keyValue("issuer", credentials.payload.issuer),
        StructuredArguments.keyValue("audience", credentials.payload.audience),
    )
    return null
}

class ServiceUnavailableException(message: String?) : Exception(message)

data class ApplicationState(
    var alive: Boolean = true,
    var ready: Boolean = true,
)
