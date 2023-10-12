import java.time.LocalDateTime
import no.nav.syfo.client.Godkjenning
import no.nav.syfo.client.Tilleggskompetanse
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.RuleResult
import no.nav.syfo.rules.hpr.HPRRules
import no.nav.syfo.rules.hpr.HelsepersonellKategori
import no.nav.syfo.services.BehandlerOgStartdato
import no.nav.syfo.services.daysBetween
import no.nav.syfo.services.sortedFOMDate
import no.nav.syfo.services.sortedTOMDate

typealias Rule<T> =
    (sykmelding: Sykmelding, behandlerOgStartdato: BehandlerOgStartdato) -> RuleResult<T>

typealias HPRRule = Rule<HPRRules>

fun behanderGyldigHPR(rule: HPRRules): HPRRule = { _, behandlerOgStartdato ->
    val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger

    val aktivAutorisasjon =
        behandlerGodkjenninger.any { (it.autorisasjon?.aktiv != null && it.autorisasjon.aktiv) }

    RuleResult(
        ruleInputs = mapOf("behandlerGodkjenninger" to behandlerGodkjenninger),
        rule = rule,
        ruleResult = aktivAutorisasjon,
    )
}

fun behandlerHarAutorisasjon(rule: HPRRules): HPRRule = { _, behandlerOgStartdato ->
    val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger

    val gyldigeGodkjenninger =
        behandlerGodkjenninger.any {
            (it.autorisasjon?.aktiv != null &&
                it.autorisasjon.aktiv &&
                it.autorisasjon.oid == 7704 &&
                it.autorisasjon.verdi != null &&
                it.autorisasjon.verdi in arrayOf("1", "17", "4", "2", "14", "18"))
        }

    RuleResult(
        ruleInputs = mapOf("behandlerGodkjenninger" to behandlerGodkjenninger),
        rule = rule,
        ruleResult = gyldigeGodkjenninger,
    )
}

fun behandlerErLege(rule: HPRRules): HPRRule = { _, behandlerOgStartdato ->
    val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger

    val behandlerErLege = sjekkBehandler(behandlerGodkjenninger, HelsepersonellKategori.LEGE)

    RuleResult(
        ruleInputs = mapOf("behandlerGodkjenninger" to behandlerGodkjenninger),
        rule = rule,
        ruleResult = behandlerErLege,
    )
}

fun behandlerErTannlege(rule: HPRRules): HPRRule = { _, behandlerOgStartdato ->
    val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger

    val behandlerErTannlege =
        sjekkBehandler(behandlerGodkjenninger, HelsepersonellKategori.TANNLEGE)

    RuleResult(
        ruleInputs = mapOf("behandlerGodkjenninger" to behandlerGodkjenninger),
        rule = rule,
        ruleResult = behandlerErTannlege,
    )
}

fun behandlerErManuellterapeut(rule: HPRRules): HPRRule = { _, behandlerOgStartdato ->
    val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger

    val behandlerErManuellterapeut =
        sjekkBehandler(behandlerGodkjenninger, HelsepersonellKategori.MANUELLTERAPEUT)

    RuleResult(
        ruleInputs = mapOf("behandlerGodkjenninger" to behandlerGodkjenninger),
        rule = rule,
        ruleResult = behandlerErManuellterapeut,
    )
}

fun behandlerErFTMedTilligskompetanseSykmelding(rule: HPRRules): HPRRule =
    { metadata, behandlerOgStartdato ->
        val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger
        val genereringsTidspunkt = metadata.signaturDato

        val erFtMedTilleggskompetanse =
            erHelsepersonellKategoriMedTilleggskompetanse(
                behandlerGodkjenninger,
                genereringsTidspunkt,
                HelsepersonellKategori.FYSIOTERAPAEUT
            )

        val result = erFtMedTilleggskompetanse

        RuleResult(
            ruleInputs = mapOf("behandlerGodkjenninger" to behandlerGodkjenninger),
            rule = rule,
            ruleResult = result,
        )
    }

