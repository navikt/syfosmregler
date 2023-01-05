package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleMetadataSykmelding

abstract class RuleNode(val yes: RuleNode?, val no: RuleNode? = null, val status: Status? = null) {
    fun evaluate(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding): Status {
        return when (isStatusNode()) {
            true -> {
                requireNotNull(status)
                status
            }
            else -> evaluateChilds(sykmelding, ruleMetadataSykmelding)
        }
    }

    private fun evaluateChilds(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding): Status {
        requireNotNull(yes)
        requireNotNull(no)
        return when (evaluateRule(sykmelding, ruleMetadataSykmelding)) {
            true -> yes.evaluate(sykmelding, ruleMetadataSykmelding)
            else -> no.evaluate(sykmelding, ruleMetadataSykmelding)
        }
    }

    fun isStatusNode(): Boolean {
        return yes == null && no == null
    }

    protected abstract fun evaluateRule(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding): Boolean
}
