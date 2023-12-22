package no.nav.syfo.services

import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import no.nav.syfo.client.Merknad
import no.nav.syfo.client.MerknadType
import no.nav.syfo.client.PeriodetypeDTO
import no.nav.syfo.client.SmregisterClient
import no.nav.syfo.generateSykmeldingDTO
import no.nav.syfo.model.Adresse
import no.nav.syfo.model.AktivitetIkkeMulig
import no.nav.syfo.model.Arbeidsgiver
import no.nav.syfo.model.AvsenderSystem
import no.nav.syfo.model.Behandler
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.Gradert
import no.nav.syfo.model.HarArbeidsgiver
import no.nav.syfo.model.KontaktMedPasient
import no.nav.syfo.model.MedisinskVurdering
import no.nav.syfo.model.Periode
import no.nav.syfo.model.Prognose
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.utils.LoggingMeta
import org.amshove.kluent.shouldBeEqualTo

class SykmeldingMetadataInfoTest :
    FunSpec(
        {
            val smregisterClient = mockk<SmregisterClient>()
            val loggingMeta = mockk<LoggingMeta>(relaxed = true)
            val sykmeldingService = SykmeldingService(smregisterClient)

            context("Test ettersending") {
                test("FALSE Ingen sykmeldinger gir ikke forlengelse eller ettersending") {
                    coEvery { smregisterClient.getSykmeldinger("1") } returns emptyList()
                    val periode =
                        getPeriode(
                            fom = LocalDate.of(2023, 1, 1),
                            tom = LocalDate.of(2023, 1, 10),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
                }

                test("False hvis bruker har sykmelding med annen FOM") {
                    val sykmeldingResponse =
                        listOf(
                            generateSykmeldingDTO(
                                fom = LocalDate.of(2021, 2, 15),
                                tom = LocalDate.of(2021, 3, 15),
                            ),
                        )
                    coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse
                    val periode =
                        getPeriode(
                            fom = LocalDate.of(2021, 2, 14),
                            tom = LocalDate.of(2021, 3, 15),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
                }

                test("False hvis bruke har en sykmelding som er til under behandlign hos manuell") {
                    val sykmeldingResponse =
                        listOf(
                            generateSykmeldingDTO(
                                fom = LocalDate.of(2021, 2, 15),
                                tom = LocalDate.of(2021, 3, 15),
                                merknader =
                                    listOf(
                                        Merknad(
                                            MerknadType.UNDER_BEHANDLING.name,
                                            "-",
                                        ),
                                    ),
                            ),
                        )
                    coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse
                    val periode =
                        getPeriode(
                            fom = LocalDate.of(2021, 2, 15),
                            tom = LocalDate.of(2021, 3, 15),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
                }

                test(
                    "False hvis bruke har en sykmelding som ikke er en godkjent tilbakedatering hos manuell",
                ) {
                    val sykmeldingResponse =
                        listOf(
                            generateSykmeldingDTO(
                                fom = LocalDate.of(2021, 2, 15),
                                tom = LocalDate.of(2021, 3, 15),
                                merknader =
                                    listOf(
                                        Merknad(
                                            MerknadType.UGYLDIG_TILBAKEDATERING.name,
                                            "-",
                                        ),
                                    ),
                            ),
                            generateSykmeldingDTO(
                                fom = LocalDate.of(2021, 2, 15),
                                tom = LocalDate.of(2021, 3, 15),
                                merknader =
                                    listOf(
                                        Merknad(
                                            MerknadType.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER
                                                .name,
                                            ".",
                                        ),
                                    ),
                            ),
                        )
                    coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse
                    val periode =
                        getPeriode(
                            fom = LocalDate.of(2021, 2, 15),
                            tom = LocalDate.of(2021, 3, 15),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
                }

                test("False om sykmeldt har en godkjent sykmelding men med annen diagnose") {
                    val sykmeldingResponse =
                        listOf(
                            generateSykmeldingDTO(
                                fom = LocalDate.of(2021, 2, 15),
                                tom = LocalDate.of(2021, 3, 15),
                            ),
                        )
                    coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse
                    val periode =
                        getPeriode(
                            fom = LocalDate.of(2021, 2, 15),
                            tom = LocalDate.of(2021, 3, 15),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L90", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
                }

                test("False om har annen TOM") {
                    val sykmeldingResponse =
                        listOf(
                            generateSykmeldingDTO(
                                fom = LocalDate.of(2021, 2, 15),
                                tom = LocalDate.of(2021, 3, 15),
                            ),
                        )
                    coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse
                    val periode =
                        getPeriode(
                            fom = LocalDate.of(2021, 2, 15),
                            tom = LocalDate.of(2021, 3, 16),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                }

                test("False om tidliger sykmelding er Behandlingsdager") {
                    val sykmeldingResponse =
                        listOf(
                            generateSykmeldingDTO(
                                fom = LocalDate.of(2021, 2, 15),
                                tom = LocalDate.of(2021, 3, 15),
                                periodeType = PeriodetypeDTO.BEHANDLINGSDAGER,
                            ),
                        )
                    coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse
                    val periode =
                        getPeriode(
                            fom = LocalDate.of(2021, 2, 15),
                            tom = LocalDate.of(2021, 3, 16),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
                }

                test("False om hoveddiagnose er null") {
                    val sykmeldingResponse =
                        listOf(
                            generateSykmeldingDTO(
                                fom = LocalDate.of(2021, 2, 15),
                                tom = LocalDate.of(2021, 3, 15),
                                diagnosekode = null,
                            ),
                        )
                    coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse
                    val periode =
                        getPeriode(
                            fom = LocalDate.of(2021, 2, 15),
                            tom = LocalDate.of(2021, 3, 15),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding(null, periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
                }

                test("True når det er en ettersending") {
                    val sykmeldingResponse =
                        listOf(
                            generateSykmeldingDTO(
                                fom = LocalDate.of(2021, 2, 15),
                                tom = LocalDate.of(2021, 3, 15),
                            ),
                        )
                    coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse
                    val periode =
                        getPeriode(
                            fom = LocalDate.of(2021, 2, 15),
                            tom = LocalDate.of(2021, 3, 15),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo sykmeldingResponse.first().id
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo
                        listOf(
                            Forlengelse(
                                sykmeldingResponse.first().id,
                                periode.fom,
                                periode.tom,
                            ),
                        )
                }
            }

            context("Test forlengelse") {
                val fom = LocalDate.of(2023, 1, 1)
                val tom = LocalDate.of(2023, 1, 31)
                val sykmeldingResponse =
                    listOf(
                        generateSykmeldingDTO(
                            fom = fom,
                            tom = tom,
                        ),
                    )
                coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse

                test("Vanlig forlengelse OK") {
                    val periode =
                        getPeriode(
                            fom = LocalDate.of(2023, 2, 1),
                            tom = LocalDate.of(2023, 2, 15),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo
                        listOf(
                            Forlengelse(
                                sykmeldingResponse.first().id,
                                fom,
                                tom,
                            ),
                        )
                }

                test("Forlengelse intill 16 dager etter") {
                    val periode =
                        getPeriode(
                            fom = LocalDate.of(2023, 2, 16),
                            tom = LocalDate.of(2023, 2, 20),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo
                        listOf(
                            Forlengelse(
                                sykmeldingResponse.first().id,
                                fom,
                                tom,
                            ),
                        )
                }

                test("Ikke Forlengelse 17 dager etter") {
                    val periode =
                        getPeriode(
                            fom = LocalDate.of(2023, 2, 17),
                            tom = LocalDate.of(2023, 2, 20),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
                }

                test("Ikke Forlengelse om sykmelding starter før tidligere sykmelding") {
                    val periode =
                        getPeriode(
                            fom = fom.minusDays(1),
                            tom = LocalDate.of(2023, 2, 20),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
                }

                test("Forlengelse med samme fom og tom") {
                    val periode =
                        getPeriode(
                            fom = fom,
                            tom = tom,
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo sykmeldingResponse.first().id
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo
                        listOf(
                            Forlengelse(
                                sykmeldingResponse.first().id,
                                fom,
                                tom,
                            ),
                        )
                }

                test("Forlengelse perioden er i en tidliger godkjent sykmeldingsperiode") {
                    val periode =
                        getPeriode(
                            fom = fom.plusDays(7),
                            tom = tom.minusDays(7),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo
                        listOf(
                            Forlengelse(
                                sykmeldingResponse.first().id,
                                fom,
                                tom,
                            ),
                        )
                }

                test("Forlengelse med en annen gradering") {
                    val periode =
                        getPeriode(
                                fom = tom.plusDays(5),
                                tom = tom.plusMonths(1),
                            )
                            .copy(gradert = Gradert(reisetilskudd = false, grad = 40))
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo
                        listOf(
                            Forlengelse(
                                sykmeldingResponse.first().id,
                                fom,
                                tom,
                            ),
                        )
                }

                test("Ikke forlengelse med sykmeldinger som er under behandling") {
                    val response =
                        listOf(
                            generateSykmeldingDTO(
                                fom = fom,
                                tom = tom,
                                merknader =
                                    listOf(
                                        Merknad(
                                            MerknadType.UNDER_BEHANDLING.name,
                                            "",
                                        ),
                                    ),
                            ),
                        )
                    coEvery { smregisterClient.getSykmeldinger("1") } returns response
                    val periode =
                        getPeriode(
                            fom = tom.plusDays(5),
                            tom = tom.plusMonths(1),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
                }

                test("Ikke forlengelse med sykmeldinger som ikke er godkjente tilbakedateringer") {
                    val response =
                        listOf(
                            generateSykmeldingDTO(
                                fom = fom,
                                tom = tom,
                                merknader =
                                    listOf(
                                        Merknad(
                                            MerknadType.UGYLDIG_TILBAKEDATERING.name,
                                            "",
                                        ),
                                    ),
                            ),
                        )
                    coEvery { smregisterClient.getSykmeldinger("1") } returns response
                    val periode =
                        getPeriode(
                            fom = tom.plusDays(5),
                            tom = tom.plusMonths(1),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
                }

                test(
                    "Forlengelse med en OK sykmelding og en sykmelding som ikke er godkjent tilbakedatering",
                ) {
                    val response =
                        listOf(
                            generateSykmeldingDTO(
                                fom = fom,
                                tom = tom,
                                merknader =
                                    listOf(
                                        Merknad(
                                            MerknadType.UGYLDIG_TILBAKEDATERING.name,
                                            "",
                                        ),
                                    ),
                            ),
                        ) +
                            listOf(
                                generateSykmeldingDTO(
                                    fom = fom,
                                    tom = tom,
                                ),
                            )
                    coEvery { smregisterClient.getSykmeldinger("1") } returns response
                    val periode =
                        getPeriode(
                            fom = tom.plusDays(5),
                            tom = tom.plusMonths(1),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo
                        listOf(
                            Forlengelse(
                                response[1].id,
                                fom,
                                tom,
                            ),
                        )
                }

                test("Forlengelse med flere sykmeldinger som passer") {
                    val response =
                        listOf(
                            generateSykmeldingDTO(
                                fom = fom,
                                tom = tom,
                            ),
                            generateSykmeldingDTO(
                                fom = fom.plusDays(1),
                                tom = tom.minusDays(1),
                            )
                        )
                    coEvery { smregisterClient.getSykmeldinger("1") } returns response
                    val periode =
                        getPeriode(
                            fom = tom.plusDays(5),
                            tom = tom.plusMonths(1),
                        )
                    val sykmeldingMetadata =
                        sykmeldingService.getSykmeldingMetadataInfo(
                            "1",
                            getSykmelding("L89", periode),
                            loggingMeta,
                        )
                    sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
                    sykmeldingMetadata.forlengelseAv shouldBeEqualTo
                        listOf(
                            Forlengelse(
                                response[0].id,
                                fom,
                                tom,
                            ),
                            Forlengelse(
                                response[1].id,
                                fom.plusDays(1),
                                tom.minusDays(1),
                            ),
                        )
                }
            }
        },
    )

fun getPeriode(fom: LocalDate, tom: LocalDate): Periode {
    return Periode(fom, tom, AktivitetIkkeMulig(null, null), null, null, null, false)
}

fun getSykmelding(diagnoseKode: String?, periode: Periode): Sykmelding {
    return Sykmelding(
        id = "2",
        msgId = "null",
        pasientAktoerId = "1",
        medisinskVurdering =
            MedisinskVurdering(
                hovedDiagnose =
                    diagnoseKode?.let {
                        Diagnose(
                            "system",
                            diagnoseKode,
                            "",
                        )
                    },
                emptyList(),
                true,
                false,
                null,
                null,
            ),
        skjermesForPasient = false,
        arbeidsgiver = Arbeidsgiver(HarArbeidsgiver.EN_ARBEIDSGIVER, null, null, null),
        perioder = listOf(periode),
        andreTiltak = null,
        avsenderSystem = AvsenderSystem("", ""),
        behandler =
            Behandler("", null, "", "", "", "", "", Adresse(null, null, null, null, null), null),
        prognose = Prognose(true, null, null, null),
        behandletTidspunkt = LocalDate.of(2023, 1, 1).atStartOfDay(),
        kontaktMedPasient = KontaktMedPasient(LocalDate.of(2023, 1, 1), null),
        meldingTilArbeidsgiver = null,
        meldingTilNAV = null,
        navnFastlege = null,
        signaturDato = LocalDate.of(2023, 1, 1).atStartOfDay(),
        syketilfelleStartDato = null,
        tiltakArbeidsplassen = null,
        tiltakNAV = null,
        utdypendeOpplysninger = emptyMap(),
    )
}
