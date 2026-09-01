package no.nav.syfo.rules.juridiskvurdering

import java.util.*
import no.nav.syfo.rules.shared.ReceivedSykmelding
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.juridisk.JuridiskVurdering
import no.nav.tsm.regulus.regula.toJuridiskVurdering
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

data class JuridiskVurderingResult(val juridiskeVurderinger: List<JuridiskVurdering>)

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

    fun processRuleResults(receivedSykmelding: ReceivedSykmelding, result: RegulaResult) {

        val juridiskVurderingResult =
            JuridiskVurderingResult(
                result.juridisk.map {
                    it.toJuridiskVurdering(
                        id = UUID.randomUUID().toString(),
                        eventName = EVENT_NAME,
                        version = VERSION,
                        kilde = KILDE,
                        versjonAvKode = versjonsKode,
                        sporing = mapOf("sykmelding" to receivedSykmelding.sykmelding.id),
                    )
                }
            )

        kafkaProducer
            .send(
                ProducerRecord(
                    juridiskVurderingTopic,
                    receivedSykmelding.sykmelding.id,
                    juridiskVurderingResult,
                )
            )
            .get()
    }
}
