package no.nav.syfo.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Histogram

const val METRICS_NS = "syfosmregler"

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
