package no.nav.syfo.rules

import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Sykmelding
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object SyketilfelleRuleChainSpek : Spek({

    describe("Testing validation rules and checking the rule outcomes") {
        fun ruleData(healthInformation: Sykmelding, ruleMetadataAndForstegangsSykemelding: RuleMetadataAndForstegangsSykemelding) =
                RuleData(healthInformation, ruleMetadataAndForstegangsSykemelding)

        it("Should check rule BACKDATED_UP_TO_8_DAYS_FIRST_SICK_LAVE, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 7),
                            tom = LocalDate.of(2019, 1, 8)
                    )
            ))

            val ruleMetadataAndForstegangsSykemelding = RuleMetadataAndForstegangsSykemelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.of(LocalDate.of(2019, 1, 8), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313"
                    ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.BACKDATED_UP_TO_8_DAYS_FIRST_SICK_LAVE(ruleData(healthInformation, ruleMetadataAndForstegangsSykemelding)) shouldEqual true
        }

        it("Should check rule BACKDATED_UP_TO_8_DAYS_FIRST_SICK_LAVE, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 7),
                            tom = LocalDate.of(2019, 1, 8)
                    )
            ))

            val ruleMetadataAndForstegangsSykemelding = RuleMetadataAndForstegangsSykemelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.of(LocalDate.of(2019, 1, 8), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313"
                    ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.BACKDATED_UP_TO_8_DAYS_FIRST_SICK_LAVE(ruleData(healthInformation, ruleMetadataAndForstegangsSykemelding)) shouldEqual false
        }

        it("Should check rule BACKDATED_MORE_THEN_8_DAYS_FIRST_SICK, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 10),
                            tom = LocalDate.of(2019, 1, 20)
                    )
            ))

            val ruleMetadataAndForstegangsSykemelding = RuleMetadataAndForstegangsSykemelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.of(LocalDate.of(2019, 1, 18), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313"
                    ), erNyttSyketilfelle = true
            )

            SyketilfelleRuleChain.BACKDATED_MORE_THEN_8_DAYS_FIRST_SICK(ruleData(healthInformation, ruleMetadataAndForstegangsSykemelding)) shouldEqual true
        }

        it("Should check rule BACKDATED_MORE_THEN_8_DAYS_FIRST_SICK, should NOT trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2019, 1, 7),
                            tom = LocalDate.of(2019, 1, 8)
                    )
            ))

            val ruleMetadataAndForstegangsSykemelding = RuleMetadataAndForstegangsSykemelding(
                    ruleMetadata = RuleMetadata(
                            receivedDate = LocalDateTime.now(),
                            signatureDate = LocalDateTime.of(LocalDate.of(2019, 1, 8), LocalTime.NOON),
                            patientPersonNumber = "1232345244",
                            rulesetVersion = "2",
                            legekontorOrgnr = "12313"
                    ), erNyttSyketilfelle = false
            )

            SyketilfelleRuleChain.BACKDATED_MORE_THEN_8_DAYS_FIRST_SICK(ruleData(healthInformation, ruleMetadataAndForstegangsSykemelding)) shouldEqual false
        }
    }
})
