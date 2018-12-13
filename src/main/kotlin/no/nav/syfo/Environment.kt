package no.nav.syfo

import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties

private val vaultApplicationPropertiesPath = Paths.get("/var/run/secrets/nais.io/vault/application.properties")

private val config = Properties().apply {
    putAll(Properties().apply {
        load(Environment::class.java.getResourceAsStream("/application.properties"))
    })
    if (Files.exists(vaultApplicationPropertiesPath)) {
        load(Files.newInputStream(vaultApplicationPropertiesPath))
    }
}

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

data class Environment(
    val applicationPort: Int = config.getProperty("application.port").toInt(),
    val applicationThreads: Int = config.getProperty("application.threads").toInt(),
    val srvsyfosmreglerUsername: String = config.getProperty("serviceuser.username"),
    val srvsyfosmreglerPassword: String = config.getProperty("serviceuser.password"),

    val personV3EndpointURL: String = getEnvVar("VIRKSOMHET_PERSON_V3_ENDPOINTURL"),
    val securityTokenServiceUrl: String = getEnvVar("SECURITYTOKENSERVICE_URL"),
    val helsepersonellv1EndpointUrl: String = getEnvVar("EKSTERN_HELSE_HELSEPERSONELL_V1_ENDPOINTURL")
)
