package no.nav.syfo.rules

import no.nav.syfo.Rule
import no.nav.syfo.model.Status
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person

// TODO: 1303, Hvis pasienten ikke finnes registrert i folkeregisteret returneres meldingen
// Kan kanskje hentes fra aktørid register
enum class TPSRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<Person>) -> Boolean) : Rule<RuleData<Person>> {

    // TODO: Utvandret: 1304
    // TODO: Sperrekode 6: 1305
    // TODO: Disse reglene trenger litt diskusjon med fag
    // TODO: Hvis det er over 3 måneder siden behandler endret fødselsnummer går meldingen til manuell behandling med beskjed om at sertifikat for digital signatur må oppdateres.: 1317
}
