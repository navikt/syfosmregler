package no.nav.syfo.services

import io.kotest.core.spec.style.FunSpec

import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.Status
import no.nav.syfo.model.juridisk.JuridiskUtfall
import no.nav.syfo.model.juridisk.JuridiskVurdering
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRulesExecution
import no.nav.syfo.rules.tilbakedatering.toRuleMetadata
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDateTime
import java.util.UUID

class JuridiskVurderingServiceTest : FunSpec({

    val kafkaProducer = mockk<KafkaProducer<String, JuridiskVurderingResult>>(relaxed = true)
    val juridiskVurderingTopic = "topic"
    val juridiskVurderingService = JuridiskVurderingService(kafkaProducer, juridiskVurderingTopic, "versjon")


    context("Test juridiskVudering") {
        test("Returns juridiskVurderingResult is OK") {
            val receivedSykmelding = ReceivedSykmelding(
                sykmelding = generateSykmelding(),
                personNrPasient = "1231231",
                tlfPasient = "1323423424",
                personNrLege = "123134",
                legeHprNr = "123219",
                legeHelsepersonellkategori = "LE",
                navLogId = "4d3fad98-6c40-47ec-99b6-6ca7c98aa5ad",
                msgId = "06b2b55f-c2c5-4ee0-8e0a-6e252ec2a550",
                legekontorOrgNr = "444333",
                legekontorOrgName = "Helese sentar",
                legekontorHerId = "33",
                legekontorReshId = "1313",
                mottattDato = LocalDateTime.now(),
                rulesetVersion = "2",
                fellesformat = "",
                tssid = "13415",
                merknader = null,
                partnerreferanse = "16524",
                vedlegg = null,
                utenlandskSykmelding = null
            )

            val ruleMetadata = RuleMetadataSykmelding(receivedSykmelding.sykmelding.toRuleMetadata(),
                false, false)

            val result = TilbakedateringRulesExecution().runRules(sykmelding = receivedSykmelding.sykmelding,
                metadata = ruleMetadata)
            val results = listOf(result)
            juridiskVurderingService.processRuleResults(receivedSykmelding, results)
            val juridiskVurderingResult = JuridiskVurderingResult(
                listOf(
                    JuridiskVurdering(
                        id = UUID.randomUUID().toString(),
                        eventName = JuridiskVurderingService.EVENT_NAME,
                        version = JuridiskVurderingService.VERSION,
                        kilde = JuridiskVurderingService.KILDE,
                        versjonAvKode = "versjon",
                        fodselsnummer = receivedSykmelding.personNrPasient,
                        juridiskHenvisning = result.treeResult.ruleHit?.juridiskHenvisning
                            ?: throw RuntimeException("JuridiskHenvisning kan ikke være null"),
                        sporing = mapOf(
                            "sykmelding" to receivedSykmelding.sykmelding.id,
                        ),
                        input = result.ruleInputs,
                        utfall = toJuridiskUtfall(result.treeResult.status),
                        tidsstempel = LocalDateTime.now()
                    )
            ))
            verify { kafkaProducer.send(match<ProducerRecord<String, JuridiskVurderingResult>> {
                val firstResult = it.value().juridiskeVurderinger.first()
                firstResult.juridiskHenvisning == juridiskVurderingResult.juridiskeVurderinger.first().juridiskHenvisning &&
                firstResult.utfall == toJuridiskUtfall(Status.OK)
            }) }
        }
    }
})

fun toJuridiskUtfall(status: Status) = when (status) {
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