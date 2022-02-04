package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.LoggingMeta
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.azuread.v2.AzureAdV2Token
import no.nav.syfo.model.AktivitetIkkeMulig
import no.nav.syfo.model.MedisinskArsak
import no.nav.syfo.model.Periode
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.OffsetDateTime

object SyketilfelleClientTest : Spek({
    val loggingMeta = LoggingMeta("", "", "", "")
    val oppfolgingsdato = LocalDate.of(2021, 1, 3)
    val accessTokenClientMock = mockk<AzureAdV2Client>()
    val httpClient = mockk<HttpClient>(relaxed = true)

    val syketilfelleClient = SyketilfelleClient("http://syfosyketilfelle", accessTokenClientMock, "syfosyketilfelle", httpClient)

    beforeGroup {
        coEvery { accessTokenClientMock.getAccessToken(any()) } returns AzureAdV2Token("accessToken", OffsetDateTime.now().plusHours(1))
    }

    describe("SyketilfelleClient - startdato") {
        it("Startdato er null hvis ingen sykeforløp") {
            val startdato = syketilfelleClient.finnStartdato(
                emptyList(),
                listOf(lagPeriode(fom = LocalDate.of(2020, 10, 1), tom = LocalDate.of(2020, 10, 20))),
                loggingMeta
            )

            startdato shouldBeEqualTo null
        }
        it("Startdato er null hvis ingen perioder") {
            val startdato = syketilfelleClient.finnStartdato(
                listOf(
                    lagSykeforloep(
                        oppfolgingsdato,
                        fom = LocalDate.of(2021, 1, 3),
                        tom = LocalDate.of(2021, 1, 10)
                    )
                ),
                emptyList(),
                loggingMeta
            )

            startdato shouldBeEqualTo null
        }
        it("Startdato er null hvis tom i tidligere sykeforløp er mer enn 16 dager før første fom i sykmelding") {
            val startdato = syketilfelleClient.finnStartdato(
                listOf(
                    lagSykeforloep(
                        oppfolgingsdato,
                        fom = LocalDate.of(2021, 1, 3),
                        tom = LocalDate.of(2021, 1, 10)
                    )
                ),
                listOf(lagPeriode(fom = LocalDate.of(2021, 1, 27), tom = LocalDate.of(2021, 2, 10))),
                loggingMeta
            )

            startdato shouldBeEqualTo null
        }
        it("Startdato er satt hvis tom i tidligere sykeforløp er mindre enn 16 dager før første fom i sykmelding") {
            val startdato = syketilfelleClient.finnStartdato(
                listOf(
                    lagSykeforloep(
                        oppfolgingsdato,
                        fom = LocalDate.of(2021, 1, 3),
                        tom = LocalDate.of(2021, 1, 10)
                    )
                ),
                listOf(lagPeriode(fom = LocalDate.of(2021, 1, 26), tom = LocalDate.of(2021, 2, 10))),
                loggingMeta
            )

            startdato shouldBeEqualTo oppfolgingsdato
        }
        it("Startdato er null hvis fom i tidligere sykeforløp er mer enn 16 dager før siste tom i sykmelding") {
            val startdato = syketilfelleClient.finnStartdato(
                listOf(
                    lagSykeforloep(
                        oppfolgingsdato,
                        fom = LocalDate.of(2021, 1, 3),
                        tom = LocalDate.of(2021, 1, 10)
                    )
                ),
                listOf(lagPeriode(fom = LocalDate.of(2020, 12, 1), tom = LocalDate.of(2020, 12, 17))),
                loggingMeta
            )

            startdato shouldBeEqualTo null
        }
        it("Startdato er satt hvis fom i tidligere sykeforløp er mindre enn 16 dager før siste tom i sykmelding") {
            val startdato = syketilfelleClient.finnStartdato(
                listOf(
                    lagSykeforloep(
                        oppfolgingsdato,
                        fom = LocalDate.of(2021, 1, 3),
                        tom = LocalDate.of(2021, 1, 10)
                    )
                ),
                listOf(lagPeriode(fom = LocalDate.of(2020, 12, 1), tom = LocalDate.of(2020, 12, 18))),
                loggingMeta
            )

            startdato shouldBeEqualTo oppfolgingsdato
        }
        it("Startdato er satt sykmelding overlapper med tidligere sykeforløp") {
            val startdato = syketilfelleClient.finnStartdato(
                listOf(
                    lagSykeforloep(
                        oppfolgingsdato,
                        fom = LocalDate.of(2021, 1, 3),
                        tom = LocalDate.of(2021, 1, 17)
                    )
                ),
                listOf(lagPeriode(fom = LocalDate.of(2021, 1, 15), tom = LocalDate.of(2021, 2, 10))),
                loggingMeta
            )

            startdato shouldBeEqualTo oppfolgingsdato
        }
        it("Velger riktig startdato hvis flere sykeforløp og tom i tidligere sykeforløp er mindre enn 16 dager før første fom i sykmelding") {
            val startdato = syketilfelleClient.finnStartdato(
                listOf(
                    lagSykeforloep(oppfolgingsdato, fom = LocalDate.of(2021, 1, 3), tom = LocalDate.of(2021, 1, 10)),
                    lagSykeforloep(
                        oppfolgingsdato.minusWeeks(8),
                        fom = LocalDate.of(2020, 11, 3),
                        tom = LocalDate.of(2020, 11, 25)
                    )
                ),
                listOf(lagPeriode(fom = LocalDate.of(2021, 1, 26), tom = LocalDate.of(2021, 2, 10))),
                loggingMeta
            )

            startdato shouldBeEqualTo oppfolgingsdato
        }
    }
})

private fun lagSykeforloep(oppfolgingsdato: LocalDate, fom: LocalDate, tom: LocalDate) =
    Sykeforloep(
        oppfolgingsdato,
        listOf(SimpleSykmelding("321", fom, tom))
    )

private fun lagPeriode(fom: LocalDate, tom: LocalDate): Periode =
    Periode(
        fom = fom,
        tom = tom,
        aktivitetIkkeMulig = AktivitetIkkeMulig(medisinskArsak = MedisinskArsak(null, emptyList()), arbeidsrelatertArsak = null),
        avventendeInnspillTilArbeidsgiver = null,
        gradert = null, behandlingsdager = null, reisetilskudd = false
    )
