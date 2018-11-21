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

data class Environment(
    val applicationPort: Int = config.getProperty("application.port").toInt(),
    val applicationThreads: Int = config.getProperty("application.threads").toInt(),
    val personV3EndpointURL: String = config.getProperty("ws.personV3.endpoint.url"),
    val securityTokenServiceUrl: String = config.getProperty("ws.security.token.service.endpoint.url"),
    val srvsyfosmreglerUsername: String = config.getProperty("serviceuser.username"),
    val srvsyfosmreglerPassword: String = config.getProperty("serviceuser.password")
)
