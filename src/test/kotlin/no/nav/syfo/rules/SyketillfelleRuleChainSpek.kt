package no.nav.syfo.rules

import no.nav.syfo.RuleData
import no.nav.syfo.api.Oppfolgingstilfelle
import no.nav.syfo.api.Periode
import no.nav.syfo.generateKontaktMedPasient
import no.nav.syfo.generatePeriode
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.Sykmelding
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

object SyketillfelleRuleChainSpek : Spek({
    fun ruleData(
        healthInformation: Sykmelding,
        antallBrukteDager: Int = 10,
        oppbruktArbeidsgvierperiode: Boolean = true,
        arbeidsgiverPeriode: Periode? = Periode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(1))
    ): RuleData<Oppfolgingstilfelle?> = RuleData(healthInformation, Oppfolgingstilfelle(antallBrukteDager, oppbruktArbeidsgvierperiode, arbeidsgiverPeriode))

    describe("Testing validation rules and checking the rule outcomes") {

        it("Should check rule BACKDATED_MORE_THEN_8_DAYS_AND_UNDER_1_YEAR_BACKDATED, should trigger rule") {
            val healthInformation = generateSykmelding(perioder = listOf(
                    generatePeriode(
                            fom = LocalDate.of(2018, 1, 1),
                            tom = LocalDate.of(2018, 2, 1)
                    )),
                    kontaktMedPasient = generateKontaktMedPasient(kontaktDato = LocalDate.of(2018, 1, 9)))

            SyketillfelleRuleChain.BACKDATED_MORE_THEN_8_DAYS_AND_UNDER_1_YEAR_BACKDATED(ruleData(healthInformation, 10, true)) shouldEqual true
        }

        it("Should check rule BACKDATED_MORE_THEN_8_DAYS_AND_UNDER_1_YEAR_BACKDATED, should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                    perioder = listOf(
                            generatePeriode(
                                    fom = LocalDate.of(2018, 1, 1),
                                    tom = LocalDate.of(2018, 2, 1)
                            )
                    ),
                    kontaktMedPasient = generateKontaktMedPasient(kontaktDato = LocalDate.of(2018, 1, 8))
            )

            SyketillfelleRuleChain.BACKDATED_MORE_THEN_8_DAYS_AND_UNDER_1_YEAR_BACKDATED(ruleData(healthInformation, 10, false)) shouldEqual false
        }
    }
})
