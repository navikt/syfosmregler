package no.nav.syfo

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.random.Random
import no.nav.helse.diagnosekoder.Diagnosekoder
import no.nav.syfo.client.BehandlingsutfallDTO
import no.nav.syfo.client.DiagnoseDTO
import no.nav.syfo.client.GradertDTO
import no.nav.syfo.client.MedisinskVurderingDTO
import no.nav.syfo.client.Merknad
import no.nav.syfo.client.PeriodetypeDTO
import no.nav.syfo.client.RegelStatusDTO
import no.nav.syfo.client.SykmeldingDTO
import no.nav.syfo.client.SykmeldingStatusDTO
import no.nav.syfo.client.SykmeldingsperiodeDTO
import no.nav.syfo.model.Adresse
import no.nav.syfo.model.AktivitetIkkeMulig
import no.nav.syfo.model.AnnenFraversArsak
import no.nav.syfo.model.Arbeidsgiver
import no.nav.syfo.model.ArbeidsrelatertArsak
import no.nav.syfo.model.ArbeidsrelatertArsakType
import no.nav.syfo.model.AvsenderSystem
import no.nav.syfo.model.Behandler
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.ErIArbeid
import no.nav.syfo.model.ErIkkeIArbeid
import no.nav.syfo.model.Gradert
import no.nav.syfo.model.HarArbeidsgiver
import no.nav.syfo.model.KontaktMedPasient
import no.nav.syfo.model.MedisinskArsak
import no.nav.syfo.model.MedisinskArsakType
import no.nav.syfo.model.MedisinskVurdering
import no.nav.syfo.model.MeldingTilNAV
import no.nav.syfo.model.Periode
import no.nav.syfo.model.Prognose
import no.nav.syfo.model.SporsmalSvar
import no.nav.syfo.model.Sykmelding

fun Diagnosekoder.ICPC2.toDiagnose() = Diagnose(system = oid, kode = code, tekst = text)

fun Diagnosekoder.ICD10.toDiagnose() = Diagnose(system = oid, kode = code, tekst = text)

fun generateSykmelding(
    fom: LocalDate = LocalDate.now(),
    tom: LocalDate = LocalDate.now().plusDays(10),
    id: String = UUID.randomUUID().toString(),
    pasientAktoerId: String = UUID.randomUUID().toString(),
    syketilfelleStartDato: LocalDate = LocalDate.now(),
    medisinskVurdering: MedisinskVurdering = generateMedisinskVurdering(),
    skjermetForPasient: Boolean = false,
    perioder: List<Periode> = listOf(generatePeriode(fom = fom, tom = tom)),
    prognose: Prognose = generatePrognose(),
    utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>> = mapOf(),
    tiltakArbeidsplassen: String? = null,
    tiltakNAV: String? = null,
    andreTiltak: String? = null,
    meldingTilNAV: MeldingTilNAV? = null,
    meldingTilArbeidsgiver: String? = null,
    kontaktMedPasient: KontaktMedPasient = generateKontaktMedPasient(),
    behandletTidspunkt: LocalDateTime = LocalDateTime.now(),
    behandler: Behandler = generateBehandler(),
    avsenderSystem: AvsenderSystem = generateAvsenderSystem(),
    arbeidsgiver: Arbeidsgiver = generateArbeidsgiver(),
    msgid: String = UUID.randomUUID().toString(),
    navnFastlege: String? = null,
    signaturDato: LocalDateTime = behandletTidspunkt,
) =
    Sykmelding(
        id = id,
        msgId = msgid,
        pasientAktoerId = pasientAktoerId,
        signaturDato = signaturDato,
        syketilfelleStartDato = syketilfelleStartDato,
        medisinskVurdering = medisinskVurdering,
        skjermesForPasient = skjermetForPasient,
        perioder = perioder,
        prognose = prognose,
        utdypendeOpplysninger = utdypendeOpplysninger,
        tiltakArbeidsplassen = tiltakArbeidsplassen,
        tiltakNAV = tiltakNAV,
        andreTiltak = andreTiltak,
        meldingTilNAV = meldingTilNAV,
        meldingTilArbeidsgiver = meldingTilArbeidsgiver,
        kontaktMedPasient = kontaktMedPasient,
        behandletTidspunkt = behandletTidspunkt,
        behandler = behandler,
        avsenderSystem = avsenderSystem,
        arbeidsgiver = arbeidsgiver,
        navnFastlege = navnFastlege,
    )

