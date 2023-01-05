package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleMetadataSykmelding
import no.nav.syfo.sm.isICD10

class SpesialistHelsetjenesten(yes: RuleNode, no: RuleNode) : RuleNode(yes = yes, no = no) {
    override fun evaluateRule(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding): Boolean {
        return sykmelding.medisinskVurdering.hovedDiagnose?.isICD10() ?: false
    }
}