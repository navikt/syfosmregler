package no.nav.syfo.rules.common

import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.services.RuleMetadataSykmelding

interface RuleExecution<T> {
    fun runRules(
        sykmelding: Sykmelding,
        ruleMetadata: RuleMetadataSykmelding
    ): Pair<TreeOutput<T, RuleResult>, Juridisk>
}
