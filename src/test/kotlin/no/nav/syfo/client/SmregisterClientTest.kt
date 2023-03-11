package no.nav.syfo.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FunSpec
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.accept
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.azuread.v2.AzureAdV2Token
import no.nav.syfo.generateGradert
import no.nav.syfo.model.AktivitetIkkeMulig
import no.nav.syfo.model.Gradert
import no.nav.syfo.model.Periode
import no.nav.syfo.utils.LoggingMeta
import org.amshove.kluent.shouldBeEqualTo
import java.net.ServerSocket
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.TimeUnit

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
                                behandletDato = LocalDate.of(2021, 3, 1)
                            )
                        )

                        "fnr4" -> call.respond(
                            HttpStatusCode.OK,
                            sykmeldingRespons(
                                fom = LocalDate.of(2021, 2, 15),
                                behandletDato = LocalDate.of(2021, 2, 22)
                            )
                        )

                        "fnr5" -> call.respond(
                            HttpStatusCode.OK,
                            sykmeldingRespons(
                                fom = LocalDate.of(2021, 2, 15),
                                behandlingsutfallDTO = BehandlingsutfallDTO(RegelStatusDTO.INVALID)
                            )
                        )

                        "fnr6" -> call.respond(HttpStatusCode.OK, sykmeldingRespons(fom = LocalDate.of(2021, 2, 15), diagnosekode = null))
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
            OffsetDateTime.now().plusHours(1)
        )
    }

    context("Test av SmRegisterClient") {
        test("False hvis bruker ikke har andre sykmeldinger") {
            smregisterClient.harOverlappendeSykmelding(
                "fnr",
                listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                "L89",
                loggingMeta
            ) shouldBeEqualTo false
        }
        test("False hvis bruker har sykmelding med annen fom") {
            smregisterClient.harOverlappendeSykmelding(
                "fnr2",
                listOf(lagPeriode(fom = LocalDate.of(2021, 1, 15), tom = LocalDate.of(2021, 2, 15))),
                "L89",
                loggingMeta
            ) shouldBeEqualTo false
        }
        test("False hvis bruker har sykmelding med samme fom som er tilbakedatert") {
            smregisterClient.harOverlappendeSykmelding(
                "fnr3",
                listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                "L89",
                loggingMeta
            ) shouldBeEqualTo false
        }
        test("True hvis bruker har sykmelding med samme fom som ikke er tilbakedatert") {
            smregisterClient.harOverlappendeSykmelding(
                "fnr2",
                listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                "L89",
                loggingMeta
            ) shouldBeEqualTo true
        }
        test("True hvis bruker har sykmelding med samme fom som er tilbakedatert 7 dager") {
            smregisterClient.harOverlappendeSykmelding(
                "fnr4",
                listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                "L89",
                loggingMeta
            ) shouldBeEqualTo true
        }
        test("False hvis bruker har avvist sykmelding med samme fom som ikke er tilbakedatert") {
            smregisterClient.harOverlappendeSykmelding(
                "fnr5",
                listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                "L89",
                loggingMeta
            ) shouldBeEqualTo false
        }
        test("False hvis bruker har sykmelding med fom 2 dager før ny fom") {
            smregisterClient.harOverlappendeSykmelding(
                "fnr2",
                listOf(lagPeriode(fom = LocalDate.of(2021, 2, 13), tom = LocalDate.of(2021, 2, 15))),
                "L89",
                loggingMeta
            ) shouldBeEqualTo false
        }
        test("False hvis bruker har sykmelding med tom 2 dager etter ny tom") {
            smregisterClient.harOverlappendeSykmelding(
                "fnr2",
                listOf(lagPeriode(fom = LocalDate.of(2021, 3, 15), tom = LocalDate.of(2021, 3, 17))),
                "L89",
                loggingMeta
            ) shouldBeEqualTo false
        }
        test("False hvis bruker har sykmelding med fom 2 dager før ny fom men annen diagnose") {
            smregisterClient.harOverlappendeSykmelding(
                "fnr2",
                listOf(lagPeriode(fom = LocalDate.of(2021, 2, 13), tom = LocalDate.of(2021, 2, 15))),
                "L87",
                loggingMeta
            ) shouldBeEqualTo false
        }
        test("False hvis bruker har sykmelding med fom 2 dager før ny fom men annen grad") {
            smregisterClient.harOverlappendeSykmelding(
                "fnr2",
                listOf(
                    Periode(
                        fom = LocalDate.of(2021, 2, 13),
                        tom = LocalDate.of(2021, 2, 15),
                        aktivitetIkkeMulig = null,
                        avventendeInnspillTilArbeidsgiver = null,
                        behandlingsdager = null,
                        gradert = Gradert(false, 50),
                        reisetilskudd = false
                    )
                ),
                "L89",
                loggingMeta
            ) shouldBeEqualTo false
        }
    }

    context("Test av ny regel") {
        test("Test av overlappende datoer, skal returnere false når periodene ikke stemmer") {
            smregisterClient.harOverlappendeSykmelding(
                fnr = "fnr2",
                listOf(
                    Periode(
                        fom = LocalDate.of(2021, 2, 13),
                        tom = LocalDate.of(2021, 2, 15),
                        aktivitetIkkeMulig = null,
                        avventendeInnspillTilArbeidsgiver = null,
                        behandlingsdager = null,
                        gradert = Gradert(false, 50),
                        reisetilskudd = false
                    )
                ),
                "L89",
                loggingMeta
            ) shouldBeEqualTo false
        }

        test("Test med samme fom og tom med ulik gradering skal gi false") {
            smregisterClient.harOverlappendeSykmelding(
                fnr = "fnr2",
                listOf(
                    Periode(
                        fom = LocalDate.of(2021, 2, 15),
                        tom = LocalDate.of(2021, 3, 15),
                        aktivitetIkkeMulig = null,
                        avventendeInnspillTilArbeidsgiver = null,
                        behandlingsdager = null,
                        gradert = Gradert(false, 50),
                        reisetilskudd = false
                    )
                ),
                "L89",
                loggingMeta
            ) shouldBeEqualTo false
        }

        test("Test med samme fom og tom med lik gradering skal gi true") {
            smregisterClient.harOverlappendeSykmelding(
                fnr = "fnr2",
                listOf(
                    lagPeriode(
                        fom = LocalDate.of(2021, 2, 15),
                        tom = LocalDate.of(2021, 3, 15)
                    )
                ),
                "L89",
                loggingMeta
            ) shouldBeEqualTo true
        }
    }

    test("Test med samme fom og tom med lik gradering og ulik diagnose skal gi false") {
        smregisterClient.harOverlappendeSykmelding(
            fnr = "fnr2",
            listOf(
                lagPeriode(
                    fom = LocalDate.of(2021, 2, 15),
                    tom = LocalDate.of(2021, 3, 15)
                )
            ),
            "LA8PV",
            loggingMeta
        ) shouldBeEqualTo false
    }

    test("Test med samme fom og tom med ulik gradering og lik diagnose skal gi false") {
        smregisterClient.harOverlappendeSykmelding(
            fnr = "fnr2",
            listOf(
                lagPeriode(
                    fom = LocalDate.of(2021, 2, 15),
                    tom = LocalDate.of(2021, 3, 15)
                ).copy(gradert = generateGradert(false, 37))
            ),
            "L89",
            loggingMeta
        ) shouldBeEqualTo false
    }

    test("Test med samme fom og tom med lik gradering og manglende diagnoser skal gi false") {
        smregisterClient.harOverlappendeSykmelding(
            fnr = "fnr6",
            listOf(
                lagPeriode(
                    fom = LocalDate.of(2021, 2, 15),
                    tom = LocalDate.of(2021, 3, 15)
                )
            ),
            diagnosekode = null,
            loggingMeta
        ) shouldBeEqualTo false
    }
})

