package no.nav.syfo.rules.juridiskvurdering

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import no.nav.syfo.rules.shared.ReceivedSykmelding
import no.nav.tsm.regulus.regula.RegulaJuridiskHenvisning
import no.nav.tsm.regulus.regula.RegulaLovverk
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.regulus.regula.TreeResult
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

data class JuridiskVurderingResult(
    val juridiskeVurderinger: List<JuridiskVurdering>,
)

class JuridiskVurderingService(
    private val kafkaProducer: KafkaProducer<String, JuridiskVurderingResult>,
    val juridiskVurderingTopic: String,
    val versjonsKode: String,
) {
    companion object {
        val EVENT_NAME = "subsumsjon"
        val VERSION = "1.0.0"
        val KILDE = "syfosmregler"
    }

    fun processRuleResults(
        receivedSykmelding: ReceivedSykmelding,
        result: RegulaResult,
    ) {
        val juridiskVurderingResult =
            JuridiskVurderingResult(
                juridiskeVurderinger =
                    result.results.mapNotNull {
                        when (val juridisk = it.juridisk) {
                            null -> null
                            else -> resultToJuridiskVurdering(receivedSykmelding, it, juridisk)
                        }
                    },
            )
        kafkaProducer
            .send(
                ProducerRecord(
                    juridiskVurderingTopic,
                    receivedSykmelding.sykmelding.id,
                    juridiskVurderingResult,
                ),
            )
            .get()
    }

    private fun resultToJuridiskVurdering(
        receivedSykmelding: ReceivedSykmelding,
        result: TreeResult,
        juridisk: RegulaJuridiskHenvisning,
    ): JuridiskVurdering {
        return JuridiskVurdering(
            id = UUID.randomUUID().toString(),
            eventName = EVENT_NAME,
            version = VERSION,
            kilde = KILDE,
            versjonAvKode = versjonsKode,
            fodselsnummer = receivedSykmelding.personNrPasient,
            juridiskHenvisning =
                JuridiskHenvisning(
                    lovverk =
                        when (juridisk.lovverk) {
                            RegulaLovverk.FOLKETRYGDLOVEN -> Lovverk.FOLKETRYGDLOVEN
                            else ->
                                throw IllegalArgumentException("Ukjent lovverk ${juridisk.lovverk}")
                        },
                    paragraf = juridisk.paragraf,
                    ledd = juridisk.ledd,
                    punktum = juridisk.punktum,
                    bokstav = juridisk.bokstav,
                ),
            sporing =
                mapOf(
                    "sykmelding" to receivedSykmelding.sykmelding.id,
                ),
            input = result.ruleInputs,
            utfall = toJuridiskUtfall(result.status),
            tidsstempel = ZonedDateTime.now(ZoneOffset.UTC),
        )
    }

    private fun toJuridiskUtfall(status: RegulaStatus) =
        when (status) {
            RegulaStatus.OK -> {
                JuridiskUtfall.VILKAR_OPPFYLT
            }
            RegulaStatus.INVALID -> {
                JuridiskUtfall.VILKAR_IKKE_OPPFYLT
            }
            RegulaStatus.MANUAL_PROCESSING -> {
                JuridiskUtfall.VILKAR_UAVKLART
            }
            else -> {
                JuridiskUtfall.VILKAR_UAVKLART
            }
        }
}
