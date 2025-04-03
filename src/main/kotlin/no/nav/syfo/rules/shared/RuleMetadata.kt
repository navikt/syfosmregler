package no.nav.syfo.rules.shared

import java.time.LocalDate
import java.time.LocalDateTime

data class RuleMetadata(
    val signatureDate: LocalDateTime,
    val receivedDate: LocalDateTime,
    val behandletTidspunkt: LocalDateTime,
    val patientPersonNumber: String,
    val rulesetVersion: String?,
    val legekontorOrgnr: String?,
    val tssid: String?,
    val avsenderFnr: String,
    val pasientFodselsdato: LocalDate,
)
