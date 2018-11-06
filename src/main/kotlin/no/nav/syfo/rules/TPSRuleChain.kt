package no.nav.syfo.rules

import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.model.Status
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person

// TODO: 1303, Hvis pasienten ikke finnes registrert i folkeregisteret returneres meldingen
enum class TPSRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<Person>) -> Boolean) : Rule<RuleData<Person>> {

    // TODO: Utvandret: 1304
    // TODO: Sperrekode 6: 1305
    @Description("Hvis pasienten er registrert død i folkeregisteret skal meldingen gå til manuell behandling")
    REGISTRATED_AS_DEAD(1307, Status.MANUAL_PROCESSING, { (healthInformation, person) ->
        (person.doedsdato != null) && person.doedsdato.doedsdato.toZoned() < healthInformation.aktivitet.periode.sortedFOMDate().first()
    }),
    // TODO: Disse reglene trenger litt diskusjon med fag
    // TODO: Hvis behandler er gift med pasient går meldingen til manuell behandling.: 1310
    // TODO: Hvis behandler er samboer med pasient går meldingen til manuell behandling.: 1311
    // TODO: Hvis behandler er forelder til pasient går meldingen til manuell behandling.: 1312
    // TODO: Hvis behandler er barn av pasient går meldingen til manuell behandling.: 1313
    // TODO: Hvis behandler er gift med pasient men  lever atskilt går meldingen til manuell behandling.: 1314
    // TODO: Hvis det er over 3 måneder siden behandler endret fødselsnummer går meldingen til manuell behandling med beskjed om at sertifikat for digital signatur må oppdateres.: 1317
}
