package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import java.net.ProxySelector
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import no.nav.syfo.api.AccessTokenClient
import no.nav.syfo.api.LegeSuspensjonClient
import no.nav.syfo.api.NorskHelsenettClient
import no.nav.syfo.api.SyketilfelleClient
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.api.registerRuleApi
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.pdl.PdlFactory
import no.nav.syfo.services.RuleService
import no.nav.syfo.sm.Diagnosekoder
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smregler")

data class ApplicationState(var ready: Boolean = false, var running: Boolean = true)

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

@KtorExperimentalAPI
fun main() {
    val env = Environment()
    val credentials = objectMapper.readValue<VaultCredentials>(Paths.get("/var/run/secrets/nais.io/vault/credentials.json").toFile())
    val applicationState = ApplicationState()

    if (Diagnosekoder.icd10.isEmpty() || Diagnosekoder.icpc2.isEmpty()) {
        throw RuntimeException("ICD10 or ICPC2 diagnose codes failed to load.")
    }

    DefaultExports.initialize()

    val config: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        expectSuccess = false
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

    val oidcClient = StsOidcClient(credentials.serviceuserUsername, credentials.serviceuserPassword)
    val legeSuspensjonClient = LegeSuspensjonClient(env.legeSuspensjonEndpointURL, credentials, oidcClient, httpClient)
    val syketilfelleClient = SyketilfelleClient(env.syketilfelleEndpointURL, oidcClient, httpClient)
    val accessTokenClient = AccessTokenClient(env.aadAccessTokenUrl, env.clientId, credentials.clientsecret, httpClientWithProxy)
    val norskHelsenettClient = NorskHelsenettClient(env.norskHelsenettEndpointURL, accessTokenClient, env.helsenettproxyId, httpClient)
    val pdlPersonService = PdlFactory.getPdlService(env, oidcClient, httpClient)

    val ruleService = RuleService(legeSuspensjonClient, syketilfelleClient, pdlPersonService, norskHelsenettClient)

    val applicationServer = embeddedServer(Netty, env.applicationPort) {
        initRouting(applicationState, ruleService)
        applicationState.ready = true
    }.start(wait = true)

    Runtime.getRuntime().addShutdownHook(Thread {
        applicationState.ready = false
        applicationServer.stop(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10))
    })
}

@KtorExperimentalAPI
fun Application.initRouting(applicationState: ApplicationState, ruleService: RuleService) {
    routing {
        registerNaisApi(readynessCheck = { applicationState.ready }, livenessCheck = { applicationState.running })
        registerRuleApi(ruleService)
    }
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
        }
    }
    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")

            log.error("Caught exception while trying to validate against rules", cause)
            throw cause
        }
    }
}
