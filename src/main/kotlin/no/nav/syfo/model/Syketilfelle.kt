package no.nav.syfo.model

import java.time.LocalDateTime

enum class SyketilfelleTag {
    SYKMELDING,
    PERIODE,
    FULL_AKTIVITET,
    INGEN_AKTIVITET,
    GRADERT_AKTIVITET,
    BEHANDLINGSDAGER
}

data class Syketilfelle(
    val aktorId: String,
    val orgnummer: String? = null,
    val inntruffet: LocalDateTime,
    val tags: String,
    val ressursId: String,
    val fom: LocalDateTime,
    val tom: LocalDateTime
)
