package no.nav.syfo.rules.dsl

import no.nav.syfo.model.Status

sealed class TreeNode<T>

class RuleNode<T> internal constructor(val rule: T) : TreeNode<T>() {

    private lateinit var yes: TreeNode<T>
    private lateinit var no: TreeNode<T>

    internal fun yes(rule: T, init: RuleNode<T>.() -> Unit) {
        yes = RuleNode(rule).apply(init)
    }
    internal fun no(rule: T, init: RuleNode<T>.() -> Unit) {
        no = RuleNode(rule).apply(init)
    }

    internal fun yes(result: Status) {
        yes = ResultNode(result)
    }
    internal fun no(result: Status) {
        no = ResultNode(result)
    }

    internal fun nextChildNode(ruleResult: RuleResult<T>) = if (ruleResult.ruleResult) yes else no
}

class ResultNode<T>(val result: Status) : TreeNode<T>()

fun <T> tree(rule: T, init: RuleNode<T>.() -> Unit): RuleNode<T> = RuleNode(rule).apply(init)