fun generateMedisinskVurdering(
    hovedDiagnose: Diagnose? = generateDiagnose(),
    bidiagnoser: List<Diagnose> = listOf(),
    svangerskap: Boolean = false,
    yrkesskade: Boolean = false,
    yrkesskadeDato: LocalDate? = null,
    annenFraversArsak: AnnenFraversArsak? = null,
) =
    MedisinskVurdering(
        hovedDiagnose = hovedDiagnose,
        biDiagnoser = bidiagnoser,
        svangerskap = svangerskap,
        yrkesskade = yrkesskade,
        yrkesskadeDato = yrkesskadeDato,
        annenFraversArsak = annenFraversArsak,
    )

fun generateDiagnose() =
    Diagnosekoder.icpc2.values
        .stream()
        .skip(Random.nextInt(Diagnosekoder.icpc2.values.size).toLong())
        .findFirst()
        .get()
        .toDiagnose()

fun generatePeriode(
    fom: LocalDate = LocalDate.now(),
    tom: LocalDate = LocalDate.now().plusDays(10),
    aktivitetIkkeMulig: AktivitetIkkeMulig? = generateAktivitetIkkeMulig(),
    avventendeInnspillTilArbeidsgiver: String? = null,
    behandlingsdager: Int? = null,
    gradert: Gradert? = null,
    reisetilskudd: Boolean = false,
) =
    Periode(
        fom = fom,
        tom = tom,
        aktivitetIkkeMulig = aktivitetIkkeMulig,
        avventendeInnspillTilArbeidsgiver = avventendeInnspillTilArbeidsgiver,
        behandlingsdager = behandlingsdager,
        gradert = gradert,
        reisetilskudd = reisetilskudd,
    )

fun generateAktivitetIkkeMulig(
    medisinskArsak: MedisinskArsak? = generateMedisinskArsak(),
    arbeidsrelatertArsak: ArbeidsrelatertArsak? = null,
) =
    AktivitetIkkeMulig(
        medisinskArsak = medisinskArsak,
        arbeidsrelatertArsak = arbeidsrelatertArsak,
    )

fun generateArbeidsrelatertArsak(
    beskrivelse: String = "test data",
    arsak: List<ArbeidsrelatertArsakType> =
        listOf(
            ArbeidsrelatertArsakType.values()[
                    Random.nextInt(ArbeidsrelatertArsakType.values().size),
                ],
        ),
) =
    ArbeidsrelatertArsak(
        beskrivelse = beskrivelse,
        arsak = arsak,
    )

fun generateMedisinskArsak(
    beskrivelse: String = "test data",
    arsak: List<MedisinskArsakType> =
        listOf(MedisinskArsakType.values()[Random.nextInt(MedisinskArsakType.values().size)]),
) =
    MedisinskArsak(
        beskrivelse = beskrivelse,
        arsak = arsak,
    )

fun generateGradert(
    reisetilskudd: Boolean = false,
    grad: Int = 50,
) =
    Gradert(
        reisetilskudd = reisetilskudd,
        grad = grad,
    )

fun generatePrognose(
    arbeidsforEtterPeriode: Boolean = true,
    hennsynArbeidsplassen: String? = null,
    erIArbeid: ErIArbeid? = generateErIArbeid(),
    erIkkeIArbeid: ErIkkeIArbeid? = null,
) =
    Prognose(
        arbeidsforEtterPeriode = arbeidsforEtterPeriode,
        hensynArbeidsplassen = hennsynArbeidsplassen,
        erIArbeid = erIArbeid,
        erIkkeIArbeid = erIkkeIArbeid,
    )

fun generateErIkkeIArbeid(
    arbeidsforPaSikt: Boolean = true,
    arbeidsforFOM: LocalDate? = LocalDate.now().plusDays(30),
    vurderingsdato: LocalDate? = LocalDate.now(),
) =
    ErIkkeIArbeid(
        arbeidsforPaSikt = arbeidsforPaSikt,
        arbeidsforFOM = arbeidsforFOM,
        vurderingsdato = vurderingsdato,
    )

