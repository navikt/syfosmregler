package no.nav.syfo.rules

import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status

enum class SyketilfelleRuleChain(
    override val ruleId: Int?,
    override val status: Status,
    override val messageForUser: String,
    override val messageForSender: String,
    override val predicate: (RuleData<RuleMetadataAndForstegangsSykemelding>) -> Boolean
) : Rule<RuleData<RuleMetadataAndForstegangsSykemelding>> {
    @Description("Første gangs sykmelding er tilbakedatert mer enn 8 dager.")
    BACKDATED_MORE_THEN_8_DAYS_FIRST_SICK(
            1204,
            Status.INVALID,
            "Første sykmelding er tilbakedatert mer enn det som er tillatt.",
            "Første sykmelding er tilbakedatert mer enn det som er tillatt.",
            { (healthInformation, ruleMetadataAndForstegangsSykemelding) ->
                ruleMetadataAndForstegangsSykemelding.erNyttSyketilfelle &&
                ruleMetadataAndForstegangsSykemelding.ruleMetadata.signatureDate > healthInformation.perioder.sortedFOMDate().first().atStartOfDay().plusDays(7) &&
                        healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty()
            }),

    @Description("Første gangs sykmelding er tilbakedatert mindre enn 8 dager.")
    BACKDATED_UP_TO_8_DAYS_FIRST_SICK_LAVE(
            1204,
            Status.INVALID,
            "Første sykmelding er tilbakedatert mer enn det som er tillatt.",
            "Første sykmelding er tilbakedatert uten at dato for kontakt er angitt eller begrunnelse er gitt.",
            { (healthInformation, ruleMetadataAndForstegangsSykemelding) ->
                ruleMetadataAndForstegangsSykemelding.erNyttSyketilfelle &&
                ruleMetadataAndForstegangsSykemelding.ruleMetadata.signatureDate <= healthInformation.perioder.sortedFOMDate().first().atStartOfDay().plusDays(7) &&
                        healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty() &&
                        healthInformation.kontaktMedPasient.kontaktDato != null &&
                        ruleMetadataAndForstegangsSykemelding.ruleMetadata.signatureDate <= healthInformation.kontaktMedPasient.kontaktDato?.atStartOfDay()
                        // burde vi i tillegg sjekke om kontaktdato er større en fom-dato?
            }),
}

data class RuleMetadataAndForstegangsSykemelding(
    val ruleMetadata: RuleMetadata,
    val erNyttSyketilfelle: Boolean
)
