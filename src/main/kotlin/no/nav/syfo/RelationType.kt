package no.nav.syfo

enum class RelationType(val kodeverkVerdi: String) {
    MOR("MORA"),
    FAR("FARA"),
    BARN("BARN"),
    FOSTERMOR("FOMO"),
    FOSTERFAR("FOFA"),
    FOSTERBARN("FOBA"),
    EKTEFELLE("EKTE"),
    REGISTRERT_PARTNER_MED("REPA"),
    ;

    companion object {
        fun fromKodeverkValue(kodeverkValue: String): RelationType? {
            return RelationType.values().find { it.kodeverkVerdi == kodeverkValue }
        }
    }
}
