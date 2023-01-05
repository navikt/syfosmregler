package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleMetadataSykmelding
import no.nav.syfo.rules.sortedFOMDate

class Tilbakedatering(yes: RuleNode, no: RuleNode) : RuleNode(yes, no) {
    override fun evaluateRule(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding): Boolean {
        val fom = sykmelding.perioder.sortedFOMDate().first()
        val behandletTidspunkt = sykmelding.behandletTidspunkt.toLocalDate()

        return behandletTidspunkt.isAfter(fom.plusDays(3))
    }
}
class TilbakedateringIntill8Dager(yes: RuleNode, no: RuleNode) : RuleNode(yes, no) {
    override fun evaluateRule(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding): Boolean {
        val fom = sykmelding.perioder.sortedFOMDate().first()
        val behandletTidspunkt = sykmelding.behandletTidspunkt.toLocalDate()

        return behandletTidspunkt.isBefore(fom.plusDays(9))
    }
}

class TilbakedateringIntill30Dager(yes: RuleNode, no: RuleNode) : RuleNode(yes, no) {
    override fun evaluateRule(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding): Boolean {
        val fom = sykmelding.perioder.sortedFOMDate().first()
        val behandletTidspunkt = sykmelding.behandletTidspunkt.toLocalDate()

        return behandletTidspunkt.isBefore(fom.plusDays(31))
    }
}
