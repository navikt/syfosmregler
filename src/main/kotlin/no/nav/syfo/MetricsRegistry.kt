package no.nav.syfo

import io.prometheus.client.Counter
import io.prometheus.client.Summary

const val NAMESPACE = "syfosmregler"

val RULE_HIT_COUNTER: Counter = Counter.Builder().namespace(NAMESPACE).name("rule_hit_counter")
        .labelNames("rule_name").help("Registers a counter for each rule in the rule set").register()
val RULE_HIT_SUMMARY: Summary = Summary.Builder().namespace(NAMESPACE).name("rule_hit_summary")
        .labelNames("rule_name").help("Registers a summary for each rule in the rule set").register()
val REST_CALL_SUMMARY: Summary = Summary.Builder().namespace(NAMESPACE).name("rest_call_summary")
        .labelNames("http_endpoint").help("Summary for rest call times").register()
val SOAP_CALL_SUMMARY: Summary = Summary.Builder().namespace(NAMESPACE).name("soap_call_summary")
        .labelNames("http_endpoint").help("Summary for SOAP call times").register()