fun generateErIArbeid(
    egetArbeidPaSikt: Boolean = true,
    annetArbeidPaSikt: Boolean = true,
    arbeidFOM: LocalDate = LocalDate.now().plusDays(30),
    vurderingsdato: LocalDate = LocalDate.now(),
) =
    ErIArbeid(
        egetArbeidPaSikt = egetArbeidPaSikt,
        annetArbeidPaSikt = annetArbeidPaSikt,
        arbeidFOM = arbeidFOM,
        vurderingsdato = vurderingsdato,
    )

fun generateKontaktMedPasient(
    kontaktDato: LocalDate? = LocalDate.now(),
    begrunnelseIkkeKontakt: String? = null,
) = KontaktMedPasient(kontaktDato = kontaktDato, begrunnelseIkkeKontakt = begrunnelseIkkeKontakt)

fun generateBehandler(
    fornavn: String = "Fornavn",
    mellomnavn: String? = "Mellomnavn",
    etternavn: String = "Etternavnsen",
    aktoerId: String = "128731827",
    tlf: String? = "98765432",
    fnr: String = "1234567891",
    hpr: String? = null,
    her: String? = null,
    adresse: Adresse = generateAdresse(),
) =
    Behandler(
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        aktoerId = aktoerId,
        tlf = tlf,
        fnr = fnr,
        hpr = hpr,
        her = her,
        adresse = adresse,
    )

fun generateAdresse(
    gate: String? = "Gate",
    postnummer: Int? = 557,
    kommune: String? = "Oslo",
    postboks: String? = null,
    land: String? = "NO",
) =
    Adresse(
        gate = gate,
        postnummer = postnummer,
        kommune = kommune,
        postboks = postboks,
        land = land,
    )

fun generateAvsenderSystem(
    navn: String = "test",
    versjon: String = "1.2.3",
) =
    AvsenderSystem(
        navn = navn,
        versjon = versjon,
    )

fun generateArbeidsgiver(
    harArbeidsgiver: HarArbeidsgiver = HarArbeidsgiver.EN_ARBEIDSGIVER,
    legekontor: String = "HelseHus",
    yrkesbetegnelse: String = "Maler",
    stillingsprosent: Int = 100,
) =
    Arbeidsgiver(
        harArbeidsgiver = harArbeidsgiver,
        navn = legekontor,
        yrkesbetegnelse = yrkesbetegnelse,
        stillingsprosent = stillingsprosent,
    )

fun generateSykmeldingDTO(
    fom: LocalDate,
    tom: LocalDate = fom.plusMonths(1),
    behandlingsutfallDTO: BehandlingsutfallDTO =
        BehandlingsutfallDTO(
            RegelStatusDTO.OK,
        ),
    behandletDato: LocalDate? = null,
    diagnosekode: String? = "L89",
    merknader: List<Merknad>? = null,
    periodeType: PeriodetypeDTO = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
    gradert: GradertDTO? = null,
    ekstraPerioder: List<SykmeldingsperiodeDTO> = emptyList(),
    sykmeldingStatus: SykmeldingStatusDTO = SykmeldingStatusDTO("APEN", OffsetDateTime.now())
): SykmeldingDTO {
    return SykmeldingDTO(
        id = UUID.randomUUID().toString(),
        behandlingsutfall = behandlingsutfallDTO,
        sykmeldingsperioder =
            listOf(
                SykmeldingsperiodeDTO(
                    fom,
                    tom,
                    gradert,
                    periodeType,
                ),
            ) + ekstraPerioder,
        behandletTidspunkt =
            if (behandletDato != null) {
                OffsetDateTime.of(behandletDato.atStartOfDay(), ZoneOffset.UTC)
            } else {
                OffsetDateTime.of(fom.atStartOfDay(), ZoneOffset.UTC)
            },
        medisinskVurdering = MedisinskVurderingDTO(diagnosekode?.let { DiagnoseDTO(diagnosekode) }),
        merknader = merknader,
        sykmeldingStatus = sykmeldingStatus
    )
}
