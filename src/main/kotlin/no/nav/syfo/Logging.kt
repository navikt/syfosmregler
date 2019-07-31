package no.nav.syfo

import no.nav.syfo.model.ReceivedSykmelding

data class LogMeta(
    val mottakId: String,
    val orgNr: String?,
    val msgId: String,
    val sykmeldingId: String
)

fun ReceivedSykmelding.extractLogMeta() = LogMeta(
        mottakId = navLogId,
        orgNr = legekontorOrgNr,
        msgId = msgId,
        sykmeldingId = sykmelding.id
)
