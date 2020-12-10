package no.nav.syfo.rules

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import no.nav.syfo.generateKontaktMedPasient
import no.nav.syfo.generateMedisinskVurdering
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.AnnenFraverGrunn
import no.nav.syfo.model.AnnenFraversArsak
import no.nav.syfo.model.KontaktMedPasient
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.sm.Diagnosekoder
import no.nav.syfo.toDiagnose
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SyketilfelleRuleChainSpek : Spek({

    describe("Testing validation rules and checking the rule outcomes") {
        fun ruleData(healthInformation: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding) =
                RuleData(healthInformation, ruleMetadataSykmelding)

        it("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, trigger regel pga kort begrunnelse") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 10),
                            tom = LocalDate.of(2019, 1, 20)
                    )
            ),
                    kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse")
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual true
        }

        it("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, trigger ikke regel pga lang begrunnelse") {
            val healthInformation = generateSykmelding(perioder = listOf(
                generatePeriode(
                    fom = LocalDate.of(2019, 1, 10),
                    tom = LocalDate.of(2019, 1, 20)
                )
            ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse som er lang nok")
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522"
                ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, should not trigger rule because of covid19") {
            val healthInformation = generateSykmelding(perioder = listOf(
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
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, should not trigger rule because of covid19") {
            val healthInformation = generateSykmelding(perioder = listOf(
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
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, should not trigger rule because of covid19") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2020, 2, 25),
                            tom = LocalDate.of(2020, 2, 28)
                    )
            ),
                    kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse"),
                    medisinskVurdering = generateMedisinskVurdering(annenFraversArsak = AnnenFraversArsak("foo", listOf(AnnenFraverGrunn.SMITTEFARE)))

            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, should trigger rule because FOM is before covid19 date") {
            val healthInformation = generateSykmelding(perioder = listOf(
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
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual true
        }

        it("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 7),
                            tom = LocalDate.of(2019, 1, 8)
                    )
            ),
                    kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "")
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 8), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 10),
                            tom = LocalDate.of(2019, 1, 20)
                    )
            ),
                    kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "")
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 20), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual true
        }

        it("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING, should not trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                generatePeriode(
                    fom = LocalDate.of(2019, 1, 10),
                    tom = LocalDate.of(2019, 1, 20)
                )
            ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse")
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 18), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522"
                ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 7),
                            tom = LocalDate.of(2019, 1, 8)
                    )
            ))

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 8), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE, should NOT trigger rule") {
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
                    receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.now(),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522"
                ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MER_ENN_8_DAGER_FORSTE_SYKMELDING_MED_BEGRUNNELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                generatePeriode(
                    fom = LocalDate.of(2019, 1, 14),
                    tom = LocalDate.of(2019, 1, 20)
                )
            ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = null)
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522"
                ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual true
        }

        it("Should check rule TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE, should not trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                generatePeriode(
                    fom = LocalDate.of(2019, 1, 14),
                    tom = LocalDate.of(2019, 1, 20)
                )
            ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse")
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 19), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522"
                ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE, should not trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                generatePeriode(
                    fom = LocalDate.of(2019, 1, 14),
                    tom = LocalDate.of(2019, 1, 20)
                )
            ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = LocalDate.of(2019, 1, 18), begrunnelseIkkeKontakt = "")
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 1, 18), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522"
                ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 10, 30),
                            tom = LocalDate.of(2019, 11, 17)
                    )
            ),
                    kontaktMedPasient = KontaktMedPasient(kontaktDato = LocalDate.of(2019, 10, 30), begrunnelseIkkeKontakt = null)
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 11, 4), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.TILBAKEDATERT_INNTIL_8_DAGER_UTEN_KONTAKTDATO_OG_BEGRUNNELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_FORLENGELSE_OVER_1_MND, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now().minusMonths(1).minusDays(1),
                            tom = LocalDate.now()
                    )
            ),
                    kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = ""))

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.now(),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_FORLENGELSE_OVER_1_MND(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual true
        }

        it("Should check rule TILBAKEDATERT_FORLENGELSE_OVER_1_MND, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now()
                    )
            ), kontaktMedPasient = generateKontaktMedPasient(
                    begrunnelseIkkeKontakt = "Noe tull skjedde, med sykmeldingen"
            )
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.now().minusMonths(1),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_FORLENGELSE_OVER_1_MND(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_FORLENGELSE_OVER_1_MND, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
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
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.now().minusMonths(2),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_FORLENGELSE_OVER_1_MND(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_FORLENGELSE_OVER_1_MND, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 10, 10),
                            tom = LocalDate.of(2019, 10, 20)
                    ))
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 11, 11), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_FORLENGELSE_OVER_1_MND(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual true
        }

        it("Should check rule TILBAKEDATERT_FORLENGELSE_OVER_1_MND, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 10, 8),
                            tom = LocalDate.of(2019, 10, 22)
                    ))
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 8, 20), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_FORLENGELSE_OVER_1_MND(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_FORLENGELSE_OVER_1_MND, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 10, 10),
                            tom = LocalDate.of(2019, 10, 20)
                    )),
                    kontaktMedPasient = generateKontaktMedPasient(
                            begrunnelseIkkeKontakt = "Noe tull skjedde, med sykmeldingen"
                    )
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.of(LocalDate.of(2019, 11, 15), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_FORLENGELSE_OVER_1_MND(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE, should trigger rule") {
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
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.now().plusDays(31),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual true
        }

        it("Should check rule TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE, should NOT trigger rule because of covid19") {
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
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.now().plusDays(31),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE, NOT should trigger rule") {
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
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.now().plusDays(29),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE, NOT should trigger rule") {
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
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.now(),
                            behandletTidspunkt = LocalDateTime.now().plusDays(30),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313",
                            tssid = "1355435",
                            avsenderFnr = "1345525522"
                    ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_MED_BEGRUNNELSE_FORLENGELSE(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_FORLENGELSE_UNDER_1_MND, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                generatePeriode(
                    fom = LocalDate.now(),
                    tom = LocalDate.now()
                )
            ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "")
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.now().plusDays(20), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522"
                ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_FORLENGELSE_UNDER_1_MND(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual true
        }

        it("Should check rule TILBAKEDATERT_FORLENGELSE_UNDER_1_MND, should not trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                generatePeriode(
                    fom = LocalDate.now(),
                    tom = LocalDate.now()
                )
            ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = null, begrunnelseIkkeKontakt = "Begrunnelse")
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.now().plusDays(20), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522"
                ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_FORLENGELSE_UNDER_1_MND(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }

        it("Should check rule TILBAKEDATERT_FORLENGELSE_UNDER_1_MND, should not trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                generatePeriode(
                    fom = LocalDate.now(),
                    tom = LocalDate.now()
                )
            ),
                kontaktMedPasient = KontaktMedPasient(kontaktDato = LocalDate.of(2019, 1, 18), begrunnelseIkkeKontakt = "")
            )

            val ruleMetadataSykmelding = RuleMetadataSykmelding(
                ruleMetadata = RuleMetadata(
                    receivedDate = LocalDateTime.now(),
                    signatureDate = LocalDateTime.now(),
                    behandletTidspunkt = LocalDateTime.of(LocalDate.now().plusDays(20), LocalTime.NOON),
                    patientPersonNumber = "1232345244",
                    rulesetVersion = "2",
                    legekontorOrgnr = "12313",
                    tssid = "1355435",
                    avsenderFnr = "1345525522"
                ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.TILBAKEDATERT_FORLENGELSE_UNDER_1_MND(ruleData(healthInformation, ruleMetadataSykmelding)) shouldEqual false
        }
    }
})
