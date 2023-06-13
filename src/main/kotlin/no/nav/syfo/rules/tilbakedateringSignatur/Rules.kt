package no.nav.syfo.rules.tilbakedateringSignatur

import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.ARBEIDSGIVERPERIODE
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.BEGRUNNELSE_MIN_1_ORD
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.BEGRUNNELSE_MIN_3_ORD
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.ETTERSENDING
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.FORLENGELSE
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.HOVEDDIAGNOSE_MANGLER_NULL
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.SPESIALISTHELSETJENESTEN
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.TILBAKEDATERING
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.TILBAKEDATERT_INNTIL_30_DAGER
import no.nav.syfo.rules.tilbakedateringSignatur.TilbakedateringSignaturRules.TILBAKEDATERT_INNTIL_8_DAGER
import no.nav.syfo.services.RuleMetadataSykmelding
import no.nav.syfo.services.sortedFOMDate
import no.nav.syfo.services.sortedTOMDate
import no.nav.syfo.sm.isICD10
import java.time.temporal.ChronoUnit

typealias Rule<T> = (sykmelding: Sykmelding, metadata: RuleMetadataSykmelding) -> RuleResult<T>
typealias TilbakedateringSignaturRule = Rule<TilbakedateringSignaturRules>

val tilbakedatering: TilbakedateringSignaturRule = { sykmelding, _ ->
    val fom = sykmelding.perioder.sortedFOMDate().first()
    val signaturDato = sykmelding.signaturDato.toLocalDate()

    RuleResult(
        ruleInputs = mapOf("fom" to fom, "signaturDato" to signaturDato),
        rule = TILBAKEDATERING,
        ruleResult = signaturDato.isAfter(fom.plusDays(3)),
    )
}

val tilbakedateringInntil30Dager: TilbakedateringSignaturRule = { sykmelding, _ ->
    val fom = sykmelding.perioder.sortedFOMDate().first()
    val signaturDato = sykmelding.signaturDato.toLocalDate()
    RuleResult(
        ruleInputs = mapOf("fom" to fom, "signaturDato" to signaturDato),
        rule = TILBAKEDATERT_INNTIL_30_DAGER,
        ruleResult = signaturDato.isBefore(fom.plusDays(31)),
    )
}

val tilbakedateringInntil8Dager: TilbakedateringSignaturRule = { sykmelding, _ ->
    val fom = sykmelding.perioder.sortedFOMDate().first()
    val signaturDato = sykmelding.signaturDato.toLocalDate()
    RuleResult(
        ruleInputs = mapOf("fom" to fom, "signaturDato" to signaturDato),
        rule = TILBAKEDATERT_INNTIL_8_DAGER,
        ruleResult = signaturDato.isBefore(fom.plusDays(9)),
    )
}

val arbeidsgiverperiode: TilbakedateringSignaturRule = { sykmelding, metadata ->
    val startDato = metadata.behandlerOgStartdato.startdato ?: sykmelding.perioder.sortedFOMDate().first()
    val tom = sykmelding.perioder.sortedTOMDate().last()
    val arbeidsgiverperiode = ChronoUnit.DAYS.between(startDato, tom) < 16
    RuleResult(
        ruleInputs = mapOf(
            "syketilfelletStartdato" to startDato,
            "fom" to sykmelding.perioder.sortedFOMDate().first(),
            "tom" to tom,
            "arbeidsgiverperiode" to arbeidsgiverperiode,
        ),
        rule = ARBEIDSGIVERPERIODE,
        ruleResult = arbeidsgiverperiode,
    )
}

val begrunnelse_min_1_ord: TilbakedateringSignaturRule = { sykmelding, _ ->
    val begrunnelse = sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt ?: ""
    val wordCount = getNumberOfWords(begrunnelse)
    val result = wordCount >= 1
    RuleResult(
        ruleInputs = mapOf("begrunnelse" to begrunnelse),
        rule = BEGRUNNELSE_MIN_1_ORD,
        ruleResult = result,
    )
}

val begrunnelse_min_3_ord: TilbakedateringSignaturRule = { sykmelding, _ ->
    val begrunnelse = sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt ?: ""
    val wordCount = getNumberOfWords(begrunnelse)
    val result = wordCount >= 3
    RuleResult(
        ruleInputs = mapOf("begrunnelse" to begrunnelse),
        rule = BEGRUNNELSE_MIN_3_ORD,
        ruleResult = result,
    )
}

val ettersending: TilbakedateringSignaturRule = { _, metadata ->
    val ettersendingAv = metadata.sykmeldingMetadataInfo.ettersendingAv
    val result = ettersendingAv != null
    val ruleInputs = mutableMapOf<String, Any>(
        "ettersending" to result,
    )
    if (ettersendingAv != null) {
        ruleInputs["ettersendingAv"] = ettersendingAv
    }
    RuleResult(
        ruleInputs = ruleInputs,
        rule = ETTERSENDING,
        ruleResult = result,
    )
}

val forlengelse: TilbakedateringSignaturRule = { _, metadata ->
    val forlengelse = metadata.sykmeldingMetadataInfo.forlengelseAv
    val result = forlengelse.isNotEmpty()
    val ruleInputs = mutableMapOf<String, Any>("forlengelse" to result)

    if (result) {
        ruleInputs["forlengelseAv"] = forlengelse
    }
    RuleResult(
        ruleInputs = ruleInputs,
        rule = FORLENGELSE,
        ruleResult = result,
    )
}

val spesialisthelsetjenesten: TilbakedateringSignaturRule = { sykmelding, _ ->
    val hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose
    val spesialhelsetjenesten = hoveddiagnose?.isICD10() ?: false
    RuleResult(
        ruleInputs = mapOf(
            "hoveddiagnose" to (hoveddiagnose ?: ""),
            "spesialisthelsetjenesten" to spesialhelsetjenesten,
        ),
        rule = SPESIALISTHELSETJENESTEN,
        ruleResult = spesialhelsetjenesten,
    )
}

val houvedDiagnoseMangler: TilbakedateringSignaturRule = { sykmelding, _ ->
    val houveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose

    RuleResult(
        ruleInputs = mapOf("hoveddiagnose" to (houveddiagnose ?: "")),
        rule = HOVEDDIAGNOSE_MANGLER_NULL,
        ruleResult = houveddiagnose == null,
    )
}

fun getNumberOfWords(input: String?): Int {
    return input?.trim()?.split(" ")?.filter { containsLetters(it) }?.size ?: 0
}
fun containsLetters(text: String): Boolean {
    return text.contains("""[A-Za-z]""".toRegex())
}
