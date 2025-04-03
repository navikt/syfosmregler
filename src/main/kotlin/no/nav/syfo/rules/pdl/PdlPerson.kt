package no.nav.syfo.rules.pdl

import no.nav.syfo.rules.pdl.client.model.Foedsel
import no.nav.syfo.rules.pdl.client.model.IdentInformasjon

data class PdlPerson(
    val identer: List<IdentInformasjon>,
    val foedselsdato: List<Foedsel>?,
) {
    val fnr: String? = identer.firstOrNull { it.gruppe == "FOLKEREGISTERIDENT" }?.ident
    val aktorId: String? = identer.firstOrNull { it.gruppe == "AKTORID" }?.ident
}
