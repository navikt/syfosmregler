package no.nav.syfo.rules

import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.model.Status
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person as TPSPerson

// TODO: 1303, Hvis pasienten ikke finnes registrert i folkeregisteret returneres meldingen
// Kan kanskje hentes fra aktørid register
enum class PostTPSRuleChain(
    override val ruleId: Int?,
    override val status: Status,
    override val textToUser: String,
    override val textToTreater: String,
    override val predicate: (RuleData<TPSPerson>) -> Boolean
) : Rule<RuleData<TPSPerson>> {
    // TODO: Hvordan skal vi håndtere dette om vi ikke sender til Arena? Hvordan kan vi hente ut informasjonen uten å kalle TPS?
    @Description("Pasient er registrert med sperrekode 6, sperret adresse, strengt fortrolig. Kode 6 overstyrer oppfølgingsregler. Melding går ikke til Arena.")
    PATIENT_HAS_SPERREKODE_6(
            1305,
            Status.MANUAL_PROCESSING,
            "Pasient er registrert med sperrekode 6, sperret adresse, strengt fortrolig",
            "Pasient er registrert med sperrekode 6, sperret adresse, strengt fortrolig", { (_, patient) ->
        patient.diskresjonskode?.kodeverksRef == "SPSF"
    })
}
