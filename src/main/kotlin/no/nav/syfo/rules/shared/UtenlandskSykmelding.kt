package no.nav.syfo.rules.shared

data class UtenlandskSykmelding(
    val land: String,
    val folkeRegistertAdresseErBrakkeEllerTilsvarende: Boolean,
)
