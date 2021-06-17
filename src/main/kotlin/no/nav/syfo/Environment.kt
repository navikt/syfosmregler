package no.nav.syfo

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

data class Environment(
    val legeSuspensjonEndpointURL: String = getEnvVar("LEGE_SUSPENSJON_ENDPOINT_URL", "http://btsys.default"),
    val syketilfelleEndpointURL: String = getEnvVar("SYKETILLFELLE_ENDPOINT_URL", "http://syfosyketilfelle.flex"),
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationThreads: Int = getEnvVar("APPLICATION_THREADS", "4").toInt(),
    val securityTokenServiceURL: String = getEnvVar("SECURITY_TOKEN_SERVICE_URL", "http://security-token-service.default/rest/v1/sts/token"),
    val norskHelsenettEndpointURL: String = getEnvVar("HELSENETT_ENDPOINT_URL", "http://syfohelsenettproxy.default"),
    val clientId: String = getEnvVar("CLIENT_ID"),
    val helsenettproxyId: String = getEnvVar("HELSENETTPROXY_ID"),
    val aadAccessTokenUrl: String = getEnvVar("AADACCESSTOKEN_URL")
)

data class VaultCredentials(
    val serviceuserUsername: String = getFileAsString("/secrets/serviceuser/username"),
    val serviceuserPassword: String = getFileAsString("/secrets/serviceuser/password"),
    val clientsecret: String = getEnvVar("CLIENT_SECRET")
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

fun getFileAsString(filePath: String) = String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)
