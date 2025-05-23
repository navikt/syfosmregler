package no.nav.syfo.utils

data class Environment(
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "syfosmregler"),
    val legeSuspensjonProxyEndpointURL: String =
        getEnvVar("LEGE_SUSPENSJON_PROXY_ENDPOINT_URL", "http://btsys-api.team-rocket"),
    val legeSuspensjonProxyScope: String = getEnvVar("LEGE_SUSPENSJON_PROXY_SCOPE"),
    val syketilfelleEndpointURL: String =
        getEnvVar("SYKETILLFELLE_ENDPOINT_URL", "http://flex-syketilfelle.flex"),
    val port: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationThreads: Int = getEnvVar("APPLICATION_THREADS", "4").toInt(),
    val norskHelsenettEndpointURL: String = "http://syfohelsenettproxy",
    val aadAccessTokenV2Url: String = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
    val clientIdV2: String = getEnvVar("AZURE_APP_CLIENT_ID"),
    val clientSecretV2: String = getEnvVar("AZURE_APP_CLIENT_SECRET"),
    val helsenettproxyScope: String = getEnvVar("HELSENETT_SCOPE"),
    val smregisterEndpointURL: String = getEnvVar("SMREGISTER_URL", "http://syfosmregister"),
    val smregisterAudience: String = getEnvVar("SMREGISTER_AUDIENCE"),
    val pdlScope: String = getEnvVar("PDL_SCOPE"),
    val pdlGraphqlPath: String = getEnvVar("PDL_GRAPHQL_PATH"),
    val etterlevelsesTopic: String = "teamsykmelding.paragraf-i-kode",
    val jwtIssuer: String = getEnvVar("AZURE_OPENID_CONFIG_ISSUER"),
    val jwkKeysUrl: String = getEnvVar("AZURE_OPENID_CONFIG_JWKS_URI"),
    val sourceVersionURL: String = getEnvVar("SOURCE_VERSION_URL"),
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName)
        ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
