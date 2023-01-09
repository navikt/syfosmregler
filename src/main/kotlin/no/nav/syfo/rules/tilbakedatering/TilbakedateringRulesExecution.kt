package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.log
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleMetadataSykmelding
import no.nav.syfo.rules.dsl.ResultNode
import no.nav.syfo.rules.dsl.RuleNode
import no.nav.syfo.rules.dsl.TreeNode
import no.nav.syfo.rules.dsl.TreeOutput
import no.nav.syfo.rules.dsl.join

typealias TilbakedateringTreeOutput = TreeOutput<TilbakedateringRules>
typealias TilbakedateringTreeNode = TreeNode<TilbakedateringRules>

class TilbakedateringRulesExecution(private val rootNode: TilbakedateringTreeNode = tilbakedateringRuleTree) {
    fun runRules(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding): TilbakedateringTreeOutput {
        val treeOutput = traverseRuleNode(
            sykmelding = sykmelding,
            metadata = ruleMetadataSykmelding,
            treeNode = rootNode,
            rulesOutput = TilbakedateringTreeOutput(status = Status.INVALID),
        )

        val rulePath = treeOutput.rulePath.joinToString(separator = "->") { "${it.rule}(${if (it.result) "yes" else "no"})" }.plus("->${treeOutput.status}")

        log.info("Rules ${sykmelding.id}, $rulePath")

        return treeOutput
    }
}

private fun traverseRuleNode(
    treeNode: TilbakedateringTreeNode,
    rulesOutput: TilbakedateringTreeOutput,
    metadata: RuleMetadataSykmelding,
    sykmelding: Sykmelding
): TilbakedateringTreeOutput =
    when (treeNode) {
        is ResultNode -> treeNode.result join rulesOutput
        is RuleNode -> {
            val ruleResult = getRule(treeNode.rule)(sykmelding, metadata)

            val nextNode = try {
                if (ruleResult.ruleResult.result) treeNode.yes else treeNode.no
            } catch (e: RuntimeException) {
                throw IllegalStateException("Mordi er mann ${treeNode.rule}", e)
            }

            ruleResult join traverseRuleNode(
                treeNode = nextNode,
                rulesOutput = rulesOutput,
                sykmelding = sykmelding,
                metadata = metadata
            )
        }
    }
