package no.nav.syfo

import no.nav.syfo.rules.HPRRuleChain
import no.nav.syfo.rules.PeriodLogicRuleChain
import no.nav.syfo.rules.TPSRuleChain
import no.nav.syfo.rules.ValidationRules
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.KClass

object RuleChainDocGenSpek : Spek({
    fun <T : Annotation> Any.enumAnnotationValue(type: KClass<out T>, enumName: String): T? = if (javaClass.getField(enumName)?.isAnnotationPresent(type.java) == true) {
            javaClass.getField(enumName).getAnnotation(type.java)
        } else {
            null
        }

    describe("Generate docs for rule chains") {
        val basePath = Paths.get("build/doc")
        Files.createDirectories(basePath)
        val ruleCSV = arrayOf("Rule name;status;Rule ID;Description").union(listOf<List<Rule<*>>>(ValidationRules.values().toList(), PeriodLogicRuleChain.values().toList(), TPSRuleChain.values().toList(), HPRRuleChain.values().toList()).flatten()
                .map { rule -> "${rule.name};${rule.status};${rule.ruleId ?: ""};${rule.enumAnnotationValue(Description::class, rule.name)?.description ?: ""}" })
        val csvFile = basePath.resolve("rules.csv")
        Files.write(csvFile, ruleCSV, Charsets.UTF_8)
    }
})
