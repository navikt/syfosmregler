import no.nav.syfo.client.Godkjenning
import no.nav.syfo.client.MerknadType
import no.nav.syfo.client.PeriodetypeDTO
import no.nav.syfo.client.RegelStatusDTO
import no.nav.syfo.client.SykmeldingDTO
import no.nav.syfo.client.SykmeldingsperiodeDTO
import no.nav.syfo.model.Periode
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.SporsmalSvar
import no.nav.syfo.model.ValidationResult
import no.nav.syfo.rules.common.Juridisk
import no.nav.syfo.rules.common.RuleResult
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.printRulePath
import no.nav.syfo.services.RuleMetadataSykmelding
import no.nav.tsm.regulus.regula.RegulaAvsender
import no.nav.tsm.regulus.regula.RegulaBehandler
import no.nav.tsm.regulus.regula.RegulaMeta
import no.nav.tsm.regulus.regula.RegulaPasient
import no.nav.tsm.regulus.regula.RegulaPayload
import no.nav.tsm.regulus.regula.executeRegulaRules
import no.nav.tsm.regulus.regula.payload.AnnenFravarsArsak
import no.nav.tsm.regulus.regula.payload.BehandlerGodkjenning
import no.nav.tsm.regulus.regula.payload.BehandlerKode
import no.nav.tsm.regulus.regula.payload.BehandlerPeriode
import no.nav.tsm.regulus.regula.payload.BehandlerTilleggskompetanse
import no.nav.tsm.regulus.regula.payload.Diagnose
import no.nav.tsm.regulus.regula.payload.SykmeldingPeriode
import no.nav.tsm.regulus.regula.payload.TidligereSykmelding
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("regula-smoke-test")

fun regulaShadowTest(
    receivedSykmelding: ReceivedSykmelding,
    ruleMetadataSykmelding: RuleMetadataSykmelding,
    tidligereSykmeldinger: List<SykmeldingDTO>,
    oldResult: List<Pair<TreeOutput<out Enum<*>, RuleResult>, Juridisk>>,
    oldValidationResult: ValidationResult,
) {
    try {
        val oldSykmelding = receivedSykmelding.sykmelding
        val rulePayload =
            RegulaPayload(
                sykmeldingId = oldSykmelding.id,
                hoveddiagnose =
                    oldSykmelding.medisinskVurdering.hovedDiagnose?.let {
                        Diagnose(it.kode, it.system)
                    },
                bidiagnoser =
                    oldSykmelding.medisinskVurdering.biDiagnoser.map {
                        Diagnose(it.kode, it.system)
                    },
                annenFravarsArsak =
                    oldSykmelding.medisinskVurdering.annenFraversArsak?.let { annenFraversArsak ->
                        AnnenFravarsArsak(
                            beskrivelse = annenFraversArsak.beskrivelse,
                            grunn = annenFraversArsak.grunn.map { it.name },
                        )
                    },
                perioder = oldSykmelding.perioder.map(Periode::toSykmeldingPeriode),
                utdypendeOpplysninger = mapSvar(oldSykmelding.utdypendeOpplysninger),
                kontaktPasientBegrunnelseIkkeKontakt =
                    oldSykmelding.kontaktMedPasient.begrunnelseIkkeKontakt,
                tidligereSykmeldinger =
                    tidligereSykmeldinger
                        // TODO: Should this be controlled by the lib? Probably
                        .filter { it.behandlingsutfall.status != RegelStatusDTO.INVALID }
                        .filterNot { harTilbakedatertMerknad(it) }
                        .filter { it.medisinskVurdering?.hovedDiagnose?.kode != null }
                        .filter {
                            it.medisinskVurdering?.hovedDiagnose?.kode ==
                                oldSykmelding.medisinskVurdering.hovedDiagnose?.kode
                        }
                        .map {
                            TidligereSykmelding(
                                sykmeldingId = it.id,
                                perioder =
                                    it.sykmeldingsperioder.map(
                                        SykmeldingsperiodeDTO::toSykmeldingPeriode,
                                    ),
                                hoveddiagnose =
                                    it.medisinskVurdering?.hovedDiagnose?.let { diagnose ->
                                        Diagnose(
                                            kode = diagnose.kode,
                                            system =
                                                "TODO: System kommer ikke fra registeret? :huh:",
                                        )
                                    },
                            )
                        },
                pasient =
                    RegulaPasient(
                        ident = receivedSykmelding.personNrPasient,
                        fodselsdato = ruleMetadataSykmelding.ruleMetadata.pasientFodselsdato,
                    ),
                meta =
                    RegulaMeta(
                        behandletTidspunkt = ruleMetadataSykmelding.ruleMetadata.behandletTidspunkt,
                        mottattDato = ruleMetadataSykmelding.ruleMetadata.receivedDate,
                        signaturdato = oldSykmelding.signaturDato,
                        rulesetVersion = receivedSykmelding.rulesetVersion,
                    ),
                behandler =
                    RegulaBehandler(
                        suspendert = ruleMetadataSykmelding.doctorSuspensjon,
                        fnr = oldSykmelding.behandler.fnr,
                        legekontorOrgnr = ruleMetadataSykmelding.ruleMetadata.legekontorOrgnr,
                        godkjenninger =
                            ruleMetadataSykmelding.behandlerOgStartdato.behandler.godkjenninger.map(
                                Godkjenning::toBehandlerGodkjenning,
                            ),
                    ),
                avsender =
                    RegulaAvsender(
                        ruleMetadataSykmelding.ruleMetadata.avsenderFnr,
                    ),
            )

        val newResult =
            executeRegulaRules(
                rulePayload,
            )


        val newVsOld: List<Pair<String, String>> =
            oldResult
                .map { it.first.printRulePath() }
                .zip(
                    newResult.results.map { it.rulePath },
                )

        val allPathsEqual = newVsOld.all { (old, new) -> old == new }

        if (allPathsEqual) {
            log.info(
                """ ✅ REGULA SHADOW TEST Result: OK
                | SykmeldingID: ${oldSykmelding.id}
                | Outcome: ${newResult.status.name} (${oldValidationResult.status.name})
                | Chains executed: ${oldResult.size} / ${newResult.results.size}
            """.trimMargin(),
            )
        } else {
            log.warn(
                """ ❌ REGULA SHADOW TEST Result: DIVERGENCE DETECTED
                    | SykmeldingID: ${oldSykmelding.id}
                    | Outcome: ${newResult.status.name} (${oldValidationResult.status.name})
                    | Chains executed: ${oldResult.size} / ${newResult.results.size}
                    | Diverging paths count: ${newVsOld.count { (old, new) -> old != new }} 
                    | 
                    | Divering paths:
                    | ${
                    newVsOld.joinToString("\n") { (old, new) ->
                        "Old: $old\nNew: $new"
                    }
                }
                    """
                    .trimMargin(),
            )

        }
    } catch (e: Exception) {
        log.error("Regulus Regula smoke test failed", e)
    }
}

