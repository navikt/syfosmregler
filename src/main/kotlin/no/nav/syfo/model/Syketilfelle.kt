package no.nav.syfo.model

import java.time.LocalDateTime

data class Syketilfelle(
    val aktorId: String,
    val orgnummer: String? = null,
    val inntruffet: LocalDateTime,
    val tags: String,
    val ressursId: String,
    val fom: LocalDateTime,
    val tom: LocalDateTime
)
