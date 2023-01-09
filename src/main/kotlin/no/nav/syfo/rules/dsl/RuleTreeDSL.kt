package no.nav.syfo.rules.dsl

import no.nav.syfo.model.Status

sealed class TreeNode<T>

class RuleNode<T>(val rule: T) : TreeNode<T>() {
    lateinit var yes: TreeNode<T>
    lateinit var no: TreeNode<T>

    fun yes(rule: T, init: RuleNode<T>.() -> Unit) {
        yes = RuleNode(rule).apply(init)
    }
    fun no(rule: T, init: RuleNode<T>.() -> Unit) {
        no = RuleNode(rule).apply(init)
    }

    fun yes(result: Status) {
        yes = ResultNode(result)
    }
    fun no(result: Status) {
        no = ResultNode(result)
    }
}

class ResultNode<T>(val result: Status) : TreeNode<T>()

fun <T> tree(rule: T, init: RuleNode<T>.() -> Unit): RuleNode<T> = RuleNode(rule).apply(init)
