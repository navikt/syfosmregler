package no.nav.syfo.metrics

import io.prometheus.client.Counter

const val METRICS_NS = "syfosmregler"

val RULE_NODE_RULE_HIT_COUNTER: Counter = Counter.Builder()
    .namespace(METRICS_NS)
    .name("rulenode_rule_hit_counter")
    .labelNames("status", "rule_hit")
    .help("Counts rulenode rules")
    .register()

val RULE_NODE_RULE_PATH_COUNTER: Counter = Counter.Builder()
    .namespace(METRICS_NS)
    .name("rulenode_rule_path_counter")
    .labelNames("path")
    .help("Counts rulenode rule paths")
    .register()

val TILBAKEDATERING_SIGNATUR_RULE_HIT_COUNTER: Counter = Counter.Builder()
    .namespace(METRICS_NS)
    .name("tilbakedatering_signatur_rule_hit_counter")
    .labelNames("status", "rule_hit")
    .help("Counts rulenode rules")
    .register()

val TILBAKEDATERING_SIGNATUR_RULE_PATH_COUNTER: Counter = Counter.Builder()
    .namespace(METRICS_NS)
    .name("tilbakedatering_signatur_rule_path_counter")
    .labelNames("path")
    .help("Counts rulenode rule paths")
    .register()
