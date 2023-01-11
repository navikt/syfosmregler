package no.nav.syfo.rules.dsl

import no.nav.syfo.model.Status

sealed class TreeNode<T>

class RuleNode<T> internal constructor(val rule: T) : TreeNode<T>() {

    lateinit var yes: TreeNode<T>
    lateinit var no: TreeNode<T>

    internal fun yes(rule: T, init: RuleNode<T>.() -> Unit) {
        yes = RuleNode(rule).apply(init)
    }

    internal fun no(rule: T, init: RuleNode<T>.() -> Unit) {
        no = RuleNode(rule).apply(init)
    }

    internal fun yes(result: Status, name: String = "") {
        yes = ResultNode(result, name)
    }

    internal fun no(result: Status, name: String = "") {
        no = ResultNode(result, name)
    }
}

class ResultNode<T>(val result: Status, val name: String) : TreeNode<T>()

fun <T> tree(rule: T, init: RuleNode<T>.() -> Unit): RuleNode<T> = RuleNode(rule).apply(init)
