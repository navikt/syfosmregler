package no.nav.syfo

enum class UtdypendeOpplysninger(val spmGruppeId: String, val spmGruppeTekst: String, val spmsvar: List<SpmId>) {
    DYNAGRUPPE6_1("6.1", "Utdypende opplysninger ved 4,12 og 28 uker ved visse diagnose", listOf(SpmId.SpmId6_1_1, SpmId.SpmId6_1_2, SpmId.SpmId6_1_3, SpmId.SpmId6_1_4, SpmId.SpmId6_1_5)),
    DYNAGRUPPE6_2("6.2", "Utdypende opplysninger ved 8,17 og 39 uker", listOf(SpmId.SpmId6_2_1, SpmId.SpmId6_2_2, SpmId.SpmId6_2_3, SpmId.SpmId6_2_4)),
    DYNAGRUPPE6_3("6.3", "Opplysninger ved vurdering av aktivitetskravet", listOf(SpmId.SpmId6_3_1, SpmId.SpmId6_3_2)),
    DYNAGRUPPE6_4("6.4", "Helseopplysninger ved 17 uker", listOf(SpmId.SpmId6_4_1, SpmId.SpmId6_4_2, SpmId.SpmId6_4_3)),
    DYNAGRUPPE6_5("6.5", "Utdypende opplysninger ved 39 uker", listOf(SpmId.SpmId6_5_1, SpmId.SpmId6_5_2, SpmId.SpmId6_5_3, SpmId.SpmId6_5_4)),
    DYNAGRUPPE6_6("6.6", "Helseopplysninger dersom pasienten søker om AAP", listOf(SpmId.SpmId6_6_1, SpmId.SpmId6_6_2, SpmId.SpmId6_6_3))
}
