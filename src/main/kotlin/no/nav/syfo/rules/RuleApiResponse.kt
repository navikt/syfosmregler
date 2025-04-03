package no.nav.syfo.rules

data class ValidationResponse(
    val status: ValidationResponseStatus,
    val ruleHits: List<ValidationResponseRuleInfo>
)

data class ValidationResponseRuleInfo(
    val ruleName: String,
    val messageForSender: String,
    val messageForUser: String,
    val ruleStatus: ValidationResponseStatus
)

enum class ValidationResponseStatus {
    OK,
    MANUAL_PROCESSING,
    INVALID
}
