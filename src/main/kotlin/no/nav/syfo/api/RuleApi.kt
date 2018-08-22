package no.nav.syfo.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.request.receiveStream
import io.ktor.response.respondWrite
import io.ktor.routing.Routing
import io.ktor.routing.post
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult

val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

fun Routing.registerRuleApi() {
    post("/v1/rules/validate") {
        call.respondJson {
            ValidationResult(
                    status = Status.OK,
                    ruleHits = listOf()
            )
        }
    }
}

suspend fun ApplicationCall.respondJson(jsonCallback: () -> Any) {
    respondWrite {
        objectMapper.writeValue(this, jsonCallback())
    }
}

suspend inline fun <reified T>ApplicationCall.receiveJson(): T = objectMapper.readValue(receiveStream(), T::class.java)
