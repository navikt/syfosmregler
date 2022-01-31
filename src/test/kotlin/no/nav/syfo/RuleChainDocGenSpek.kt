package no.nav.syfo

import org.spekframework.spek2.Spek
import kotlin.reflect.KClass

object RuleChainDocGenSpek : Spek({
    fun <T : Annotation> Any.enumAnnotationValue(type: KClass<out T>, enumName: String): T? = if (javaClass.getField(enumName)?.isAnnotationPresent(type.java) == true) {
        javaClass.getField(enumName).getAnnotation(type.java)
    } else {
        null
    }

    /*describe("Generate docs for rule chains") {
        it("Generates a CSV file with rule chain") {
            val basePath = Paths.get("build", "reports")
            Files.createDirectories(basePath)
            val ruleCSV = arrayOf("Regel navn;Status;Regel ID;Beskrivelse;Tekst til bruker;Tekst til behandler").union(
                listOf<List<Rule<*>>>(ValidationRuleChain.values().toList(), PeriodLogicRuleChain.values().toList(), HPRRuleChain.values().toList(), LegesuspensjonRuleChain.values().toList(), SyketilfelleRuleChain.values().toList()).flatten()
                    .map { rule ->
                        val description = rule.enumAnnotationValue(Description::class, rule.name)?.description ?: ""
                        "${rule.name};${rule.status};${rule.ruleId ?: ""};$description;${rule.messageForUser};${rule.messageForSender}"
                    }
            )
            val csvFile = basePath.resolve("rules.csv")
            Files.write(csvFile, ruleCSV, Charsets.UTF_8)
        }
    }*/
})
