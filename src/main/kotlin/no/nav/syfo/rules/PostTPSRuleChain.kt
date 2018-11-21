package no.nav.syfo.rules

import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.model.Status
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.syfo.RelationType

// TODO: 1303, Hvis pasienten ikke finnes registrert i folkeregisteret returneres meldingen
// Kan kanskje hentes fra aktørid register
enum class PostTPSRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<RuleMetadata>) -> Boolean) : Rule<RuleData<RuleMetadata>> {

    @Description("Pasienten er registrert død i Folkeregisteret.")
    REGISTERED_DEAD_IN_TPS(1301, Status.MANUAL_PROCESSING, { (_, _, pasientTPS) ->
        pasientTPS.doedsdato != null
    }),

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
    }),

    @Description("Pasient er registrert med sperrekode 6, sperret adresse, strengt fortrolig. Kode 6 overstyrer oppfølgingsregler. Melding går ikke til Arena.")
    REGISTERED_DEAD_IN_TPS_BEFORE_SICKLAVE_START(1307, Status.INVALID, { (healthInformation, _, pasientTPS) ->
        pasientTPS.doedsdato.doedsdato.toGregorianCalendar().toZonedDateTime().toLocalDate().isBefore(healthInformation.aktivitet.periode.first().periodeFOMDato.toGregorianCalendar().toZonedDateTime().toLocalDate())
    }),

    @Description("Pasient er registrert med sperrekode 6, sperret adresse, strengt fortrolig. Kode 6 overstyrer oppfølgingsregler. Melding går ikke til Arena.")
    MARRIED_TO_PATIENT(1310, Status.MANUAL_PROCESSING, { (_, _, pasientTPS, doctorPersonnumber) ->
        if (findDoctorInRelations(pasientTPS, doctorPersonnumber) != null) {
            RelationType.fromKodeverkValue(findDoctorInRelations(pasientTPS, doctorPersonnumber)!!.tilRolle.value) == RelationType.EKTEFELLE
        } else {
            false
        }
    }),

    @Description("Pasient er registrert med sperrekode 6, sperret adresse, strengt fortrolig. Kode 6 overstyrer oppfølgingsregler. Melding går ikke til Arena.")
    REGISTERED_PARTNER_WITH_PATIENT(1311, Status.MANUAL_PROCESSING, { (_, _, pasientTPS, doctorPersonnumber) ->
        if (findDoctorInRelations(pasientTPS, doctorPersonnumber) != null) {
            RelationType.fromKodeverkValue(findDoctorInRelations(pasientTPS, doctorPersonnumber)!!.tilRolle.value) == RelationType.REGISTRERT_PARTNER_MED
        } else {
            false
        }
    }),

    @Description("Pasient er registrert med sperrekode 6, sperret adresse, strengt fortrolig. Kode 6 overstyrer oppfølgingsregler. Melding går ikke til Arena.")
    PARENT_TO_PATIENT(1312, Status.MANUAL_PROCESSING, { (_, _, pasientTPS, doctorPersonnumber) ->
        if (findDoctorInRelations(pasientTPS, doctorPersonnumber) != null) {
            listOf(RelationType.FAR, RelationType.FOSTERFAR, RelationType.MOR, RelationType.FOSTERMOR).contains(
            RelationType.fromKodeverkValue(findDoctorInRelations(pasientTPS, doctorPersonnumber)!!.tilRolle.value))
        } else {
            false
        }
    }),

    @Description("Pasient er registrert med sperrekode 6, sperret adresse, strengt fortrolig. Kode 6 overstyrer oppfølgingsregler. Melding går ikke til Arena.")
    CHILD_OF_PATIENT(1313, Status.MANUAL_PROCESSING, { (_, _, pasientTPS, doctorPersonnumber) ->
        if (findDoctorInRelations(pasientTPS, doctorPersonnumber) != null) {
            listOf(RelationType.BARN, RelationType.FOSTERBARN).contains(
                    RelationType.fromKodeverkValue(findDoctorInRelations(pasientTPS, doctorPersonnumber)!!.tilRolle.value))
        } else {
            false
        }
    })

    // TODO: Utvandret: 1304
    // TODO: Sperrekode 6: 1305
    // TODO: Disse reglene trenger litt diskusjon med fag
    // TODO: Hvis det er over 3 måneder siden behandler endret fødselsnummer går meldingen til manuell behandling med beskjed om at sertifikat for digital signatur må oppdateres.: 1317
}

fun findDoctorInRelations(patient: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person, doctorPersonnumber: String): Familierelasjon? =
        patient.harFraRolleI.find {
            val aktoer = it.tilPerson.aktoer
            aktoer is PersonIdent && aktoer.ident.ident == doctorPersonnumber
        }