fun behandlerErKIMedTilligskompetanseSykmelding(rule: HPRRules): HPRRule =
    { metadata, behandlerOgStartdato ->
        val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger
        val genereringsTidspunkt = metadata.signaturDato

        val erKIMedTilleggskompetanse =
            erHelsepersonellKategoriMedTilleggskompetanse(
                behandlerGodkjenninger,
                genereringsTidspunkt,
                HelsepersonellKategori.KIROPRAKTOR
            )

        val result = erKIMedTilleggskompetanse

        RuleResult(
            ruleInputs = mapOf("behandlerGodkjenninger" to behandlerGodkjenninger),
            rule = rule,
            ruleResult = result,
        )
    }

fun sykefravarOver12Uker(rule: HPRRules): HPRRule = { sykmelding, behandlerOgStartdato ->
    val forsteFomDato = sykmelding.perioder.sortedFOMDate().first()
    val sisteTomDato = sykmelding.perioder.sortedTOMDate().last()
    val behandlerStartDato = behandlerOgStartdato.startdato
    val behandlerGodkjenninger = behandlerOgStartdato.behandler.godkjenninger

    val over12Uker =
        (forsteFomDato..sisteTomDato).daysBetween() > 84 ||
            (behandlerStartDato != null && (behandlerStartDato..sisteTomDato).daysBetween() > 84)

    RuleResult(
        ruleInputs =
            mapOf(
                "fom" to forsteFomDato,
                "tom" to sisteTomDato,
                "startDatoSykefravær" to (behandlerStartDato ?: forsteFomDato),
                "behandlerGodkjenninger" to behandlerGodkjenninger,
            ),
        rule = rule,
        ruleResult = over12Uker,
    )
}

private fun erHelsepersonellKategoriMedTilleggskompetanse(
    behandlerGodkjenninger: List<Godkjenning>,
    genereringsTidspunkt: LocalDateTime,
    helsepersonellkategori: HelsepersonellKategori
) =
    (behandlerGodkjenninger.size == 1 &&
        behandlerGodkjenninger.any { godkjenning ->
            godkjenning.helsepersonellkategori?.verdi == helsepersonellkategori.verdi &&
                godkjenning.tillegskompetanse?.any { tillegskompetanse ->
                    tillegskompetanse.avsluttetStatus == null &&
                        tillegskompetanse.gyldigPeriode(genereringsTidspunkt) &&
                        tillegskompetanse.type?.aktiv == true &&
                        tillegskompetanse.type.oid == 7702 &&
                        tillegskompetanse.type.verdi == "1"
                }
                    ?: false
        })

private fun sjekkBehandler(
    behandlerGodkjenninger: List<Godkjenning>,
    helsepersonellkategori: HelsepersonellKategori
) =
    behandlerGodkjenninger.any {
        (it.helsepersonellkategori?.aktiv != null &&
            it.autorisasjon?.aktiv == true &&
            harAktivHelsepersonellAutorisasjonsSom(
                behandlerGodkjenninger,
                helsepersonellkategori.verdi
            ))
    }

private fun harAktivHelsepersonellAutorisasjonsSom(
    behandlerGodkjenninger: List<Godkjenning>,
    helsepersonerVerdi: String,
): Boolean =
    behandlerGodkjenninger.any { godkjenning ->
        godkjenning.helsepersonellkategori?.aktiv != null &&
            godkjenning.autorisasjon?.aktiv == true &&
            godkjenning.helsepersonellkategori.verdi != null &&
            godkjenning.helsepersonellkategori.let { it.aktiv && it.verdi == helsepersonerVerdi }
    }

private fun Tilleggskompetanse.gyldigPeriode(genereringsTidspunkt: LocalDateTime): Boolean {
    val fom = gyldig?.fra?.toLocalDate()
    val tom = gyldig?.til?.toLocalDate()
    val genDate = genereringsTidspunkt.toLocalDate()
    if (fom == null) {
        return false
    }
    return fom.minusDays(1).isBefore(genDate) && (tom == null || tom.plusDays(1).isAfter(genDate))
}
