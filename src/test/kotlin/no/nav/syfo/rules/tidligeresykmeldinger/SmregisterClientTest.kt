package no.nav.syfo.rules.tidligeresykmeldinger

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import no.nav.syfo.rules.shared.AktivitetIkkeMulig
import no.nav.syfo.rules.shared.Periode

/*

object SmregisterClientTest : FunSpec({
    val loggingMeta = LoggingMeta("", "", "", "")
    val accessTokenClientMock = mockk<AzureAdV2Client>()
    val httpClient = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    }
    val mockHttpServerPort = ServerSocket(0).use { it.localPort }
    val mockHttpServerUrl = "http://localhost:$mockHttpServerPort"
    val mockServer = embeddedServer(Netty, mockHttpServerPort) {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        routing {
            accept(ContentType.Application.Json) {
                get("/api/v2/sykmelding/sykmeldinger") {
                    when (call.request.headers["fnr"]) {
                        "fnr" -> call.respond(HttpStatusCode.OK, emptyList<SykmeldingDTO>())
                        "fnr2" -> call.respond(HttpStatusCode.OK, sykmeldingRespons(fom = LocalDate.of(2021, 2, 15)))
                        "fnr3" -> call.respond(
                            HttpStatusCode.OK,
                            sykmeldingRespons(
                                fom = LocalDate.of(2021, 2, 15),
                                behandletDato = LocalDate.of(2021, 3, 1),
                            ),
                        )

                        "fnr4" -> call.respond(
                            HttpStatusCode.OK,
                            sykmeldingRespons(
                                fom = LocalDate.of(2021, 2, 15),
                                behandletDato = LocalDate.of(2021, 2, 22),
                            ),
                        )

                        "fnr5" -> call.respond(
                            HttpStatusCode.OK,
                            sykmeldingRespons(
                                fom = LocalDate.of(2021, 2, 15),
                                behandlingsutfallDTO = BehandlingsutfallDTO(RegelStatusDTO.INVALID),
                            ),
                        )

                        "fnr6" -> call.respond(HttpStatusCode.OK, sykmeldingRespons(fom = LocalDate.of(2021, 2, 15), diagnosekode = null))

                        "fnr7" -> call.respond(
                            HttpStatusCode.OK,
                            sykmeldingRespons(
                                fom = LocalDate.of(2021, 2, 15),
                                merknader = listOf(
                                    Merknad(
                                        type = MerknadType.UGYLDIG_TILBAKEDATERING.name,
                                        beskrivelse = null,
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }.start()

    val smregisterClient = SmregisterClient(mockHttpServerUrl, accessTokenClientMock, "resourceId", httpClient)

    afterSpec {
        mockServer.stop(TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(1))
    }

    beforeTest {
        coEvery { accessTokenClientMock.getAccessToken(any()) } returns AzureAdV2Token(
            "accessToken",
            OffsetDateTime.now().plusHours(1),
        )
    }

    context("Test av SmRegisterClient") {
        test("False hvis bruker ikke har andre sykmeldinger") {
            smregisterClient.erEttersending(
                "fnr",
                listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                "L89",
                loggingMeta,
            ) shouldBeEqualTo false
        }
        test("False hvis bruker har sykmelding med annen fom") {
            smregisterClient.erEttersending(
                "fnr2",
                listOf(lagPeriode(fom = LocalDate.of(2021, 1, 15), tom = LocalDate.of(2021, 2, 15))),
                "L89",
                loggingMeta,
            ) shouldBeEqualTo false
        }
        test("False hvis bruker har sykmelding med samme fom som er tilbakedatert") {
            smregisterClient.erEttersending(
                "fnr3",
                listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                "L89",
                loggingMeta,
            ) shouldBeEqualTo false
        }
        test("True hvis bruker har sykmelding med samme fom som ikke er tilbakedatert") {
            smregisterClient.erEttersending(
                "fnr2",
                listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                "L89",
                loggingMeta,
            ) shouldBeEqualTo true
        }
        test("True hvis bruker har sykmelding med samme fom som er tilbakedatert 7 dager") {
            smregisterClient.erEttersending(
                "fnr4",
                listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                "L89",
                loggingMeta,
            ) shouldBeEqualTo true
        }
        test("False hvis bruker har avvist sykmelding med samme fom som ikke er tilbakedatert") {
            smregisterClient.erEttersending(
                "fnr5",
                listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                "L89",
                loggingMeta,
            ) shouldBeEqualTo false
        }
        test("False hvis bruker har sykmelding med fom 2 dager før ny fom") {
            smregisterClient.erEttersending(
                "fnr2",
                listOf(lagPeriode(fom = LocalDate.of(2021, 2, 13), tom = LocalDate.of(2021, 2, 15))),
                "L89",
                loggingMeta,
            ) shouldBeEqualTo false
        }
        test("False hvis bruker har sykmelding med tom 2 dager etter ny tom") {
            smregisterClient.erEttersending(
                "fnr2",
                listOf(lagPeriode(fom = LocalDate.of(2021, 3, 15), tom = LocalDate.of(2021, 3, 17))),
                "L89",
                loggingMeta,
            ) shouldBeEqualTo false
        }
        test("False hvis bruker har sykmelding med fom 2 dager før ny fom men annen diagnose") {
            smregisterClient.erEttersending(
                "fnr2",
                listOf(lagPeriode(fom = LocalDate.of(2021, 2, 13), tom = LocalDate.of(2021, 2, 15))),
                "L87",
                loggingMeta,
            ) shouldBeEqualTo false
        }
        test("False hvis bruker har sykmelding med fom 2 dager før ny fom men annen grad") {
            smregisterClient.erEttersending(
                "fnr2",
                listOf(
                    Periode(
                        fom = LocalDate.of(2021, 2, 13),
                        tom = LocalDate.of(2021, 2, 15),
                        aktivitetIkkeMulig = null,
                        avventendeInnspillTilArbeidsgiver = null,
                        behandlingsdager = null,
                        gradert = Gradert(false, 50),
                        reisetilskudd = false,
                    ),
                ),
                "L89",
                loggingMeta,
            ) shouldBeEqualTo false
        }
    }

    context("Test av ny regel") {
        test("Test av overlappende datoer, skal returnere false når periodene ikke stemmer") {
            smregisterClient.erEttersending(
                fnr = "fnr2",
                listOf(
                    Periode(
                        fom = LocalDate.of(2021, 2, 13),
                        tom = LocalDate.of(2021, 2, 15),
                        aktivitetIkkeMulig = null,
                        avventendeInnspillTilArbeidsgiver = null,
                        behandlingsdager = null,
                        gradert = Gradert(false, 50),
                        reisetilskudd = false,
                    ),
                ),
                "L89",
                loggingMeta,
            ) shouldBeEqualTo false
        }

        test("Test med samme fom og tom med ulik gradering skal gi false") {
            smregisterClient.erEttersending(
                fnr = "fnr2",
                listOf(
                    Periode(
                        fom = LocalDate.of(2021, 2, 15),
                        tom = LocalDate.of(2021, 3, 15),
                        aktivitetIkkeMulig = null,
                        avventendeInnspillTilArbeidsgiver = null,
                        behandlingsdager = null,
                        gradert = Gradert(false, 50),
                        reisetilskudd = false,
                    ),
                ),
                "L89",
                loggingMeta,
            ) shouldBeEqualTo false
        }

        test("Test med samme fom og tom med lik gradering skal gi true") {
            smregisterClient.erEttersending(
                fnr = "fnr2",
                listOf(
                    lagPeriode(
                        fom = LocalDate.of(2021, 2, 15),
                        tom = LocalDate.of(2021, 3, 15),
                    ),
                ),
                "L89",
                loggingMeta,
            ) shouldBeEqualTo true
        }
    }

    test("Test med samme fom og tom med lik gradering og ulik diagnose skal gi false") {
        smregisterClient.erEttersending(
            fnr = "fnr2",
            listOf(
                lagPeriode(
                    fom = LocalDate.of(2021, 2, 15),
                    tom = LocalDate.of(2021, 3, 15),
                ),
            ),
            "LA8PV",
            loggingMeta,
        ) shouldBeEqualTo false
    }

    test("Test med samme fom og tom med ulik gradering og lik diagnose skal gi false") {
        smregisterClient.erEttersending(
            fnr = "fnr2",
            listOf(
                lagPeriode(
                    fom = LocalDate.of(2021, 2, 15),
                    tom = LocalDate.of(2021, 3, 15),
                ).copy(gradert = generateGradert(false, 37)),
            ),
            "L89",
            loggingMeta,
        ) shouldBeEqualTo false
    }

    test("Test med samme fom og tom med lik gradering og manglende diagnoser skal gi false") {
        smregisterClient.erEttersending(
            fnr = "fnr6",
            listOf(
                lagPeriode(
                    fom = LocalDate.of(2021, 2, 15),
                    tom = LocalDate.of(2021, 3, 15),
                ),
            ),
            diagnosekode = null,
            loggingMeta,
        ) shouldBeEqualTo false
    }

    test("test med merknader skal ikke være ettersending") {
        smregisterClient.erEttersending(
            fnr = "fnr7",
            listOf(
                lagPeriode(
                    fom = LocalDate.of(2021, 2, 15),
                    tom = LocalDate.of(2021, 3, 15),
                ),
            ),
            "L89",
            loggingMeta,
        ) shouldBeEqualTo false
    }
})
*/

