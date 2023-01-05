package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleMetadataSykmelding

class BegrunnelseKontaktDato(yes: RuleNode, no: RuleNode) : RuleNode(yes, no) {
    override fun evaluateRule(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding): Boolean {
        return sykmelding.kontaktMedPasient.kontaktDato != null || !sykmelding.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty()
    }
}
