package no.nav.syfo

import java.nio.file.Path
import java.nio.file.Paths

val vaultApplicationPropertiesPath: Path = Paths.get("/var/run/secrets/nais.io/vault/credentials.json")

data class ApplicationConfig(
    val applicationPort: Int = 8080,
    val applicationThreads: Int = 1,

    val personV3EndpointURL: String,
    val securityTokenServiceUrl: String,
    val helsepersonellv1EndpointUrl: String,
    val legeSuspensjonEndpointUrl: String = "http://btsys",
    val stsRestEndpointUrl: String = "http://security-token-service",
    val syketilfelleEndpointUrl: String
)

data class VaultCredentials(
    val serviceuserUsername: String,
    val serviceuserPassword: String
)
