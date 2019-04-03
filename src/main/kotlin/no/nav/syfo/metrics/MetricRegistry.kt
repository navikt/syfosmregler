package no.nav.syfo.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Summary

const val METRICS_NS = "syfosmregler"

val RULE_HIT_STATUS_COUNTER: Counter = Counter.Builder()
        .namespace(METRICS_NS)
        .name("rule_hit_status_counter")
        .labelNames("rule_status")
        .help("Registers a counter for each rule status")
        .register()