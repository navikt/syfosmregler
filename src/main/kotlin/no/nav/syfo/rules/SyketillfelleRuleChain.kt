package no.nav.syfo.rules

import no.nav.syfo.Description
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.api.Oppfolgingstilfelle
import no.nav.syfo.model.Status

enum class SyketillfelleRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<Oppfolgingstilfelle?>) -> Boolean) : Rule<RuleData<Oppfolgingstilfelle?>> {
    @Description("Hvis fom dato i varighet er lik start dato på sykdomtilfelle og første konsultasjon er mer enn 8 dager fra start dato men ikke over et år")
    BACKDATED_MORE_THEN_8_DAYS_AND_UNDER_1_YEAR_BACKDATED(1204, Status.INVALID, { (healthInformation, _) ->
        healthInformation.kontaktMedPasient.kontaktDato != null && healthInformation.perioder.any {
            healthInformation.kontaktMedPasient.kontaktDato > it.fom.plusDays(7) && (
                    healthInformation.kontaktMedPasient.kontaktDato <= it.fom.plusYears(1).minusDays(1) ||
                    !healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty())
        }
    }),
}

enum class SyketillfelleRuleChain(override val ruleId: Int?, override val status: Status, override val predicate: (RuleData<Oppfolgingstilfelle?>) -> Boolean) : Rule<RuleData<Oppfolgingstilfelle?>> {
    @Description("Hvis første gangs sykmelding er tilbakedatert mer enn 8 dager før første konsultasjon uten begrunnelse eller kontaktdato i perioden")
    BACKDATED_MORE_THEN_8_DAYS_FIRST_SICK_LEAVE (1204, Status.INVALID, { (healthInformation, _) ->
       //sjekker først om det er tilbakedatering uten begrunnelse
       healthInformation.perioder.sortedFOMDate().first() < ruleMetadata.signaturDate && 
                                    healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty() {
           // Skal avvises hvis mer enn 8 dager tilbakedatert eller kontaktdato ikke er i perioden. 
          healthInformation.perioder.sortedFOMDate().first().plusDays(8) < ruleMetadata.signaturDate ||
                                    healthInformation.kontaktMedPasient.kontaktDato > ruleMetadata.signaturDate ||
                                       healthInformation.kontaktMedPasient.kontaktDato.plusDays(8) < ruleMetadata.signaturDate 
                                       
        
        }
    }),
}
