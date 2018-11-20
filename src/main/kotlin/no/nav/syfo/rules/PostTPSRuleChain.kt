package no.nav.syfo.rules

import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.model.Status

// TODO: 1303, Hvis pasienten ikke finnes registrert i folkeregisteret returneres meldingen
// Kan kanskje hentes fra aktørid register
enum class PostTPSRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<RuleMetadata>) -> Boolean) : Rule<RuleData<RuleMetadata>> {

    @Description("Pasientens fødselsnummer eller D-nummer finnes ikke registrert i Folkeregisteret.")
    PATIENT_NOT_FOUND_TPS(1303, Status.INVALID, { (_, _, pasientTPS) ->
        pasientTPS.foedselsdato == null
    }),

    @Description("Person er registrert utvandret i Folkeregisteret.")
    PATIENT_EMIGRATED(1304, Status.MANUAL_PROCESSING, { (_, _, pasientTPS) ->
        pasientTPS.personstatus?.personstatus?.value == "UTVA"
    }),

    @Description("Pasient er registrert med sperrekode 6, sperret adresse, strengt fortrolig. Kode 6 overstyrer oppfølgingsregler. Melding går ikke til Arena.")
    PATIENT_HAS_SPERREKODE_6(1305, Status.MANUAL_PROCESSING, { (_, _, pasientTPS) ->
        pasientTPS.diskresjonskode?.kodeverksRef == "SPSF"
    })

    // TODO: Utvandret: 1304
    // TODO: Sperrekode 6: 1305
    // TODO: Disse reglene trenger litt diskusjon med fag
    // TODO: Hvis det er over 3 måneder siden behandler endret fødselsnummer går meldingen til manuell behandling med beskjed om at sertifikat for digital signatur må oppdateres.: 1317
}
