package no.nav.syfo.metrics

import io.prometheus.client.Counter

const val METRICS_NS = "syfosmregler"

val RULE_HIT_STATUS_COUNTER: Counter = Counter.Builder()
        .namespace(METRICS_NS)
        .name("rule_hit_status_counter")
        .labelNames("rule_status")
        .help("Registers a counter for each rule status")
        .register()

val BORN_AFTER_1999_COUNTER: Counter = Counter.Builder()
        .namespace(METRICS_NS)
        .name("born_after_199_counter")
        .help("Registers a counter for each person that is born before 1999 rule status")
        .register()