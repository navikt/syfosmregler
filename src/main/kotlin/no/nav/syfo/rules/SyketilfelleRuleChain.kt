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
    @Description("Sykmeldinges er tilbakedater mer enn 8 dager tilbake i tid.")
    BACKDATED_MORE_THEN_8_DAYS_FIRST_SICK(
            1204,
            Status.INVALID,
            "Sykmeldingen er tilbakedatert uten at det er begrunnet",
            "Sykmeldinges er tilbakedater mer enn 8 dager tilbake i tid.",
            { (healthInformation, ruleMetadataAndForstegangsSykemelding) ->
                ruleMetadataAndForstegangsSykemelding.erNyttSyketilfelle &&
                ruleMetadataAndForstegangsSykemelding.ruleMetadata.signatureDate > healthInformation.perioder.sortedFOMDate().first().atStartOfDay().plusDays(7) &&
                        healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty()
            }),

    @Description("Sykmeldinges er tilbakedater mindre enn 8 dager tilbake i tid.")
    BACKDATED_UP_TO_8_DAYS_FIRST_SICK_LAVE(
            1204,
            Status.INVALID,
            "Sykmeldingen er tilbakedatert uten at det er begrunnet.",
            "Sykmeldinges er tilbakedater mindre enn 8 dager tilbake i tid.",
            { (healthInformation, ruleMetadataAndForstegangsSykemelding) ->
                ruleMetadataAndForstegangsSykemelding.erNyttSyketilfelle &&
                ruleMetadataAndForstegangsSykemelding.ruleMetadata.signatureDate <= healthInformation.perioder.sortedFOMDate().first().atStartOfDay().plusDays(7) &&
                        healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty() &&
                        healthInformation.kontaktMedPasient.kontaktDato != null &&
                        ruleMetadataAndForstegangsSykemelding.ruleMetadata.signatureDate <= healthInformation.kontaktMedPasient.kontaktDato?.atStartOfDay()
            }),
}

data class RuleMetadataAndForstegangsSykemelding(
    val ruleMetadata: RuleMetadata,
    val erNyttSyketilfelle: Boolean
)
