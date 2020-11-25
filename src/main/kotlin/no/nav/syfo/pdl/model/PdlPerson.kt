package no.nav.syfo.pdl.model

data class PdlPerson(
    val adressebeskyttelse: String?
)

fun PdlPerson.getDiskresjonskode(): String? {
    return when (adressebeskyttelse) {
        "STRENGT_FORTROLIG" -> "6"
        "FORTROLIG" -> "7"
        else -> null
    }
}