private fun sykmeldingRespons(
    fom: LocalDate,
    behandlingsutfallDTO: BehandlingsutfallDTO = BehandlingsutfallDTO(RegelStatusDTO.OK),
    behandletDato: LocalDate? = null,
    diagnosekode: String? = "L89"
) = listOf(
    SykmeldingDTO(
        id = UUID.randomUUID().toString(),
        behandlingsutfall = behandlingsutfallDTO,
        sykmeldingsperioder = listOf(
            SykmeldingsperiodeDTO(
                fom,
                fom.plusMonths(1),
                null,
                PeriodetypeDTO.AKTIVITET_IKKE_MULIG
            )
        ),
        behandletTidspunkt = if (behandletDato != null) {
            OffsetDateTime.of(behandletDato.atStartOfDay(), ZoneOffset.UTC)
        } else {
            OffsetDateTime.of(fom.atStartOfDay(), ZoneOffset.UTC)
        },
        medisinskVurdering = MedisinskVurderingDTO(diagnosekode?.let { DiagnoseDTO(diagnosekode) })
    )
)

private fun lagPeriode(fom: LocalDate, tom: LocalDate) =
    Periode(
        fom = fom,
        tom = tom,
        aktivitetIkkeMulig = AktivitetIkkeMulig(null, null),
        avventendeInnspillTilArbeidsgiver = null,
        behandlingsdager = null,
        gradert = null,
        reisetilskudd = false
    )
