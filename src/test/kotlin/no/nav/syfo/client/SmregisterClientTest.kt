package no.nav.syfo.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
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
import kotlinx.coroutines.runBlocking
import no.nav.syfo.LoggingMeta
import no.nav.syfo.azuread.v2.AzureAdV2Client
import no.nav.syfo.azuread.v2.AzureAdV2Token
import no.nav.syfo.model.AktivitetIkkeMulig
import no.nav.syfo.model.Periode
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.ServerSocket
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.TimeUnit

class SmregisterClientTest : Spek({
    val loggingMeta = LoggingMeta("", "", "", "")
    val accessTokenClientMock = mockk<AzureAdV2Client>()
    val httpClient = HttpClient(Apache) {
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
                    }
                }
            }
        }
    }.start()

    val smregisterClient = SmregisterClient(mockHttpServerUrl, accessTokenClientMock, "resourceId", httpClient)

    afterGroup {
        mockServer.stop(TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(1))
    }

    beforeEachTest {
        coEvery { accessTokenClientMock.getAccessToken(any()) } returns AzureAdV2Token("accessToken", OffsetDateTime.now().plusHours(1))
    }

    describe("Test av SmRegisterClient") {
        it("finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert er false hvis bruker ikke har andre sykmeldinger") {
            runBlocking {
                smregisterClient.finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert(
                    "fnr",
                    listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                    loggingMeta
                ) shouldBeEqualTo false
            }
        }
        it("finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert er false hvis bruker har sykmelding med annen fom") {
            runBlocking {
                smregisterClient.finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert(
                    "fnr2",
                    listOf(lagPeriode(fom = LocalDate.of(2021, 1, 15), tom = LocalDate.of(2021, 2, 15))),
                    loggingMeta
                ) shouldBeEqualTo false
            }
        }
        it("finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert er false hvis bruker har sykmelding med samme fom som er tilbakedatert") {
            runBlocking {
                smregisterClient.finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert(
                    "fnr3",
                    listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                    loggingMeta
                ) shouldBeEqualTo false
            }
        }
        it("finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert er true hvis bruker har sykmelding med samme fom som ikke er tilbakedatert") {
            runBlocking {
                smregisterClient.finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert(
                    "fnr2",
                    listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                    loggingMeta
                ) shouldBeEqualTo true
            }
        }
        it("finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert er true hvis bruker har sykmelding med samme fom som er tilbakedatert 7 dager") {
            runBlocking {
                smregisterClient.finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert(
                    "fnr4",
                    listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                    loggingMeta
                ) shouldBeEqualTo true
            }
        }
        it("finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert er false hvis bruker har avvist sykmelding med samme fom som ikke er tilbakedatert") {
            runBlocking {
                smregisterClient.finnesSykmeldingMedSammeFomSomIkkeErTilbakedatert(
                    "fnr5",
                    listOf(lagPeriode(fom = LocalDate.of(2021, 2, 15), tom = LocalDate.of(2021, 3, 15))),
                    loggingMeta
                ) shouldBeEqualTo false
            }
        }
    }
})

private fun sykmeldingRespons(
    fom: LocalDate,
    behandlingsutfallDTO: BehandlingsutfallDTO = BehandlingsutfallDTO(RegelStatusDTO.OK),
    behandletDato: LocalDate? = null
) = listOf(
    SykmeldingDTO(
        id = UUID.randomUUID().toString(),
        behandlingsutfall = behandlingsutfallDTO,
        sykmeldingsperioder = listOf(SykmeldingsperiodeDTO(fom, fom.plusMonths(1))),
        behandletTidspunkt = if (behandletDato != null) {
            OffsetDateTime.of(behandletDato.atStartOfDay(), ZoneOffset.UTC)
        } else {
            OffsetDateTime.of(fom.atStartOfDay(), ZoneOffset.UTC)
        }
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
