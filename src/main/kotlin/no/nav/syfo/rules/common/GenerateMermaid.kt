package no.nav.syfo.rules.common

import no.nav.syfo.model.Status
import no.nav.syfo.rules.arbeidsuforhet.arbeidsuforhetRuleTree
import no.nav.syfo.rules.dsl.ResultNode
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.TreeNode
import no.nav.syfo.rules.hpr.hprRuleTree
import no.nav.syfo.rules.legesuspensjon.legeSuspensjonRuleTree
import no.nav.syfo.rules.patientunder13.patientAgeUnder13RuleTree
import no.nav.syfo.rules.periode.periodeRuleTree
import no.nav.syfo.rules.periodvalidering.periodLogicRuleTree
import no.nav.syfo.rules.tilbakedatering.tilbakedateringRuleTree
import no.nav.syfo.rules.validation.validationRuleTree

fun main() {
    val ruleTrees =
        listOf(
            "Lege suspensjon" to legeSuspensjonRuleTree,
            "Validation" to validationRuleTree,
            "Periode validering" to periodLogicRuleTree,
            "HPR" to hprRuleTree,
            "Arbeidsuforhet" to arbeidsuforhetRuleTree,
            "Pasient under 13" to patientAgeUnder13RuleTree,
            "Periode" to periodeRuleTree,
            "Tilbakedatering" to tilbakedateringRuleTree,
        )

    ruleTrees.forEachIndexed { idx, (name, ruleTree) -> // add index to differentiate each loop
        val builder = StringBuilder()
        builder.append("## $idx. $name\n\n") // section headers with added index number

        // separator
        builder.append("---\n\n")
        val treeStringBuilder = StringBuilder()
        val juridiskHenvisninger = mutableListOf<Juridisk>()
        ruleTree.traverseTree(
            treeStringBuilder,
            thisNodeKey = "root",
            nodeKey = "root",
            juridiskHenvisninger
        )

        val henvisninger =
            juridiskHenvisninger.filterIsInstance<MedJuridisk>().distinctBy {
                it.juridiskHenvisning.paragraf
            }

        if (henvisninger.size > 1) {
            throw RuntimeException(
                "Juridisk henvisning has more than one paragraphs: ${henvisninger.joinToString { it.juridiskHenvisning.paragraf }}"
            )
        }

        val henvisning = henvisninger.singleOrNull()

        if (henvisning != null) {
            builder.append("- ### Juridisk Henvisning:\n")
            henvisning.juridiskHenvisning.lovverk.let { builder.append("  - **Lovverk**: $it\n") }
            henvisning.juridiskHenvisning.paragraf.let { builder.append("  - **Paragraf**: $it\n") }
        }
        builder.append("```mermaid\n")
        builder.append("graph TD\n")
        builder.append(treeStringBuilder)
        builder.append("    classDef ok fill:#c3ff91,stroke:#004a00,color: black;\n")
        builder.append("    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;\n")
        builder.append("    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;\n")
        builder.append("```\n\n")

        println(builder.toString())
    }
}

private fun <T> TreeNode<T, RuleResult>.traverseTree(
    builder: StringBuilder,
    thisNodeKey: String,
    nodeKey: String,
    juridiskHenvisninger: MutableList<Juridisk>,
) {
    when (this) {
        is ResultNode -> {
            // Is handled by parent node
            return
        }
        is RuleNode -> {
            val currentNodeKey = "${nodeKey}_$rule"
            if (yes is ResultNode) {
                val childResult = (yes as ResultNode<T, RuleResult>).result
                val childKey = "${currentNodeKey}_${childResult.status}"
                juridiskHenvisninger.add(childResult.juridisk)
                val result = "${childResult.status}\n${genererLovhenvisning(childResult.juridisk)}"
                builder.append(
                    "    $thisNodeKey($rule) -->|Yes| $childKey($result)${getStyle(childResult.status)}\n"
                )
            } else {
                val childRule = (yes as RuleNode<T, RuleResult>).rule
                val childKey = "${currentNodeKey}_$childRule"
                builder.append("    $thisNodeKey($rule) -->|Yes| $childKey($childRule)\n")
                yes.traverseTree(builder, childKey, currentNodeKey, juridiskHenvisninger)
            }
            if (no is ResultNode) {
                val childResult = (no as ResultNode<T, RuleResult>).result
                juridiskHenvisninger.add(childResult.juridisk)
                val childKey = "${currentNodeKey}_${childResult.status}"
                val result =
                    "${childResult.status} <br/>" + genererLovhenvisning(childResult.juridisk)
                builder.append(
                    "    $thisNodeKey($rule) -->|No| $childKey(${result})${getStyle(childResult.status)}\n"
                )
            } else {
                val childRule = (no as RuleNode<T, RuleResult>).rule
                val childKey = "${currentNodeKey}_$childRule"
                builder.append("    $thisNodeKey($rule) -->|No| $childKey($childRule)\n")
                no.traverseTree(
                    builder,
                    "${currentNodeKey}_$childRule",
                    currentNodeKey,
                    juridiskHenvisninger
                )
            }
        }
    }
}

fun getStyle(childResult: Status): String =
    when (childResult) {
        Status.OK -> ":::ok"
        Status.INVALID -> ":::invalid"
        Status.MANUAL_PROCESSING -> ":::manuell"
    }

fun genererLovhenvisning(juridisk: Juridisk): String {
    return when (juridisk) {
        is UtenJuridisk -> ""
        is MedJuridisk -> {
            val henvisning = juridisk.juridiskHenvisning

            val leddDel = henvisning.ledd?.let { "$it. ledd" }
            val punktumDel = henvisning.punktum?.let { "$it. punktum" }
            val bokstavDel = henvisning.bokstav?.let { "bokstav $it" }

            return listOfNotNull(leddDel, punktumDel, bokstavDel).joinToString(" ")
        }
    }
}
