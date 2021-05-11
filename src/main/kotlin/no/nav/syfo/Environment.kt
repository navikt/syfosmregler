package no.nav.syfo

data class Environment(
    val legeSuspensjonEndpointURL: String = getEnvVar("LEGE_SUSPENSJON_ENDPOINT_URL", "http://btsys"),
    val syketilfelleEndpointURL: String = getEnvVar("SYKETILLFELLE_ENDPOINT_URL", "http://syfosyketilfelle.flex"),
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationThreads: Int = getEnvVar("APPLICATION_THREADS", "4").toInt(),
    val securityTokenServiceURL: String = getEnvVar("SECURITY_TOKEN_SERVICE_URL"),
    val norskHelsenettEndpointURL: String = getEnvVar("HELSENETT_ENDPOINT_URL", "http://syfohelsenettproxy"),
    val clientId: String = getEnvVar("CLIENT_ID"),
    val helsenettproxyId: String = getEnvVar("HELSENETTPROXY_ID"),
    val aadAccessTokenUrl: String = getEnvVar("AADACCESSTOKEN_URL"),
    val pdlGraphqlPath: String = getEnvVar("PDL_GRAPHQL_PATH")
)

data class VaultCredentials(
    val serviceuserUsername: String,
    val serviceuserPassword: String,
    val clientsecret: String
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