fun sykmeldingRespons(
    fom: LocalDate,
    tom: LocalDate = fom.plusMonths(1),
    behandlingsutfallDTO: SmregisterBehandlingsutfall =
        SmregisterBehandlingsutfall(SmregisterRegelStatus.OK),
    behandletDato: LocalDate? = null,
    diagnosekode: String? = "L89",
    merknader: List<SmregisterMerknad>? = null,
    periodeType: SmregisterPeriodetype = SmregisterPeriodetype.AKTIVITET_IKKE_MULIG,
    gradert: SmregisterGradert? = null,
    sykmeldingStatus: SmregisterSykmeldingStatus,
) =
    listOf(
        SmregisterSykmelding(
            id = UUID.randomUUID().toString(),
            behandlingsutfall = behandlingsutfallDTO,
            sykmeldingsperioder =
                listOf(
                    SmregisterSykmeldingsperiode(
                        fom,
                        tom,
                        gradert,
                        periodeType,
                    ),
                ),
            behandletTidspunkt =
                if (behandletDato != null) {
                    OffsetDateTime.of(behandletDato.atStartOfDay(), ZoneOffset.UTC)
                } else {
                    OffsetDateTime.of(fom.atStartOfDay(), ZoneOffset.UTC)
                },
            medisinskVurdering =
                SmregisterMedisinskVurdering(
                    diagnosekode?.let { SmregisterDiagnose(diagnosekode) }
                ),
            merknader = merknader,
            sykmeldingStatus = sykmeldingStatus,
        ),
    )

private fun lagPeriode(fom: LocalDate, tom: LocalDate) =
    Periode(
        fom = fom,
        tom = tom,
        aktivitetIkkeMulig = AktivitetIkkeMulig(null, null),
        avventendeInnspillTilArbeidsgiver = null,
        behandlingsdager = null,
        gradert = null,
        reisetilskudd = false,
    )
