package no.nav.syfo.rules.tilbakedatering

import java.time.temporal.ChronoUnit
import no.nav.helse.diagnosekoder.Diagnosekoder
import no.nav.syfo.metrics.TILBAKEDATERING_RULE_DAYS_COUNTER
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.ARBEIDSGIVERPERIODE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.BEGRUNNELSE_MIN_1_ORD
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.BEGRUNNELSE_MIN_3_ORD
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.ETTERSENDING
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.FORLENGELSE
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.SPESIALISTHELSETJENESTEN
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERING
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_INNTIL_4_DAGER
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_INNTIL_8_DAGER
import no.nav.syfo.rules.tilbakedatering.TilbakedateringRules.TILBAKEDATERT_MINDRE_ENN_1_MAANED
import no.nav.syfo.services.RuleMetadataSykmelding
import no.nav.syfo.services.sortedFOMDate
import no.nav.syfo.services.sortedTOMDate

typealias Rule<T> = (sykmelding: Sykmelding, metadata: RuleMetadataSykmelding) -> RuleResult<T>

typealias TilbakedateringRule = Rule<TilbakedateringRules>

val tilbakedatering: TilbakedateringRule = { sykmelding, _ ->
    val fom = sykmelding.perioder.sortedFOMDate().first()
    val genereringstidspunkt = sykmelding.signaturDato.toLocalDate()

    RuleResult(
        ruleInputs = mapOf("fom" to fom, "genereringstidspunkt" to genereringstidspunkt),
        rule = TILBAKEDATERING,
        ruleResult = genereringstidspunkt.isAfter(fom),
    )
}

val tilbakedateringInntil4Dager: TilbakedateringRule = { sykmelding, _ ->
    val fom = sykmelding.perioder.sortedFOMDate().first()
    val genereringstidspunkt = sykmelding.signaturDato.toLocalDate()
    val daysBetween = ChronoUnit.DAYS.between(fom, genereringstidspunkt)
    TILBAKEDATERING_RULE_DAYS_COUNTER.labels("digital", daysBetween.toString()).inc()
    RuleResult(
        ruleInputs = mapOf("fom" to fom, "genereringstidspunkt" to genereringstidspunkt),
        rule = TILBAKEDATERT_INNTIL_4_DAGER,
        ruleResult = daysBetween <= 4,
    )
}

val tilbakedateringMindreEnn1Maaned: TilbakedateringRule = { sykmelding, _ ->
    val fom = sykmelding.perioder.sortedFOMDate().first()
    val genereringstidspunkt = sykmelding.signaturDato.toLocalDate()
    RuleResult(
        ruleInputs = mapOf("fom" to fom, "genereringstidspunkt" to genereringstidspunkt),
        rule = TILBAKEDATERT_MINDRE_ENN_1_MAANED,
        ruleResult = genereringstidspunkt.isBefore(fom.plusMonths(1).plusDays(1)),
    )
}

val tilbakedateringInntil8Dager: TilbakedateringRule = { sykmelding, _ ->
    val fom = sykmelding.perioder.sortedFOMDate().first()
    val genereringstidspunkt = sykmelding.signaturDato.toLocalDate()
    RuleResult(
        ruleInputs = mapOf("fom" to fom, "genereringstidspunkt" to genereringstidspunkt),
        rule = TILBAKEDATERT_INNTIL_8_DAGER,
        ruleResult = genereringstidspunkt.isBefore(fom.plusDays(9)),
    )
}

val arbeidsgiverperiode: TilbakedateringRule = { sykmelding, metadata ->
    val startDato =
        metadata.behandlerOgStartdato.startdato ?: sykmelding.perioder.sortedFOMDate().first()
    val tom = sykmelding.perioder.sortedTOMDate().last()

    val dager = metadata.sykmeldingMetadataInfo.dagerForArbeidsgiverperiodeCheck
    val arbeidsgiverperiodeNy = dager.size < 17

    RuleResult(
        ruleInputs =
            mapOf(
                "syketilfelletStartdato" to startDato,
                "fom" to sykmelding.perioder.sortedFOMDate().first(),
                "tom" to tom,
                "dagerForArbeidsgiverperiode" to dager.sorted(),
                "arbeidsgiverperiode" to arbeidsgiverperiodeNy,
            ),
        rule = ARBEIDSGIVERPERIODE,
        ruleResult = arbeidsgiverperiodeNy,
    )
}

val begrunnelse_min_1_ord: TilbakedateringRule = { sykmelding, _ ->
    val begrunnelse = sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt ?: ""
    val wordCount = getNumberOfWords(begrunnelse)
    val result = wordCount >= 1
    RuleResult(
        ruleInputs = mapOf("begrunnelse" to "$wordCount ord"),
        rule = BEGRUNNELSE_MIN_1_ORD,
        ruleResult = result,
    )
}

val begrunnelse_min_3_ord: TilbakedateringRule = { sykmelding, _ ->
    val begrunnelse = sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt ?: ""
    val wordCount = getNumberOfWords(begrunnelse)
    val result = wordCount >= 3
    RuleResult(
        ruleInputs = mapOf("begrunnelse" to "$wordCount ord"),
        rule = BEGRUNNELSE_MIN_3_ORD,
        ruleResult = result,
    )
}

val ettersending: TilbakedateringRule = { _, metadata ->
    val ettersendingAv = metadata.sykmeldingMetadataInfo.ettersending
    val result = ettersendingAv != null
    val ruleInputs = mutableMapOf<String, Any>()
    if (ettersendingAv != null) {
        ruleInputs["ettersending"] = ettersendingAv
    }
    RuleResult(
        ruleInputs = ruleInputs,
        rule = ETTERSENDING,
        ruleResult = result,
    )
}

val forlengelse: TilbakedateringRule = { _, metadata ->
    val forlengelse = metadata.sykmeldingMetadataInfo.forlengelse
    val result = forlengelse != null
    val ruleInputs = mutableMapOf<String, Any>()

    if (forlengelse != null) {
        ruleInputs["forlengelse"] = forlengelse
    }
    RuleResult(
        ruleInputs = ruleInputs,
        rule = FORLENGELSE,
        ruleResult = result,
    )
}

val spesialisthelsetjenesten: TilbakedateringRule = { sykmelding, _ ->
    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose
    val spesialhelsetjenesten = hoveddiagnose?.isICD10() ?: false
    RuleResult(
        ruleInputs =
            mapOf(
                "diagnosesystem" to (hoveddiagnose?.system ?: ""),
                "spesialisthelsetjenesten" to spesialhelsetjenesten,
            ),
        rule = SPESIALISTHELSETJENESTEN,
        ruleResult = spesialhelsetjenesten,
    )
}

fun getNumberOfWords(input: String?): Int {
    return input?.trim()?.split(" ")?.filter { containsLetters(it) }?.size ?: 0
}

fun containsLetters(text: String): Boolean {
    return text.contains("""[A-Za-z]""".toRegex())
}

fun Diagnose.isICD10(): Boolean = system == Diagnosekoder.ICD10_CODE
