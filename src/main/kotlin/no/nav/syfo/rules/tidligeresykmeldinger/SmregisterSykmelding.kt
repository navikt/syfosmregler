package no.nav.syfo.rules.tidligeresykmeldinger

import java.time.LocalDate
import java.time.OffsetDateTime

data class SmregisterSykmelding(
    val id: String,
    val sykmeldingStatus: SmregisterSykmeldingStatus,
    val behandlingsutfall: SmregisterBehandlingsutfall,
    val sykmeldingsperioder: List<SmregisterSykmeldingsperiode>,
    val behandletTidspunkt: OffsetDateTime,
    val medisinskVurdering: SmregisterMedisinskVurdering?,
    val merknader: List<SmregisterMerknad>?,
)

data class SmregisterMerknad(
    val type: String,
    val beskrivelse: String?,
)

data class SmregisterSykmeldingsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val gradert: SmregisterGradert?,
    val type: SmregisterPeriodetype,
)

data class SmregisterMedisinskVurdering(
    val hovedDiagnose: SmregisterDiagnose?,
)

data class SmregisterDiagnose(
    val kode: String,
)

data class SmregisterGradert(
    val grad: Int,
    val reisetilskudd: Boolean,
)

enum class SmregisterPeriodetype {
    AKTIVITET_IKKE_MULIG,
    AVVENTENDE,
    BEHANDLINGSDAGER,
    GRADERT,
    REISETILSKUDD,
}

data class SmregisterBehandlingsutfall(
    val status: SmregisterRegelStatus,
)

data class SmregisterSykmeldingStatus(
    val statusEvent: String,
    val timestamp: OffsetDateTime,
)

enum class SmregisterRegelStatus {
    OK,
    MANUAL_PROCESSING,
    INVALID
}
