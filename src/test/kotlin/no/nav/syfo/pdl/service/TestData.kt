package no.nav.syfo.pdl.service

import no.nav.syfo.pdl.client.model.Foedsel
import no.nav.syfo.pdl.client.model.GraphQLResponse
import no.nav.syfo.pdl.client.model.HentPerson
import no.nav.syfo.pdl.client.model.IdentInformasjon
import no.nav.syfo.pdl.client.model.Identliste
import no.nav.syfo.pdl.client.model.PdlResponse

fun getPdlResponse(): GraphQLResponse<PdlResponse> {
    return GraphQLResponse(
        PdlResponse(
            hentPerson = HentPerson(listOf(Foedsel("1900", "1900-01-01"))),
            hentIdenter = Identliste(listOf(IdentInformasjon(ident = "01245678901", gruppe = "FOLKEREGISTERIDENT", historisk = false)))
        ),
        errors = null
    )
}
