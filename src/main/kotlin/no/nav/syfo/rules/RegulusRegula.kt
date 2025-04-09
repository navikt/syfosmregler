package no.nav.syfo.rules

import no.nav.syfo.rules.nhn.Behandler
import no.nav.syfo.rules.nhn.Godkjenning
import no.nav.syfo.rules.shared.Periode
import no.nav.syfo.rules.shared.RuleMetadata
import no.nav.syfo.rules.shared.SporsmalSvar
import no.nav.syfo.rules.shared.Sykmelding
import no.nav.syfo.rules.tidligeresykmeldinger.SmregisterPeriodetype
import no.nav.syfo.rules.tidligeresykmeldinger.SmregisterRegelStatus
import no.nav.syfo.rules.tidligeresykmeldinger.SmregisterSykmelding
import no.nav.syfo.rules.tidligeresykmeldinger.SmregisterSykmeldingsperiode
import no.nav.tsm.regulus.regula.RegulaAvsender
import no.nav.tsm.regulus.regula.RegulaBehandler
import no.nav.tsm.regulus.regula.RegulaMeta
import no.nav.tsm.regulus.regula.RegulaPasient
import no.nav.tsm.regulus.regula.RegulaPayload
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.regulus.regula.payload.Aktivitet
import no.nav.tsm.regulus.regula.payload.AnnenFravarsArsak
import no.nav.tsm.regulus.regula.payload.BehandlerGodkjenning
import no.nav.tsm.regulus.regula.payload.BehandlerKode
import no.nav.tsm.regulus.regula.payload.BehandlerPeriode
import no.nav.tsm.regulus.regula.payload.BehandlerTilleggskompetanse
import no.nav.tsm.regulus.regula.payload.Diagnose
import no.nav.tsm.regulus.regula.payload.RelevanteMerknader
import no.nav.tsm.regulus.regula.payload.TidligereSykmelding
import no.nav.tsm.regulus.regula.payload.TidligereSykmeldingAktivitet
import no.nav.tsm.regulus.regula.payload.TidligereSykmeldingMeta
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.rules.RegulusRegula")

fun mapToRegulaPayload(
    sykmelding: Sykmelding,
    tidligereSykmeldinger: List<SmregisterSykmelding>,
    ruleMetadata: RuleMetadata,
    pasientIdent: String,
    behandler: Behandler?,
    behandlerSuspendert: Boolean,
): RegulaPayload {
    try {
        val mappedTidligereSykmeldinger =
            tidligereSykmeldinger.map {
                TidligereSykmelding(
                    sykmeldingId = it.id,
                    aktivitet =
                        it.sykmeldingsperioder.map(
                            SmregisterSykmeldingsperiode::toSykmeldingPeriode,
                        ),
                    hoveddiagnose =
                        it.medisinskVurdering?.hovedDiagnose?.let { diagnose ->
                            Diagnose(
                                kode = diagnose.kode,
                                system = "TODO: System kommer ikke fra registeret? :huh:",
                            )
                        },
                    meta =
                        TidligereSykmeldingMeta(
                            status = it.behandlingsutfall.status.toRegulaStatus(),
                            userAction = it.sykmeldingStatus.statusEvent,
                            merknader =
                                it.merknader
                                    ?.map { merknad -> merknad.type.toRegulaMerknad() }
                                    ?.filterNotNull(),
                        ),
                )
            }

        return RegulaPayload(
            sykmeldingId = sykmelding.id,
            hoveddiagnose =
                sykmelding.medisinskVurdering.hovedDiagnose?.let {
                    Diagnose(kode = it.kode, system = it.system)
                },
            bidiagnoser =
                sykmelding.medisinskVurdering.biDiagnoser.map {
                    Diagnose(kode = it.kode, system = it.system)
                },
            annenFravarsArsak =
                sykmelding.medisinskVurdering.annenFraversArsak?.let { annenFraversArsak ->
                    AnnenFravarsArsak(
                        beskrivelse = annenFraversArsak.beskrivelse,
                        grunn = annenFraversArsak.grunn.map { it.name },
                    )
                },
            aktivitet = sykmelding.perioder.map(Periode::toSykmeldingPeriode),
            utdypendeOpplysninger = mapSvar(sykmelding.utdypendeOpplysninger),
            kontaktPasientBegrunnelseIkkeKontakt =
                sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
            tidligereSykmeldinger = mappedTidligereSykmeldinger,
            behandletTidspunkt = ruleMetadata.behandletTidspunkt,
            pasient =
                RegulaPasient(
                    ident = pasientIdent,
                    fodselsdato = ruleMetadata.pasientFodselsdato,
                ),
            meta =
                RegulaMeta.LegacyMeta(
                    mottattDato = ruleMetadata.receivedDate,
                    signaturdato = sykmelding.signaturDato,
                    rulesetVersion = ruleMetadata.rulesetVersion,
                ),
            behandler =
                if (behandler == null)
                    RegulaBehandler.FinnesIkke(
                        fnr = sykmelding.behandler.fnr,
                    )
                else
                    RegulaBehandler.Finnes(
                        suspendert = behandlerSuspendert,
                        fnr = sykmelding.behandler.fnr,
                        legekontorOrgnr = ruleMetadata.legekontorOrgnr,
                        godkjenninger =
                            behandler.godkjenninger.map(Godkjenning::toBehandlerGodkjenning),
                    ),
            avsender = RegulaAvsender.Finnes(ruleMetadata.avsenderFnr),
        )
    } catch (e: Exception) {
        log.error("Regulus Regula rule execution failed", e)
        throw e
    }
}

