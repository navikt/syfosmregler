package no.nav.syfo.rules.tilbakedatering

import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleMetadataSykmelding


class BinaryTreeDSL {
    var yes: BinaryTreeDSL? = null
    var no: BinaryTreeDSL? = null

    fun yes(yesNode: BinaryTreeDSL.() -> Unit) {
        yes = BinaryTreeDSL().apply(yesNode)
    }
    fun no(noNode: BinaryTreeDSL.() -> Unit) {
        no = BinaryTreeDSL().apply(noNode)
    }

}
fun binaryTree(tree: BinaryTreeDSL.() -> Unit): BinaryTreeDSL {
    return BinaryTreeDSL().apply(tree)
}
val tree = binaryTree {
    yes {
        yes { }
        no {  }
    }
    no {
        yes {}
        no { }
    }
}
abstract class RuleNode(val yes: RuleNode? = null, val no: RuleNode? = null, val status: Status? = null) {
    fun evaluate(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding) : Status {
        return when (isStatusNode()) {
            true -> {
                requireNotNull(status)
                status
            }
            else -> evaluateChilds(sykmelding, ruleMetadataSykmelding)
        }
    }

     private fun evaluateChilds(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding) : Status {
         requireNotNull(yes)
         requireNotNull(no)
         return when (evaluateRule(sykmelding, ruleMetadataSykmelding)) {
             true -> yes.evaluate(sykmelding, ruleMetadataSykmelding)
             else -> no.evaluate(sykmelding, ruleMetadataSykmelding)
         }
    }

    fun isStatusNode() : Boolean {
        return yes == null && no == null
    }

    protected abstract fun evaluateRule(sykmelding: Sykmelding, ruleMetadataSykmelding: RuleMetadataSykmelding) : Boolean
}

