package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.rules.containsLetters
import no.nav.syfo.rules.dsl.Rule
import no.nav.syfo.rules.dsl.RuleExecution
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.rules.sortedFOMDate
import no.nav.syfo.rules.sortedTOMDate
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.ARBEIDSGIVERPERIODE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.BEGRUNNELSE_MIN_1_ORD
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.BEGRUNNELSE_MIN_3_ORD
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.ETTERSENDING
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.FORLENGELSE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.SPESIALISTHELSETJENESTEN
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERING
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_INNTIL_30_DAGER
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_INNTIL_8_DAGER
import no.nav.syfo.sm.isICD10
import java.time.temporal.ChronoUnit

typealias TilbakedateringRule = Rule<TilbakedateringRules>

val tilbakedatering: TilbakedateringRule = { sykmelding, _ ->
    val fom = sykmelding.perioder.sortedFOMDate().first()
    val behandletTidspunkt = sykmelding.behandletTidspunkt.toLocalDate()

    RuleResult(
        ruleInputs = mapOf("fom" to fom, "behandletTidspunkt" to behandletTidspunkt),
        ruleResult = RuleExecution(TILBAKEDATERING, (behandletTidspunkt.isAfter(fom.plusDays(3))))
    )
}

val tilbakedateringInntil31Dager: TilbakedateringRule = { sykmelding, _ ->
    val fom = sykmelding.perioder.sortedFOMDate().first()
    val behandletTidspunkt = sykmelding.behandletTidspunkt.toLocalDate()
    RuleResult(
        ruleInputs = mapOf("fom" to fom, "behandletTidspunkt" to behandletTidspunkt),
        ruleResult = RuleExecution(TILBAKEDATERT_INNTIL_30_DAGER, (behandletTidspunkt.isBefore(fom.plusDays(31))))
    )
}

val tilbakedateringInntil8Dager: TilbakedateringRule = { sykmelding, _ ->
    val fom = sykmelding.perioder.sortedFOMDate().first()
    val behandletTidspunkt = sykmelding.behandletTidspunkt.toLocalDate()
    RuleResult(
        ruleInputs = mapOf("fom" to fom, "behandletTidspunkt" to behandletTidspunkt),
        ruleResult = RuleExecution(TILBAKEDATERT_INNTIL_8_DAGER, (behandletTidspunkt.isBefore(fom.plusDays(9))))
    )
}

val arbeidsgiverperiode: TilbakedateringRule = { sykmelding, _ ->
    val fom = sykmelding.perioder.sortedFOMDate().first()
    val tom = sykmelding.perioder.sortedTOMDate().last()
    val arbeidsgiverperiode = ChronoUnit.DAYS.between(fom, tom) < 16
    RuleResult(
        ruleInputs = mapOf(
            "fom" to fom,
            "tom" to tom,
            "arbeidsgiverperiode" to arbeidsgiverperiode
        ),
        ruleResult = RuleExecution(ARBEIDSGIVERPERIODE, arbeidsgiverperiode)
    )
}

val begrunnelse_min_1_ord: TilbakedateringRule = { sykmelding, _ ->
    val begrunnelse = sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt ?: ""
    val wordCount = getNumberOfWords(begrunnelse)
    val result = wordCount >= 1
    RuleResult(
        ruleInputs = mapOf("begrunnelse" to begrunnelse),
        ruleResult = RuleExecution(BEGRUNNELSE_MIN_1_ORD, result)
    )
}

val begrunnelse_min_3_ord: TilbakedateringRule = { sykmelding, _ ->
    val begrunnelse = sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt ?: ""
    val wordCount = getNumberOfWords(begrunnelse)
    val result = wordCount >= 3
    RuleResult(
        ruleInputs = mapOf("begrunnelse" to begrunnelse),
        ruleResult = RuleExecution(BEGRUNNELSE_MIN_3_ORD, result)
    )
}

val ettersending: TilbakedateringRule = { _, metadata ->
    val result = metadata.erEttersendingAvTidligereSykmelding ?: false
    RuleResult(
        ruleInputs = mutableMapOf("ettersending" to result),
        ruleResult = RuleExecution(ETTERSENDING, result)
    )
}

val forlengelse: TilbakedateringRule = { _, metadata ->
    val forlengelse = !metadata.erNyttSyketilfelle
    RuleResult(
        ruleInputs = mapOf("forlengelse" to forlengelse),
        ruleResult = RuleExecution(FORLENGELSE, (forlengelse))
    )
}

val spesialisthelsetjenesten: TilbakedateringRule = { sykmelding, _ ->
    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose
    val spesialhelsetjenesten = hoveddiagnose?.isICD10() ?: false
    RuleResult(
        ruleInputs = mapOf(
            "hoveddiagnose" to (hoveddiagnose ?: ""),
            "spesialisthelsetjenesten" to spesialhelsetjenesten
        ),
        ruleResult = RuleExecution(SPESIALISTHELSETJENESTEN, spesialhelsetjenesten)
    )
}

private fun getNumberOfWords(input: String): Int {
    return input.trim().split(" ").filter { containsLetters(it) }.size
}
