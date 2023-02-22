package no.nav.syfo.services

import no.nav.syfo.getEnvVar
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.Status
import no.nav.syfo.model.juridisk.JuridiskUtfall
import no.nav.syfo.model.juridisk.JuridiskVurdering
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.TreeOutput
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDateTime
import java.util.UUID

data class JuridiskVurderingResult(
    val juridiskeVurderinger: List<JuridiskVurdering>,
)

class JuridiskVurderingService(
    private val kafkaProducer: KafkaProducer<String, JuridiskVurderingResult>,
    val juridiskVurderingTopic: String,
    val versjonsKode: String = getEnvVar("NAIS_APP_IMAGE")
) {
    companion object {
        val EVENT_NAME = "subsumsjon"
        val VERSION = "1.0.0"
        val KILDE = "syfosmregler"
    }

    fun processRuleResults(
        receivedSykmelding: ReceivedSykmelding,
        result: List<TreeOutput<out Enum<*>, RuleResult>>
    ) {
        val juridiskVurderingResult = JuridiskVurderingResult(
            juridiskeVurderinger = result
                .filter { it.treeResult.ruleHit?.juridiskHenvisning != null }
                .map { resultToJuridiskVurdering(receivedSykmelding, it) }
        )
        kafkaProducer.send(
            ProducerRecord(
                juridiskVurderingTopic,
                receivedSykmelding.sykmelding.id,
                juridiskVurderingResult
            )
        ).get()
    }

    private fun resultToJuridiskVurdering(
        receivedSykmelding: ReceivedSykmelding,
        treeOutput: TreeOutput<out Enum<*>, RuleResult>
    ): JuridiskVurdering {
        return JuridiskVurdering(
            id = UUID.randomUUID().toString(),
            eventName = EVENT_NAME,
            version = VERSION,
            kilde = KILDE,
            versjonAvKode = versjonsKode,
            fodselsnummer = receivedSykmelding.personNrPasient,
            juridiskHenvisning = treeOutput.treeResult.ruleHit?.juridiskHenvisning
                ?: throw RuntimeException("JuridiskHenvisning kan ikke være null"),
            sporing = mapOf(
                "sykmelding" to receivedSykmelding.sykmelding.id,
            ),
            input = treeOutput.ruleInputs,
            utfall = toJuridiskUtfall(treeOutput.treeResult.status),
            tidsstempel = LocalDateTime.now()
        )
    }

    private fun toJuridiskUtfall(status: Status) = when (status) {
        Status.OK -> {
            JuridiskUtfall.VILKAR_OPPFYLT
        }
        Status.INVALID -> {
            JuridiskUtfall.VILKAR_IKKE_OPPFYLT
        }
        Status.MANUAL_PROCESSING -> {
            JuridiskUtfall.VILKAR_UAVKLART
        }
        else -> {
            JuridiskUtfall.VILKAR_UAVKLART
        }
    }
}
