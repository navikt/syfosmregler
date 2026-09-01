package no.nav.syfo.utils

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder

val jsonMapper: JsonMapper = jacksonMapperBuilder().build()
