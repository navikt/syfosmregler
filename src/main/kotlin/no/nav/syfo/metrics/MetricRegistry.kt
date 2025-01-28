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

val PDL_HISTOGRAM: Histogram =
    Histogram.Builder()
        .namespace(METRICS_NS)
        .labelNames("path")
        .name("pdl_requests_duration_seconds")
        .help("pdl requests durations for incoming requests in seconds")
        .register()

val HPR_HISTOGRAM: Histogram =
    Histogram.Builder()
        .namespace(METRICS_NS)
        .labelNames("path")
        .name("hpr_requests_duration_seconds")
        .help("hpr requests durations for incoming requests in seconds")
        .register()

val HPR_RETRY_COUNT: Counter =
    Counter.Builder()
        .namespace(METRICS_NS)
        .name("hpr_retry_count")
        .labelNames("path")
        .help("Counts hpr retries")
        .register()

val SUSPANSJON_HISTOGRAM: Histogram =
    Histogram.Builder()
        .namespace(METRICS_NS)
        .labelNames("path")
        .name("suspansjon_requests_duration_seconds")
        .help("suspansjon requests durations for incoming requests in seconds")
        .register()

val ARBEIDSGIVERPERIODE_RULE_COUNTER: Counter =
    Counter.Builder()
        .namespace(METRICS_NS)
        .labelNames("version", "status")
        .name("arbeidsgiverperiode_count")
        .help("Counts number of cases of arbeidsgiverperiode rule")
        .register()

val TILBAKEDATERING_RULE_DAYS_COUNTER: Counter =
    Counter.Builder()
        .namespace("tsmregler")
        .labelNames("kilde", "dager")
        .name("tilbakedatert_antall_dager")
        .help("Teller antall sykmeldinger som er tilbakedatert med antall dager")
        .register()
