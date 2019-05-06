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
            "Sykmeldingen er tilbakedatert uten at det er begrunnet.",
            "Første sykmelding er tilbakedatert mer enn det som er tillatt.",
            { (healthInformation, ruleMetadataAndForstegangsSykemelding) ->
                ruleMetadataAndForstegangsSykemelding.erNyttSyketilfelle &&
                ruleMetadataAndForstegangsSykemelding.ruleMetadata.signatureDate > healthInformation.perioder.sortedFOMDate().first().atStartOfDay().plusDays(8) &&
                        healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty()
            }),

    @Description("Første gangs sykmelding er tilbakedatert mindre enn 8 dager.")
    BACKDATED_UP_TO_8_DAYS_FIRST_SICK_LAVE(
            1204,
            Status.INVALID,
            "Sykmeldingen er tilbakedatert uten at det er begrunnet.",
            "Første sykmelding er tilbakedatert uten at dato for kontakt er angitt eller begrunnelse er gitt.",
            { (healthInformation, ruleMetadataAndForstegangsSykemelding) ->
                ruleMetadataAndForstegangsSykemelding.erNyttSyketilfelle &&
                ruleMetadataAndForstegangsSykemelding.ruleMetadata.signatureDate <= healthInformation.perioder.sortedFOMDate().first().atStartOfDay().plusDays(8) &&
                        healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty() &&
                        healthInformation.kontaktMedPasient.kontaktDato != null &&
                        ruleMetadataAndForstegangsSykemelding.ruleMetadata.signatureDate <= healthInformation.kontaktMedPasient.kontaktDato?.atStartOfDay()
                        // burde vi i tillegg sjekke om kontaktdato er større en fom-dato?
            }),

    @Description("Fom-dato i ny sykmelding som er en forlengelse kan maks være tilbakedatert 1 mnd fra signaturdato. Skal telles.")
    BACKDATING_SYKMELDING_EXTENSION(
            null,
            Status.INVALID,
            "Det må begrunnes hvorfor sykmeldingen er tilbakedatert.",
            "Fom-dato i ny sykmelding som er en forlengelse kan maks være tilbakedatert 1 mnd fra signaturdato. Skal telles.",
            { (healthInformation, ruleMetadataAndForstegangsSykemelding) ->
                !ruleMetadataAndForstegangsSykemelding.erNyttSyketilfelle &&
                healthInformation.perioder.sortedFOMDate().first().minusMonths(1).atStartOfDay() > ruleMetadataAndForstegangsSykemelding.ruleMetadata.signatureDate
            }),

    @Description("Sykmeldingens fom-dato er inntil 3 år tilbake i tid og årsak for tilbakedatering er angitt.")
    BACKDATED_WITH_REASON_FIRST_SICK_LAVE(
            1207,
            Status.MANUAL_PROCESSING,
            "Sykmeldingens fom-dato er inntil 3 år tilbake i tid og årsak for tilbakedatering er angitt.",
            "Sykmeldingens fom-dato er inntil 3 år tilbake i tid og årsak for tilbakedatering er angitt.",
            { (healthInformation, ruleMetadataAndForstegangsSykemelding) ->
                ruleMetadataAndForstegangsSykemelding.erNyttSyketilfelle &&
                        ruleMetadataAndForstegangsSykemelding.ruleMetadata.signatureDate > healthInformation.perioder.sortedFOMDate().first().atStartOfDay().plusDays(8) &&
                        !healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty()
            }),

    @Description("Sykmeldingens fom-dato er inntil 3 år tilbake i tid og årsak for tilbakedatering er angitt.")
    BACKDATED_WITH_REASON_EXTENSION(
            1207,
            Status.MANUAL_PROCESSING,
            "Sykmeldingens fom-dato er inntil 3 år tilbake i tid og årsak for tilbakedatering er angitt.",
            "Sykmeldingens fom-dato er inntil 3 år tilbake i tid og årsak for tilbakedatering er angitt.",
            { (healthInformation, ruleMetadataAndForstegangsSykemelding) ->
                !ruleMetadataAndForstegangsSykemelding.erNyttSyketilfelle &&
                        ruleMetadataAndForstegangsSykemelding.ruleMetadata.signatureDate > healthInformation.perioder.sortedFOMDate().first().atStartOfDay().plusDays(30) &&
                        !healthInformation.kontaktMedPasient.begrunnelseIkkeKontakt.isNullOrEmpty()
            }),
}

data class RuleMetadataAndForstegangsSykemelding(
    val ruleMetadata: RuleMetadata,
    val erNyttSyketilfelle: Boolean
)
