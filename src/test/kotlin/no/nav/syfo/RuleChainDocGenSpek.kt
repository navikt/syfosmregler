package no.nav.syfo

import no.nav.syfo.rules.tmpRuleChain
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Files
import java.nio.file.Paths

object RuleChainDocGenSpek : Spek({
    describe("Generate docs for rule chains") {
        val basePath = Paths.get("build/doc")
        Files.createDirectories(basePath)
        arrayOf(
                tmpRuleChain
        ).forEach {
            it("Generates documentation for ${tmpRuleChain.name}") {
                val csvFile = basePath.resolve("${tmpRuleChain.name}-rules.csv")
                Files.write(
                        csvFile,
                        arrayOf("Rule name;Outcome type;Rule ID;Description").union(it.rules.map { "${it.name};${it.outcomeType};${it.outcomeType.ruleId};${it.description}" }),
                        Charsets.UTF_8
                )
            }
        }
    }
})
