package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleMetadataSykmelding
import no.nav.syfo.rules.containsLetters

class Begrunnelse(yes: RuleNode, no: RuleNode) : RuleNode(yes, no){
    override fun evaluateRule(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding): Boolean {
        val begrunnelse = sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt
        return begrunnelse?.let { containsLetters(it) && it.length > 16 } ?: false
    }
}