private fun String.toRegulaMerknad() =
    when (this) {
        "UGYLDIG_TILBAKEDATERING" -> RelevanteMerknader.UGYLDIG_TILBAKEDATERING
        "TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER" ->
            RelevanteMerknader.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER
        "UNDER_BEHANDLING" -> RelevanteMerknader.UNDER_BEHANDLING
        else -> null
    }

private fun SmregisterRegelStatus.toRegulaStatus(): RegulaStatus =
    when (this) {
        SmregisterRegelStatus.OK -> RegulaStatus.OK
        SmregisterRegelStatus.INVALID -> RegulaStatus.INVALID
        SmregisterRegelStatus.MANUAL_PROCESSING -> RegulaStatus.MANUAL_PROCESSING
    }

private fun Periode.toSykmeldingPeriode(): Aktivitet =
    when {
        aktivitetIkkeMulig != null ->
            Aktivitet.IkkeMulig(
                fom = fom,
                tom = tom,
            )
        gradert != null ->
            Aktivitet.Gradert(
                fom = fom,
                tom = tom,
                grad = gradert.grad,
            )
        reisetilskudd ->
            Aktivitet.Reisetilskudd(
                fom = fom,
                tom = tom,
            )
        behandlingsdager != null ->
            Aktivitet.Behandlingsdager(
                fom = fom,
                tom = tom,
                behandlingsdager = behandlingsdager,
            )
        avventendeInnspillTilArbeidsgiver != null ->
            Aktivitet.Avventende(
                fom = fom,
                tom = tom,
                avventendeInnspillTilArbeidsgiver = avventendeInnspillTilArbeidsgiver,
            )
        else ->
            Aktivitet.Ugyldig(
                fom = fom,
                tom = tom,
            )
    }

private fun SmregisterSykmeldingsperiode.toSykmeldingPeriode(): TidligereSykmeldingAktivitet =
    when {
        type == SmregisterPeriodetype.AKTIVITET_IKKE_MULIG ->
            TidligereSykmeldingAktivitet.IkkeMulig(
                fom = fom,
                tom = tom,
            )
        type == SmregisterPeriodetype.GRADERT && gradert != null ->
            TidligereSykmeldingAktivitet.Gradert(
                fom = fom,
                tom = tom,
                grad = gradert.grad,
            )
        type == SmregisterPeriodetype.REISETILSKUDD ->
            TidligereSykmeldingAktivitet.Reisetilskudd(
                fom = fom,
                tom = tom,
            )
        type == SmregisterPeriodetype.BEHANDLINGSDAGER ->
            TidligereSykmeldingAktivitet.Behandlingsdager(
                fom = fom,
                tom = tom,
            )
        type == SmregisterPeriodetype.AVVENTENDE ->
            TidligereSykmeldingAktivitet.Avventende(
                fom = fom,
                tom = tom,
            )
        else -> {
            log.warn("Shadow test: Ukjent periode type: $type")
            TidligereSykmeldingAktivitet.Ugyldig(
                fom = fom,
                tom = tom,
            )
        }
    }

private fun Godkjenning.toBehandlerGodkjenning() =
    BehandlerGodkjenning(
        helsepersonellkategori =
            helsepersonellkategori?.let {
                BehandlerKode(
                    oid = it.oid,
                    aktiv = it.aktiv,
                    verdi = it.verdi,
                )
            },
        tillegskompetanse =
            tillegskompetanse?.map { tillegskompetanse ->
                BehandlerTilleggskompetanse(
                    avsluttetStatus =
                        tillegskompetanse.avsluttetStatus?.let {
                            BehandlerKode(
                                oid = it.oid,
                                aktiv = it.aktiv,
                                verdi = it.verdi,
                            )
                        },
                    gyldig =
                        tillegskompetanse.gyldig?.let {
                            BehandlerPeriode(fra = it.fra, til = it.til)
                        },
                    type =
                        tillegskompetanse.type?.let {
                            BehandlerKode(
                                oid = it.oid,
                                aktiv = it.aktiv,
                                verdi = it.verdi,
                            )
                        },
                )
            },
        autorisasjon =
            autorisasjon?.let {
                BehandlerKode(
                    oid = it.oid,
                    aktiv = it.aktiv,
                    verdi = it.verdi,
                )
            },
    )

private fun mapSvar(
    input: Map<String, Map<String, SporsmalSvar>>
): Map<String, Map<String, Map<String, String>>> {
    return input.mapValues { (_, innerMap) ->
        innerMap.mapValues { (_, svar) ->
            mapOf(
                "sporsmal" to svar.sporsmal,
                "svar" to svar.svar,
                "restriksjoner" to svar.restriksjoner.joinToString(",") { it.name },
            )
        }
    }
}
