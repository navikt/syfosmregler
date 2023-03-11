package no.nav.syfo.questions

enum class QuestionId(val spmId: String, val spmTekst: String, val restriksjon: RestrictionCode) {
    ID_6_1_1(
        "6.1.1",
        "Er det sykdommen, utredningen og/eller behandlingen som hindrer økt aktivitet? Beskriv.",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    ),
    ID_6_1_2("6.1.2", "Har behandlingen frem til nå bedret arbeidsevnen?", RestrictionCode.RESTRICTED_FOR_EMPLOYER),
    ID_6_1_3("6.1.3", "Hva er videre plan for behandling?", RestrictionCode.RESTRICTED_FOR_EMPLOYER),
    ID_6_1_4(
        "6.1.4",
        "Er det arbeidsforholdet som hindrer (økt) aktivitet? Beskriv.",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    ),
    ID_6_1_5("6.1.5", "Er det andre forhold som hindrer (økt) aktivitet?", RestrictionCode.RESTRICTED_FOR_EMPLOYER),
    ID_6_2_1(
        "6.2.1",
        "Beskriv kort sykehistorie, symptomer og funn i dagens situasjon.",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    ),
    ID_6_2_2("6.2.2", "Hvordan påvirker sykdommen arbeidsevnen?", RestrictionCode.RESTRICTED_FOR_EMPLOYER),
    ID_6_2_3("6.2.3", "Har behandlingen frem til nå bedret arbeidsevnen?", RestrictionCode.RESTRICTED_FOR_EMPLOYER),
    ID_6_2_4(
        "6.2.4",
        "Beskriv pågående og planlagt henvisning,utredning og/eller behandling.",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    ),
    ID_6_3_1(
        "6.3.1",
        "Beskriv kort sykehistorie, symptomer og funn i dagens situasjon",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    ),
    ID_6_3_2(
        "6.3.2",
        "Beskriv pågående og planlagt henvisning, utredning og/eller behandling. Lar dette seg kombinere med delvis arbeid?",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    ),
    ID_6_4_1(
        "6.4.1",
        "Beskriv kort sykehistorie, symptomer og funn i dagens situasjon",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    ),
    ID_6_4_2(
        "6.4.2",
        "Beskriv pågående og planlagt henvisning, utredning og/eller behandling",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    ),
    ID_6_4_3(
        "6.4.3",
        "Hva mener du skal til for at pasienten kan komme tilbake i eget eller annet arbeid?",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    ),
    ID_6_5_1(
        "6.5.1",
        "Beskriv kort sykehistorie, symptomer og funn i dagens situasjon.",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    ),
    ID_6_5_2("6.5.2", "Hvordan påvirker dette funksjons-/arbeidsevnen?", RestrictionCode.RESTRICTED_FOR_EMPLOYER),
    ID_6_5_3(
        "6.5.3",
        "Beskriv pågående og planlagt henvisning, utredning og/eller medisinsk behandling",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    ),
    ID_6_5_4(
        "6.5.4",
        "Kan arbeidsevnen bedres gjennom medisinsk behandling og/eller arbeidsrelatert aktivitet? I så fall hvordan? Angi tidsperspektiv",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    ),
    ID_6_6_1(
        "6.6.1",
        "Hva antar du at pasienten kan utføre av eget arbeid/arbeidsoppgaver i dag eller i nær framtid?",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    ),
    ID_6_6_2(
        "6.6.2",
        "Hvis pasienten ikke kan gå tilbake til eget arbeid, hva antar du at pasienten kan utføre av annet arbeid/arbeidsoppgaver?",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    ),
    ID_6_6_3(
        "6.6.3",
        "Hvilken betydning har denne sykdommen for den nedsatte arbeidsevnen?",
        RestrictionCode.RESTRICTED_FOR_EMPLOYER
    )
}
