package no.nav.syfo.rules.tilbakedatering

import ch.qos.logback.core.status.OnFileStatusListener
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleMetadataSykmelding

class StatusNode(status: Status) : RuleNode(null, null, status) {
    override fun evaluateRule(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding): Boolean {
        return true
    }

}