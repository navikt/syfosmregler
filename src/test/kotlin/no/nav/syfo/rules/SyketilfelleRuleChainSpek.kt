package no.nav.syfo.rules

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.generateKontaktMedPasient
import no.nav.syfo.generateMedisinskVurdering
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.AnnenFraverGrunn
import no.nav.syfo.model.AnnenFraversArsak
import no.nav.syfo.model.KontaktMedPasient
import no.nav.syfo.model.MeldingTilNAV
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.SporsmalSvar
import no.nav.syfo.model.SvarRestriksjon
import no.nav.syfo.questions.QuestionGroup
import no.nav.syfo.questions.QuestionId
import no.nav.syfo.sm.Diagnosekoder
import no.nav.syfo.toDiagnose
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SyketilfelleRuleChainSpek : FunSpec({
    context("Testing validation rules and checking the rule outcomes") {
        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, trigger regel pga kort begrunnelse") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 10),
                        tom = LocalDate.of(2019, 1, 20)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse")
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo true
        }
        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, trigger ikke regel pga lang begrunnelse") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 10),
                        tom = LocalDate.of(2019, 1, 20)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(
                    kontaktDato = null,
                    begrunnelseIkkeKontakt = "Begrunnelse som er lang nok"
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, should not trigger rule because of covid19") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2020, 2, 25),
                        tom = LocalDate.of(2020, 2, 28)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse"),
                medisinskVurdering = generateMedisinskVurdering(hovedDiagnose = Diagnosekoder.icpc2["R991"]!!.toDiagnose())

            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, should not trigger rule because of covid19") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2020, 2, 25),
                        tom = LocalDate.of(2020, 2, 28)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse"),
                medisinskVurdering = generateMedisinskVurdering(hovedDiagnose = Diagnosekoder.icd10["U071"]!!.toDiagnose())

            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, should not trigger rule because of covid19") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2020, 2, 25),
                        tom = LocalDate.of(2020, 2, 28)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse"),
                medisinskVurdering = generateMedisinskVurdering(
                    annenFraversArsak = AnnenFraversArsak(
                        "foo",
                        listOf(AnnenFraverGrunn.SMITTEFARE)
                    )
                )

            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, should trigger rule because FOM is before covid19 date") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 10),
                        tom = LocalDate.of(2019, 1, 20)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse"),
                medisinskVurdering = generateMedisinskVurdering(hovedDiagnose = Diagnosekoder.icpc2["R991"]!!.toDiagnose())

            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo true
        }
        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, trigger regel fordi FOM er etter sluttdato for covid19") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 1, 20)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse"),
                medisinskVurdering = generateMedisinskVurdering(hovedDiagnose = Diagnosekoder.icpc2["R991"]!!.toDiagnose())

            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2023, 1, 10), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo true
        }
        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, trigger ikke regel pga ICD-10-diagnose (spesialisthelsetjenesten)") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 10),
                        tom = LocalDate.of(2019, 1, 20)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse"),
                medisinskVurdering = generateMedisinskVurdering(hovedDiagnose = Diagnosekoder.icd10["S821"]!!.toDiagnose())
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 7),
                        tom = LocalDate.of(2019, 1, 8)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "")
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 8), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, trigger ikke regel pga ettersending") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 10),
                        tom = LocalDate.of(2019, 1, 20)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse")
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = true
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 10),
                        tom = LocalDate.of(2019, 1, 20)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "")
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 20), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING")
                .executeRule().result shouldBeEqualTo true
        }
        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING, should not trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 10),
                        tom = LocalDate.of(2019, 1, 20)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse")
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 18), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 7),
                        tom = LocalDate.of(2019, 1, 8)
                    )
                )
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 8), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = null
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING, should not trigger when begrunnelseIkkeKontakt is not empty and date is not tilbakedatert") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2022, 3, 28),
                        tom = LocalDate.of(2022, 3, 29)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = ".")
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.of(2022, 3, 30, 16, 10, 10),
                    receivedDate = LocalDateTime.of(2022, 3, 30, 12, 0, 0),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2022, 3, 30), LocalTime.MIDNIGHT),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = null
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING")
                .executeRule().result shouldBeEqualTo false
        }

        test("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING trigges fordi begrunnelsen ikke inneholder bokstaver") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 10),
                        tom = LocalDate.of(2019, 1, 20)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "123.4")
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 20), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING")
                .executeRule().result shouldBeEqualTo true
        }
        test("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now().minusDays(7),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "Noe tull skjedde, med sykmeldingen"
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now(),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = null
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 14),
                        tom = LocalDate.of(2019, 1, 20)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = null)
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo true
        }
        test("Should check rule TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE, should not trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 14),
                        tom = LocalDate.of(2019, 1, 20)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse")
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE, should not trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 1, 14),
                        tom = LocalDate.of(2019, 1, 20)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(
                    kontaktDato = LocalDate.of(2019, 1, 18),
                    begrunnelseIkkeKontakt = ""
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 18), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 10, 30),
                        tom = LocalDate.of(2019, 11, 17)
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(
                    kontaktDato = LocalDate.of(2019, 10, 30),
                    begrunnelseIkkeKontakt = null
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 11, 4), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = true,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_FORLENGELSE_OVER_1_MND, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now().minusMonths(1).minusDays(1),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "")
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now(),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_FORLENGELSE_OVER_1_MND")
                .executeRule().result shouldBeEqualTo true
        }
        test("Should check rule TILBAKEDATERT_FORLENGELSE_OVER_1_MND, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "Noe tull skjedde, med sykmeldingen"
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().minusMonths(1),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_FORLENGELSE_OVER_1_MND")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_FORLENGELSE_OVER_1_MND, should NOT trigger when rule ICD-10") {
            val healthInformation = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(hovedDiagnose = Diagnosekoder.icd10["S821"]!!.toDiagnose()),
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now().minusMonths(1).minusDays(1),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "")
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now(),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_FORLENGELSE_OVER_1_MND")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule TILBAKEDATERT_FORLENGELSE_OVER_1_MND, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "Noe tull skjedde, med sykmeldingen"
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().minusMonths(2),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_FORLENGELSE_OVER_1_MND")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_FORLENGELSE_OVER_1_MND, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 10, 10),
                        tom = LocalDate.of(2019, 10, 20)
                    )
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 11, 11), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_FORLENGELSE_OVER_1_MND")
                .executeRule().result shouldBeEqualTo true
        }
        test("Should check rule TILBAKEDATERT_FORLENGELSE_OVER_1_MND, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 10, 8),
                        tom = LocalDate.of(2019, 10, 22)
                    )
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 8, 20), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = null
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_FORLENGELSE_OVER_1_MND")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_FORLENGELSE_OVER_1_MND, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.of(2019, 10, 10),
                        tom = LocalDate.of(2019, 10, 20)
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "Noe tull skjedde, med sykmeldingen"
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 11, 15), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_FORLENGELSE_OVER_1_MND")
                .executeRule().result shouldBeEqualTo false
        }
        test(
            "TILBAKEDATERT_MED_UTILSTREKKELIG_BEGRUNNELSE_FORLENGELSE trigges hvis begrunnelse ikke inneholder" +
                " bokstaver, og utd.oppl og melding til NAV mangler"
        ) {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "6.3"
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().plusDays(31),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MED_UTILSTREKKELIG_BEGRUNNELSE_FORLENGELSE")
                .executeRule().result shouldBeEqualTo true
        }
        test("TILBAKEDATERT_MED_UTILSTREKKELIG_BEGRUNNELSE_FORLENGELSE trigges ikke hvis begrunnelse inneholder bokstaver") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "Fikk ikke time"
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().plusDays(31),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MED_UTILSTREKKELIG_BEGRUNNELSE_FORLENGELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("TILBAKEDATERT_MED_UTILSTREKKELIG_BEGRUNNELSE_FORLENGELSE trigges ikke hvis utdypende opplysninger er satt") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "6.3"
                ),
                utdypendeOpplysninger = mapOf(
                    QuestionGroup.GROUP_6_2.spmGruppeId to mapOf(
                        QuestionId.ID_6_2_1.spmId to SporsmalSvar(
                            "Pasienten er syk",
                            "svar",
                            listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)
                        )
                    )
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().plusDays(31),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MED_UTILSTREKKELIG_BEGRUNNELSE_FORLENGELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("TILBAKEDATERT_MED_UTILSTREKKELIG_BEGRUNNELSE_FORLENGELSE trigges ikke hvis melding til NAV er satt") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "6.3"
                ),
                meldingTilNAV = MeldingTilNAV(false, "Her er begrunnelsen")
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().plusDays(31),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MED_UTILSTREKKELIG_BEGRUNNELSE_FORLENGELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now().minusDays(31),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "Noe tull skjedde, med sykmeldingen"
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().plusDays(31),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE")
                .executeRule().result shouldBeEqualTo true
        }
        test("Should check rule TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE, should NOT trigger rule because of covid19") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "Noe tull skjedde, med sykmeldingen"
                ),
                medisinskVurdering = generateMedisinskVurdering(hovedDiagnose = Diagnosekoder.icpc2["R991"]!!.toDiagnose())
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().minusDays(31),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule TILBAKEDATERT_UTEN_BEGRUNNELSE_FORLENGELSE_ICD_10, should trigger rule") {
            val healthInformation = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(hovedDiagnose = Diagnosekoder.icd10["S821"]!!.toDiagnose()),
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    kontaktDato = null
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().plusDays(31),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_UTEN_BEGRUNNELSE_FORLENGELSE_ICD_10")
                .executeRule().result shouldBeEqualTo true
        }
        test("Should check rule TILBAKEDATERT_UTEN_BEGRUNNELSE_FORLENGELSE_ICD_10, should NOT trigger rule because of covid19") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    kontaktDato = null
                ),
                medisinskVurdering = generateMedisinskVurdering(hovedDiagnose = Diagnosekoder.icpc2["Z09"]!!.toDiagnose())
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().plusDays(31),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_UTEN_BEGRUNNELSE_FORLENGELSE_ICD_10")
                .executeRule().result shouldBeEqualTo false
        }

        test("Should check rule TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE, trigger ikke regel pga ICD-10-diagnose (spesialisthelsetjenesten)") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "Noe tull skjedde med sykmeldingen"
                ),
                medisinskVurdering = generateMedisinskVurdering(hovedDiagnose = Diagnosekoder.icd10["S821"]!!.toDiagnose())
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().plusDays(31),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE, trigger ikke regel pga ettersending") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "Noe tull skjedde med sykmeldingen"
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().plusDays(31),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = true
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE, NOT should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "Noe tull skjedde, med sykmeldingen"
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().plusDays(29),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE, NOT should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().plusDays(30),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE trigges ikke hvis begrunnelse ikke inneholder bokstaver, og utd.oppl og melding til NAV mangler") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "6.3"
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now().plusDays(31),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE trigges hvis sykmeldingsperioden er over 31 dager") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now().minusDays(32),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "Viktig møte på jobb"
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now(),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE")
                .executeRule().result shouldBeEqualTo true
        }
        test("TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE trigges ikke hvis sykmeldingsperioden er under 31 dager") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now().minusDays(30),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "Viktig møte på jobb"
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now(),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_FORLENGELSE_UNDER_1_MND, should trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "")
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.now().plusDays(20), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_FORLENGELSE_UNDER_1_MND")
                .executeRule().result shouldBeEqualTo true
        }
        test("Should check rule TILBAKEDATERT_FORLENGELSE_UNDER_1_MND, should not trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse")
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.now().plusDays(20), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_FORLENGELSE_UNDER_1_MND")
                .executeRule().result shouldBeEqualTo false
        }
        test("Should check rule TILBAKEDATERT_FORLENGELSE_UNDER_1_MND, should not trigger rule") {
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(
                    kontaktDato = LocalDate.of(2019, 1, 18),
                    begrunnelseIkkeKontakt = ""
                )
            )
            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.now().plusDays(20), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_FORLENGELSE_UNDER_1_MND")
                .executeRule().result shouldBeEqualTo false
        }
        test("TILBAKEDATERT_FORLENGELSE_UNDER_1_MND trigges hvis begrunnelse ikke inneholder bokstaver") {
            val begrunnelseIkkeKontakt = "63"
            val healthInformation = generateSykmelding(
                perioder = listOf(
                    generatePeriode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now()
                    )
                ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = begrunnelseIkkeKontakt)
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    signatureDate = LocalDateTime.now(),
                    receivedDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.now().plusDays(20), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522",
                    pasientFodselsdato = LocalDate.now()
                ),
                erNyttSyketilfelle = false,
                erEttersendingAvTidligereSykmelding = false
            )

            SyketilfelleRuleChain(
                healthInformation,
                ruleMetadataSykmelding
            ).getRuleByName("TILBAKEDATERT_FORLENGELSE_UNDER_1_MND")
                .executeRule().result shouldBeEqualTo true
        }
    }
})
