package no.nav.syfo.rules.tilbakedatering

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.KontaktMedPasient
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleMetadataSykmelding
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate

class TilbakedateringTest : FunSpec({
    context("Test tilbakedateringsregler mindre enn 9 dager") {
        test("Test tilbakedatering minder enn 9 dager, ikke tilbakedatert, Status OK") {
            val invalidStatus = StatusNode(Status.INVALID)
            val okStatus = StatusNode(Status.OK)
            val tilbakedatering = Tilbakedatering(yes = invalidStatus, no = okStatus)

            val sykmelding = generateSykmelding(
                fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay()
            )
            val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), false, false)
            val status = tilbakedatering.evaluate(sykmelding, sykmeldingMetadata)
            status shouldBeEqualTo Status.OK
        }

        test("Test tilbakedatering minder enn 9 dager, tilbakedatert, Status INVALID") {

            val invalidStatus = StatusNode(Status.INVALID)
            val okStatus = StatusNode(Status.OK)
            val begrunnelse = BegrunnelseKontaktDato(yes = okStatus, no = invalidStatus)
            val tilbakedatering = Tilbakedatering(yes = begrunnelse, no = okStatus)

            val sykmelding = generateSykmelding(
                fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 6).atStartOfDay(),
                kontaktMedPasient = KontaktMedPasient(LocalDate.now(), null)
            )
            val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), false, false)
            val status = tilbakedatering.evaluate(sykmelding, sykmeldingMetadata)

            Status.INVALID shouldBeEqualTo status
        }
    }
})

fun Sykmelding.toRuleMetadata() = RuleMetadata(
    signatureDate = signaturDato,
    receivedDate = signaturDato,
    behandletTidspunkt = behandletTidspunkt,
    patientPersonNumber = "1",
    rulesetVersion = null,
    legekontorOrgnr = null,
    tssid = null,
    avsenderFnr = "2",
    pasientFodselsdato = LocalDate.now()
)
