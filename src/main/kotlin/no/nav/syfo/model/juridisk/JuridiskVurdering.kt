package no.nav.syfo.model.juridisk

import java.time.LocalDate
import java.time.ZonedDateTime
import no.nav.syfo.rules.common.MedJuridisk
import no.nav.syfo.rules.common.UtenJuridisk

data class JuridiskVurdering(
    val id: String,
    val eventName: String,
    val version: String,
    val kilde: String,
    val versjonAvKode: String,
    val fodselsnummer: String,
    val juridiskHenvisning: JuridiskHenvisning,
    val sporing: Map<String, String>,
    val input: Map<String, Any>,
    val tidsstempel: ZonedDateTime,
    val utfall: JuridiskUtfall
)

data class JuridiskVurderingResult(val juridiskeVurderinger: List<JuridiskVurdering>)

data class JuridiskHenvisning(
    val lovverk: Lovverk,
    val paragraf: String,
    val ledd: Int?,
    val punktum: Int?,
    val bokstav: String?,
)

enum class JuridiskEnum(val JuridiskHenvisning: no.nav.syfo.rules.common.Juridisk) {
    INGEN(UtenJuridisk),
    FOLKETRYGDLOVEN_8_3_1(
        MedJuridisk(
            JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-3",
                ledd = 1,
                punktum = null,
                bokstav = null,
            ),
        ),
    ),
    FOLKETRYGDLOVEN_8_4_1(
        MedJuridisk(
            JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-4",
                ledd = 1,
                punktum = null,
                bokstav = null,
            ),
        ),
    ),
    FOLKETRYGDLOVEN_8_7(
        MedJuridisk(
            JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-7",
                ledd = null,
                punktum = null,
                bokstav = null,
            ),
        ),
    ),
    FOLKETRYGDLOVEN_8_7_1(
        MedJuridisk(
            JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-7",
                ledd = 1,
                punktum = null,
                bokstav = null,
            ),
        ),
    ),
    FOLKETRYGDLOVEN_8_7_1_1(
        MedJuridisk(
            JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-7",
                ledd = 1,
                punktum = 1,
                bokstav = null,
            ),
        ),
    ),
    FOLKETRYGDLOVEN_8_7_2_2(
        MedJuridisk(
            JuridiskHenvisning(
                lovverk = Lovverk.FOLKETRYGDLOVEN,
                paragraf = "8-7",
                ledd = 2,
                punktum = 2,
                bokstav = null,
            ),
        ),
    ),
}

enum class Lovverk(val navn: String, val kortnavn: String, val lovverksversjon: LocalDate) {
    FOLKETRYGDLOVEN(
        navn = "Lov om folketrygd",
        kortnavn = "Folketrygdloven",
        lovverksversjon = LocalDate.of(2022, 1, 1),
    ),
}

enum class JuridiskUtfall {
    VILKAR_OPPFYLT,
    VILKAR_IKKE_OPPFYLT,
    VILKAR_UAVKLART,
}
