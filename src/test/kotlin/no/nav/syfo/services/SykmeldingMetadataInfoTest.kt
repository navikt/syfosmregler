package no.nav.syfo.services

import io.kotest.core.spec.style.FunSpec
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.client.Merknad
import no.nav.syfo.client.MerknadType
import no.nav.syfo.client.PeriodetypeDTO
import no.nav.syfo.client.SmregisterClient
import no.nav.syfo.client.sykmeldingRespons
import no.nav.syfo.model.Adresse
import no.nav.syfo.model.AktivitetIkkeMulig
import no.nav.syfo.model.Arbeidsgiver
import no.nav.syfo.model.AvsenderSystem
import no.nav.syfo.model.Behandler
import no.nav.syfo.model.Diagnose
import no.nav.syfo.model.HarArbeidsgiver
import no.nav.syfo.model.KontaktMedPasient
import no.nav.syfo.model.MedisinskVurdering
import no.nav.syfo.model.Periode
import no.nav.syfo.model.Prognose
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.utils.LoggingMeta
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate

class SykmeldingMetadataInfoTest : FunSpec({
    val smregisterClient = mockk<SmregisterClient>()
    val loggingMeta = mockk<LoggingMeta>(relaxed = true)
    val sykmeldingService = SykmeldingService(smregisterClient)
    beforeAny {
        clearMocks(smregisterClient)
    }
    context("Test ettersending") {
        test("FALSE Ingen sykmeldinger gir ikke forlengelse eller ettersending") {
            coEvery { smregisterClient.getSykmeldinger("1") } returns emptyList()
            val periode = getPeriode(
                fom = LocalDate.of(2023, 1, 1),
                tom = LocalDate.of(2023, 1, 10),
            )
            val sykmeldingMetadata = sykmeldingService.getSykmeldingMetadataInfo("1", getSykmelding("L89", periode), loggingMeta)
            sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
            sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
        }

        test("False hvis bruker har sykmelding med annen FOM") {
            val sykmeldingResponse = sykmeldingRespons(
                fom = LocalDate.of(2021, 2, 15),
                tom = LocalDate.of(2021, 3, 15)
            )
            coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse
            val periode = getPeriode(
                fom = LocalDate.of(2021, 2, 14),
                tom = LocalDate.of(2021, 3, 15),
            )
            val sykmeldingMetadata = sykmeldingService.getSykmeldingMetadataInfo("1", getSykmelding("L89", periode), loggingMeta)
            sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
            sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
        }

        test("False hvis bruke har en sykmelding som er til under behandlign hos manuell") {
            val sykmeldingResponse = sykmeldingRespons(
                fom = LocalDate.of(2021, 2, 15),
                tom = LocalDate.of(2021, 3, 15),
                merknader = listOf(Merknad(
                    MerknadType.UNDER_BEHANDLING.name, "-"
                ))
            )
            coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse
            val periode = getPeriode(
                fom = LocalDate.of(2021, 2, 15),
                tom = LocalDate.of(2021, 3, 15),
            )
            val sykmeldingMetadata = sykmeldingService.getSykmeldingMetadataInfo("1", getSykmelding("L89", periode), loggingMeta)
            sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
            sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
        }

        test("False hvis bruke har en sykmelding som ikke er en godkjent tilbakedatering hos manuell") {
            val sykmeldingResponse = sykmeldingRespons(
                fom = LocalDate.of(2021, 2, 15),
                tom = LocalDate.of(2021, 3, 15),
                merknader = listOf(Merknad(
                    MerknadType.UGYLDIG_TILBAKEDATERING.name, "-"
                ))
            ) + sykmeldingRespons(
                fom = LocalDate.of(2021, 2, 15),
                tom = LocalDate.of(2021, 3, 15),
                merknader = listOf(Merknad(
                    MerknadType.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER.name, "."
                ))
            )
            coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse
            val periode = getPeriode(
                fom = LocalDate.of(2021, 2, 15),
                tom = LocalDate.of(2021, 3, 15),
            )
            val sykmeldingMetadata = sykmeldingService.getSykmeldingMetadataInfo("1", getSykmelding("L89", periode), loggingMeta)
            sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
            sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
        }

        test("False om sykmeldt har en godkjent sykmelding men med annen diagnose") {
            val sykmeldingResponse = sykmeldingRespons(
                fom = LocalDate.of(2021, 2, 15),
                tom = LocalDate.of(2021, 3, 15),
            )
            coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse
            val periode = getPeriode(
                fom = LocalDate.of(2021, 2, 15),
                tom = LocalDate.of(2021, 3, 15),
            )
            val sykmeldingMetadata = sykmeldingService.getSykmeldingMetadataInfo("1", getSykmelding("L90", periode), loggingMeta)
            sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
            sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
        }

        test("False om har annen TOM") {
            val sykmeldingResponse = sykmeldingRespons(
                fom = LocalDate.of(2021, 2, 15),
                tom = LocalDate.of(2021, 3, 15),
            )
            coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse
            val periode = getPeriode(
                fom = LocalDate.of(2021, 2, 15),
                tom = LocalDate.of(2021, 3, 16),
            )
            val sykmeldingMetadata = sykmeldingService.getSykmeldingMetadataInfo("1", getSykmelding("L89", periode), loggingMeta)
            sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
        }

        test("False om tidliger sykmelding er Behandlingsdager") {
            val sykmeldingResponse = sykmeldingRespons(
                fom = LocalDate.of(2021, 2, 15),
                tom = LocalDate.of(2021, 3, 15),
                periodeType = PeriodetypeDTO.BEHANDLINGSDAGER
            )
            coEvery { smregisterClient.getSykmeldinger("1") } returns sykmeldingResponse
            val periode = getPeriode(
                fom = LocalDate.of(2021, 2, 15),
                tom = LocalDate.of(2021, 3, 16),
            )
            val sykmeldingMetadata = sykmeldingService.getSykmeldingMetadataInfo("1", getSykmelding("L89", periode), loggingMeta)
            sykmeldingMetadata.ettersendingAv shouldBeEqualTo null
            sykmeldingMetadata.forlengelseAv shouldBeEqualTo emptyList()
        }
    }
})

fun getPeriode(fom: LocalDate, tom: LocalDate) : Periode {
    return Periode(fom, tom, AktivitetIkkeMulig(null, null), null, null, null, false)
}
fun getSykmelding(diagnoseKode: String, periode: Periode): Sykmelding {
    return Sykmelding(
        id = "2",
        msgId = "null",
        pasientAktoerId = "1",
        medisinskVurdering = MedisinskVurdering(
            hovedDiagnose = Diagnose(
                "system", diagnoseKode, ""
            ),
            emptyList(), true, false, null, null,
        ),
        skjermesForPasient = false,
        arbeidsgiver = Arbeidsgiver(HarArbeidsgiver.EN_ARBEIDSGIVER, null, null, null),
        perioder = listOf(periode),
        andreTiltak = null,
        avsenderSystem = AvsenderSystem("", ""),
        behandler = Behandler("", null, "", "", "", "", "", Adresse(null, null, null, null, null), null),
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
        utdypendeOpplysninger = emptyMap()
    )
}