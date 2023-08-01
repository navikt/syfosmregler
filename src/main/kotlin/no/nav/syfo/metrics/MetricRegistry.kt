package no.nav.syfo.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Histogram

const val METRICS_NS = "syfosmregler"

val RULE_NODE_RULE_HIT_COUNTER: Counter =
    Counter.Builder()
        .namespace(METRICS_NS)
        .name("rulenode_rule_hit_counter")
        .labelNames("status", "rule_hit")
        .help("Counts rulenode rules")
        .register()

val RULE_NODE_RULE_PATH_COUNTER: Counter =
    Counter.Builder()
        .namespace(METRICS_NS)
        .name("rulenode_rule_path_counter")
        .labelNames("path")
        .help("Counts rulenode rule paths")
        .register()

val HTTP_HISTOGRAM: Histogram =
    Histogram.Builder()
        .namespace(METRICS_NS)
        .labelNames("path")
        .name("requests_duration_seconds")
        .help("http requests durations for incoming requests in seconds")
        .register()