private fun Periode.toSykmeldingPeriode(): SykmeldingPeriode =
    when {
        aktivitetIkkeMulig != null ->
            SykmeldingPeriode.AktivitetIkkeMulig(
                fom = fom,
                tom = tom,
            )

        gradert != null ->
            SykmeldingPeriode.Gradert(
                fom = fom,
                tom = tom,
                grad = gradert.grad,
            )

        reisetilskudd ->
            SykmeldingPeriode.Reisetilskudd(
                fom = fom,
                tom = tom,
            )

        behandlingsdager != null ->
            SykmeldingPeriode.Behandlingsdager(
                fom = fom,
                tom = tom,
                behandlingsdager = behandlingsdager,
            )

        avventendeInnspillTilArbeidsgiver != null ->
            SykmeldingPeriode.Avventende(
                fom = fom,
                tom = tom,
                avventendeInnspillTilArbeidsgiver = avventendeInnspillTilArbeidsgiver,
            )

        else ->
            SykmeldingPeriode.Ugyldig(
                fom = fom,
                tom = tom,
            )
    }

private fun SykmeldingsperiodeDTO.toSykmeldingPeriode(): SykmeldingPeriode =
    when {
        type == PeriodetypeDTO.AKTIVITET_IKKE_MULIG ->
            SykmeldingPeriode.AktivitetIkkeMulig(
                fom = fom,
                tom = tom,
            )

        type == PeriodetypeDTO.GRADERT && gradert != null ->
            SykmeldingPeriode.Gradert(
                fom = fom,
                tom = tom,
                grad = gradert.grad,
            )

        type == PeriodetypeDTO.REISETILSKUDD ->
            SykmeldingPeriode.Reisetilskudd(
                fom = fom,
                tom = tom,
            )

        type == PeriodetypeDTO.BEHANDLINGSDAGER ->
            SykmeldingPeriode.Behandlingsdager(
                fom = fom,
                tom = tom,
                behandlingsdager =
                    // TODO: Kommer ikke fra registeret, ikke nødvendig for testene, burde regula ha
                    // forskjellige typer for de to periode (gamle og nåværende)
                    0,
            )

        type == PeriodetypeDTO.AVVENTENDE ->
            SykmeldingPeriode.Avventende(
                fom = fom,
                tom = tom,
                // TODO: Kommer ikke fra registeret, ikke nødvendig for testene, burde regula ha
                // forskjellige typer for de to periode (gamle og nåværende)
                avventendeInnspillTilArbeidsgiver = null,
            )

        else ->
            SykmeldingPeriode.Ugyldig(
                fom = fom,
                tom = tom,
            )
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

private fun harTilbakedatertMerknad(sykmelding: SykmeldingDTO): Boolean {
    return sykmelding.merknader?.any { MerknadType.contains(it.type) } ?: false
}
