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
// Camillas forsøk på å skrive kode:
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
    
    
    // 1. Sjekke om begrunnIkkeKontakt er utfylt
    // 2. Sjekke om sykmelding er ny eller forlengelse og at den er tilbakedatert
    // 3. Hvis ny sykmelding - bruke 8-dagers regel, dvs utover 8 dager -> manuell behandling
    // 4. Hvis forlenget - bruke 1 mnd regel, dvs tilbakedatering utover 1 mnd -> manuell behandling
    // Skal være første fom i sykmeldingen

    @Description("Sykmeldingens fom-dato er inntil 3 år tilbake i tid og årsak for tilbakedatering er angitt.")
    BACKDATED_WITH_REASON(1207, Status.MANUAL_PROCESSING, { (healthInformation, ruleMetadata) ->
        !healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty() &&
        ruleMetadata.signatureDate.minusYears(3).isBefore(healthInformation.perioder.sortedFOMDate().first().atStartOfDay()) 
            If !forlengelse healthInformation.signatureDate < it.periodeFOMDato.plusDays(8)
            else healthInformation.signatureDate < it.periodeFOMDato.plusDays(30)
        }
   }), 
}
