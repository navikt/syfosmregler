package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleMetadataSykmelding
import no.nav.syfo.rules.sortedFOMDate
import no.nav.syfo.rules.sortedTOMDate
import java.time.temporal.ChronoUnit

class ArbeigsgiverPeriode(yes: RuleNode, no: RuleNode) : RuleNode(yes, no) {
    override fun evaluateRule(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding): Boolean {
        val fom = sykmelding.perioder.sortedFOMDate().first()
        val tom = sykmelding.perioder.sortedTOMDate().last()

        return ChronoUnit.DAYS.between(fom, tom) < 16
    }
}
