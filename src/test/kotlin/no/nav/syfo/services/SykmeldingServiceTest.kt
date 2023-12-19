package no.nav.syfo.services

import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import no.nav.syfo.TestDates.Companion.april
import no.nav.syfo.TestDates.Companion.february
import no.nav.syfo.TestDates.Companion.january
import no.nav.syfo.TestDates.Companion.march
import no.nav.syfo.TestDates.Companion.may
import no.nav.syfo.TestDates.Companion.september
import no.nav.syfo.client.PeriodetypeDTO
import no.nav.syfo.client.SmregisterClient
import no.nav.syfo.client.SykmeldingStatusDTO
import no.nav.syfo.client.SykmeldingsperiodeDTO
import no.nav.syfo.generateSykmeldingDTO
import no.nav.syfo.utils.LoggingMeta
import org.amshove.kluent.shouldBeEqualTo
import java.time.OffsetDateTime

class SykmeldingServiceTest :
    FunSpec(
        {
            val smregisterClient = mockk<SmregisterClient>()
            val sykmeldingService = SykmeldingService(smregisterClient)
            val loggingMetadata = mockk<LoggingMeta>(relaxed = true)

            test("happy case - uten sykmeldinger fra register") {
                coEvery { smregisterClient.getSykmeldinger(any()) } returns emptyList()
                val metadata =
                    sykmeldingService.getSykmeldingMetadataInfo(
                        "1234678910",
                        getSykmelding(
                            null,
                            getPeriode(1.january(2023), 1.january(2023)),
                        ),
                        loggingMetadata,
                    )
                metadata.syketilfelleStartDato shouldBeEqualTo LocalDate.of(2023, 1, 1)
            }
            test("med en sykmelding kant til kant") {
                coEvery { smregisterClient.getSykmeldinger(any()) } returns
                    listOf(
                        generateSykmeldingDTO(
                            fom = 1.january(2023),
                            tom = 15.january(2023),
                        ),
                    )
                val metadata =
                    sykmeldingService.getSykmeldingMetadataInfo(
                        "1234678910",
                        getSykmelding(
                            null,
                            getPeriode(
                                fom = 16.january(2023),
                                tom = 20.january(2023),
                            ),
                        ),
                        loggingMetadata,
                    )
                metadata.syketilfelleStartDato shouldBeEqualTo LocalDate.of(2023, 1, 1)
            }
            test("med en sykmelding med 16 dager mellomrom") {
                coEvery { smregisterClient.getSykmeldinger(any()) } returns
                    listOf(
                        generateSykmeldingDTO(
                            fom = 1.january(2023),
                            tom = 5.january(2023),
                        ),
                    )
                val metadata =
                    sykmeldingService.getSykmeldingMetadataInfo(
                        "1234678910",
                        getSykmelding(
                            null,
                            getPeriode(
                                fom = 22.january(2023),
                                tom = 28.january(2023),
                            ),
                        ),
                        loggingMetadata,
                    )
                metadata.syketilfelleStartDato shouldBeEqualTo 22.january(2023)
            }

            test("med en sykmelding med 15 dager mellomrom") {
                coEvery { smregisterClient.getSykmeldinger(any()) } returns
                    listOf(
                        generateSykmeldingDTO(
                            fom = 1.january(2023),
                            tom = 5.january(2023),
                        ),
                    )
                val metadata =
                    sykmeldingService.getSykmeldingMetadataInfo(
                        "1234678910",
                        getSykmelding(
                            null,
                            getPeriode(
                                fom = 21.january(2023),
                                tom = 28.january(2023),
                            ),
                        ),
                        loggingMetadata,
                    )
                metadata.syketilfelleStartDato shouldBeEqualTo 1.january(2023)
            }

            test("test med sykmelding langt frem i tid") {
                coEvery { smregisterClient.getSykmeldinger(any()) } returns
                    listOf(
                        generateSykmeldingDTO(
                            fom = 5.january(2023),
                            tom = 5.september(2023),
                        ),
                    )
                val metadata =
                    sykmeldingService.getSykmeldingMetadataInfo(
                        "1234678910",
                        getSykmelding(
                            null,
                            getPeriode(
                                fom = 4.january(2023),
                                tom = 28.january(2023),
                            ),
                        ),
                        loggingMetadata,
                    )
                metadata.syketilfelleStartDato shouldBeEqualTo 4.january(2023)
            }

            test("tilbakedatert sykmelding") {
                coEvery { smregisterClient.getSykmeldinger(any()) } returns
                    listOf(
                        generateSykmeldingDTO(
                            fom = 1.february(2023),
                            tom = 28.february(2023),
                        ),
                    )
                val metadata =
                    sykmeldingService.getSykmeldingMetadataInfo(
                        "1234678910",
                        getSykmelding(
                            null,
                            getPeriode(
                                fom = 1.january(2023),
                                tom = 31.january(2023),
                            ),
                        ),
                        loggingMetadata,
                    )
                metadata.syketilfelleStartDato shouldBeEqualTo 1.january(2023)
            }

            test("Flere sykmeldinger fra syfosmregister") {
                coEvery { smregisterClient.getSykmeldinger(any()) } returns
                    listOf(
                        generateSykmeldingDTO(1.january(2023), 31.january(2023)),
                        generateSykmeldingDTO(1.february(2023), 28.february(2023)),
                        generateSykmeldingDTO(1.march(2023), 31.march(2023)),
                        generateSykmeldingDTO(1.april(2023), 30.april(2023)),
                    )

                val metadata =
                    sykmeldingService.getSykmeldingMetadataInfo(
                        "1234678910",
                        getSykmelding(
                            null,
                            getPeriode(
                                fom = 1.may(2023),
                                tom = 15.may(2023),
                            ),
                        ),
                        loggingMetadata,
                    )
                metadata.syketilfelleStartDato shouldBeEqualTo 1.january(2023)
            }

            test("Flere sykmeldinger fra syfosmregister med gap") {
                coEvery { smregisterClient.getSykmeldinger(any()) } returns
                    listOf(
                        generateSykmeldingDTO(1.january(2023), 31.january(2023)),
                        generateSykmeldingDTO(1.february(2023), 28.february(2023)),
                        generateSykmeldingDTO(17.march(2023), 31.march(2023)),
                        generateSykmeldingDTO(1.april(2023), 30.april(2023)),
                    )

                val metadata =
                    sykmeldingService.getSykmeldingMetadataInfo(
                        "1234678910",
                        getSykmelding(
                            null,
                            getPeriode(
                                fom = 1.may(2023),
                                tom = 15.may(2023),
                            ),
                        ),
                        loggingMetadata,
                    )
                metadata.syketilfelleStartDato shouldBeEqualTo 17.march(2023)
            }
            test("Flere sykmeldinger fra syfosmregister med avventende sykmelding") {
                coEvery { smregisterClient.getSykmeldinger(any()) } returns
                    listOf(
                        generateSykmeldingDTO(1.january(2023), 31.january(2023)),
                        generateSykmeldingDTO(1.february(2023), 28.february(2023)),
                        generateSykmeldingDTO(
                            1.march(2023),
                            31.march(2023),
                            periodeType = PeriodetypeDTO.AVVENTENDE,
                        ),
                        generateSykmeldingDTO(1.april(2023), 30.april(2023)),
                    )

                val metadata =
                    sykmeldingService.getSykmeldingMetadataInfo(
                        "1234678910",
                        getSykmelding(
                            null,
                            getPeriode(
                                fom = 1.may(2023),
                                tom = 15.may(2023),
                            ),
                        ),
                        loggingMetadata,
                    )
                metadata.syketilfelleStartDato shouldBeEqualTo 1.april(2023)
            }
            test("Flere sykmeldinger fra syfosmregister med flere perioder uten gap") {
                coEvery { smregisterClient.getSykmeldinger(any()) } returns
                    listOf(
                        generateSykmeldingDTO(1.january(2023), 31.january(2023)),
                        generateSykmeldingDTO(
                            1.february(2023),
                            28.february(2023),
                            ekstraPerioder =
                                listOf(
                                    SykmeldingsperiodeDTO(
                                        1.march(2023),
                                        10.march(2023),
                                        null,
                                        PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                                    ),
                                    SykmeldingsperiodeDTO(
                                        11.march(2023),
                                        20.march(2023),
                                        null,
                                        PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                                    ),
                                    SykmeldingsperiodeDTO(
                                        20.march(2023),
                                        5.april(2023),
                                        null,
                                        PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                                    ),
                                ),
                        ),
                        generateSykmeldingDTO(1.april(2023), 30.april(2023)),
                    )

                val metadata =
                    sykmeldingService.getSykmeldingMetadataInfo(
                        "1234678910",
                        getSykmelding(
                            null,
                            getPeriode(
                                fom = 1.may(2023),
                                tom = 15.may(2023),
                            ),
                        ),
                        loggingMetadata,
                    )
                metadata.syketilfelleStartDato shouldBeEqualTo 1.january(2023)
            }
            test("Flere sykmeldinger fra syfosmregister med flere perioder") {
                coEvery { smregisterClient.getSykmeldinger(any()) } returns
                    listOf(
                        generateSykmeldingDTO(
                            16.january(2023),
                            31.january(2023),
                            ekstraPerioder =
                                listOf(
                                    SykmeldingsperiodeDTO(
                                        1.february(2023),
                                        10.february(2023),
                                        null,
                                        PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                                    ),
                                    SykmeldingsperiodeDTO(
                                        11.february(2023),
                                        20.february(2023),
                                        null,
                                        PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                                    ),
                                ),
                        ),
                        generateSykmeldingDTO(6.april(2023), 30.april(2023)),
                    )

                val metadata =
                    sykmeldingService.getSykmeldingMetadataInfo(
                        "1234678910",
                        getSykmelding(
                            null,
                            getPeriode(
                                fom = 1.may(2023),
                                tom = 15.may(2023),
                            ),
                        ),
                        loggingMetadata,
                    )
                metadata.syketilfelleStartDato shouldBeEqualTo 6.april(2023)
            }

            test("Sykmelding fra syfosmregister med status AVBRUTT") {
                coEvery { smregisterClient.getSykmeldinger(any()) } returns
                    listOf(
                        generateSykmeldingDTO(16.january(2023), 31.january(2023)),
                        generateSykmeldingDTO(1.february(2023), 28.february(2023), sykmeldingStatus = SykmeldingStatusDTO("AVBRUTT", OffsetDateTime.now())
                            ),
                    )

                val metadata =
                    sykmeldingService.getSykmeldingMetadataInfo(
                        "1234678910",
                        getSykmelding(
                            null,
                            getPeriode(
                                fom = 1.march(2023),
                                tom = 15.march(2023),
                            ),
                        ),
                        loggingMetadata,
                    )
                metadata.syketilfelleStartDato shouldBeEqualTo 1.march(2023)
            }
        },
    )
