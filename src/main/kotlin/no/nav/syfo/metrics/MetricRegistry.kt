package no.nav.syfo.metrics

import io.prometheus.client.Counter

const val METRICS_NS = "syfosmregler"

val RULE_HIT_STATUS_COUNTER: Counter = Counter.Builder()
    .namespace(METRICS_NS)
    .name("rule_hit_status_counter")
    .labelNames("rule_status")
    .help("Registers a counter for each rule status")
    .register()

val FODSELSDATO_FRA_PDL_COUNTER: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name("fodselsdato_fra_pdl_counter")
    .help("Antall fodselsdatoer hentet fra PDL")
    .register()

val FODSELSDATO_FRA_IDENT_COUNTER: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name("fodselsdato_fra_ident_counter")
    .help("Antall fodselsdatoer utledet fra fnr/dnr")
    .register()

val RULE_HIT_COUNTER: Counter = Counter.Builder()
    .namespace("syfosm")
    .name("rule_hit_counter")
    .labelNames("rule_name")
    .help("Counts the amount of times a rule is hit").register()

val TILBAKEDATERING_RULE_HIT_COUNTER: Counter = Counter.Builder()
    .namespace(METRICS_NS)
    .name("tilbakedatering_rule_hit_counter")
    .labelNames("status", "rule_hit")
    .help("Counts tilbakedatering rules")
    .register()

val TILBAKEDATERING_RULE_PATH_COUNTER: Counter = Counter.Builder()
    .namespace(METRICS_NS)
    .name("tilbakedatering_rule_path_counter")
    .labelNames("path")
    .help("Counts tilbakedatering rule paths")
    .register()

val HPR_RULE_HIT_COUNTER: Counter = Counter.Builder()
    .namespace(METRICS_NS)
    .name("hpr_rule_hit_counter")
    .labelNames("status", "rule_hit")
    .help("Counts hpr rules")
    .register()

val HPR_RULE_PATH_COUNTER: Counter = Counter.Builder()
    .namespace(METRICS_NS)
    .name("hpr_rule_path_counter")
    .labelNames("path")
    .help("Counts hpr rule paths")
    .register()

val LEGESUSPENSJON_RULE_HIT_COUNTER: Counter = Counter.Builder()
    .namespace(METRICS_NS)
    .name("legesuspensjon_rule_hit_counter")
    .labelNames("status", "rule_hit")
    .help("Counts legesuspensjon rules")
    .register()

val LEGESUSPENSJON_RULE_PATH_COUNTER: Counter = Counter.Builder()
    .namespace(METRICS_NS)
    .name("legesuspensjon_rule_path_counter")
    .labelNames("path")
    .help("Counts legesuspensjon rule paths")
    .register()
