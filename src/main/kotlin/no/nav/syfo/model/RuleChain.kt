package no.nav.syfo.model

interface RuleChain {
    val rules: List<RuleThingy<*>>

    fun getRuleByName(name: String): RuleThingy<*> {
        return rules.find { name == it.name } ?: throw IllegalArgumentException("Unable to find with name $name")
    }

    fun executeRules(): List<RuleResult<*>> {
        return rules.map { it.executeRule() }
    }
}
