package no.nav.syfo.rules

import com.devskiller.jfairy.Fairy
import no.nav.syfo.validation.validatePersonAndDNumber
import org.spekframework.spek2.Spek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val fairy: Fairy = Fairy.create() // (Locale("no", "NO"))
val personNumberDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy")

object ValidationRuleChainSpek : Spek({
    fun ruleData(
        healthInformation: Sykmelding,
        receivedDate: LocalDateTime = LocalDateTime.now(),
        signatureDate: LocalDateTime = LocalDateTime.now(),
        behandletTidspunkt: LocalDateTime = LocalDateTime.now(),
        patientPersonNumber: String = "1234567891",
        rulesetVersion: String = "1",
        legekontorOrgNr: String = "123456789",
        tssid: String? = "1314445",
        avsenderfnr: String = "12344",
        pasientFodselsdato: LocalDate = LocalDate.now()
    ): RuleData<RuleMetadata> = RuleData(
        healthInformation,
        RuleMetadata(
            signatureDate,
            receivedDate,
            behandletTidspunkt,
            patientPersonNumber,
            rulesetVersion,
            legekontorOrgNr,
            tssid,
            avsenderfnr,
            pasientFodselsdato
        )
    )

    describe("Testing validation rules and checking the rule outcomes") {
        it("Should check rule PASIENT_YNGRE_ENN_13,should trigger rule") {
            val person = fairy.person(PersonProperties.ageBetween(PersonProvider.MIN_AGE, 12))

            ValidationRuleChain.PASIENT_YNGRE_ENN_13(
                ruleData(
                    generateSykmelding(),
                    patientPersonNumber = generatePersonNumber(person.dateOfBirth, false)
                )
            ) shouldBeEqualTo true
        }

        it("Should check rule PASIENT_YNGRE_ENN_13,should NOT trigger rule") {
            val person = fairy.person(
                PersonProperties.ageBetween(13, 70)
            )

            ValidationRuleChain.PASIENT_YNGRE_ENN_13(
                ruleData(
                    generateSykmelding(),
                    patientPersonNumber = generatePersonNumber(person.dateOfBirth, false),
                    pasientFodselsdato = LocalDate.now().minusYears(14)
                )
            ) shouldBeEqualTo false
        }

        it("Should check rule PASIENT_ELDRE_ENN_70,should trigger rule") {
            val person = fairy.person(
                PersonProperties.ageBetween(71, 88)
            )

            ValidationRuleChain.PASIENT_ELDRE_ENN_70(
                ruleData(
                    generateSykmelding(),
                    patientPersonNumber = generatePersonNumber(person.dateOfBirth, false),
                    pasientFodselsdato = LocalDate.now().minusYears(71)
                )
            ) shouldBeEqualTo true
        }

        it("Should check rule PASIENT_ELDRE_ENN_70,should NOT trigger rule") {
            val person = fairy.person(
                PersonProperties.ageBetween(13, 69)
            )

            ValidationRuleChain.PASIENT_ELDRE_ENN_70(
                ruleData(
                    generateSykmelding(),
                    patientPersonNumber = generatePersonNumber(person.dateOfBirth, false)
                )
            ) shouldBeEqualTo false
        }

        it("Skal håndtere fødselsnummer fra 1854-1899") {
            val beregnetFodselsar1 = extractBornYear("01015450000")
            val beregnetFodselsar2 = extractBornYear("01015474900")
            val beregnetFodselsar3 = extractBornYear("01019950000")
            val beregnetFodselsar4 = extractBornYear("01019974900")

            beregnetFodselsar1 shouldBeEqualTo 1854
            beregnetFodselsar2 shouldBeEqualTo 1854
            beregnetFodselsar3 shouldBeEqualTo 1899
            beregnetFodselsar4 shouldBeEqualTo 1899
        }

        it("Skal håndtere fødselsnummer fra 1900-1999") {
            val beregnetFodselsar1 = extractBornYear("01010000000")
            val beregnetFodselsar2 = extractBornYear("01010049900")
            val beregnetFodselsar3 = extractBornYear("01019900000")
            val beregnetFodselsar4 = extractBornYear("01019949900")

            beregnetFodselsar1 shouldBeEqualTo 1900
            beregnetFodselsar2 shouldBeEqualTo 1900
            beregnetFodselsar3 shouldBeEqualTo 1999
            beregnetFodselsar4 shouldBeEqualTo 1999
        }

        it("Skal håndtere fødselsnummer fra 1940-1999") {
            val beregnetFodselsar1 = extractBornYear("01014090000")
            val beregnetFodselsar2 = extractBornYear("01014099900")
            val beregnetFodselsar3 = extractBornYear("01019990000")
            val beregnetFodselsar4 = extractBornYear("01019999900")

            beregnetFodselsar1 shouldBeEqualTo 1940
            beregnetFodselsar2 shouldBeEqualTo 1940
            beregnetFodselsar3 shouldBeEqualTo 1999
            beregnetFodselsar4 shouldBeEqualTo 1999
        }

        it("Skal håndtere fødselsnummer fra 2000-2039") {
            val beregnetFodselsar1 = extractBornYear("01010050000")
            val beregnetFodselsar2 = extractBornYear("01010099900")
            val beregnetFodselsar3 = extractBornYear("01013950000")
            val beregnetFodselsar4 = extractBornYear("01013999900")

            beregnetFodselsar1 shouldBeEqualTo 2000
            beregnetFodselsar2 shouldBeEqualTo 2000
            beregnetFodselsar3 shouldBeEqualTo 2039
            beregnetFodselsar4 shouldBeEqualTo 2039
        }

        it("Should check rule ICPC_2_Z_DIAGNOSE,should trigger rule") {
            val healthInformation = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnosekoder.icpc2["Z09"]!!.toDiagnose()
                )
            )

            ValidationRuleChain.ICPC_2_Z_DIAGNOSE(ruleData(healthInformation)) shouldBeEqualTo true
        }

        it("Should check rule ICPC_2_Z_DIAGNOSE,should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnosekoder.icpc2["A09"]!!.toDiagnose()
                )
            )

            ValidationRuleChain.ICPC_2_Z_DIAGNOSE(ruleData(healthInformation)) shouldBeEqualTo false
        }

        it("Should check rule ICPC_2_Z_DIAGNOSE,should NOT trigger rule") {
            val healthInformation = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.7170",
                        kode = "A62",
                        tekst = "Brudd legg/ankel"
                    )
                )
            )

            ValidationRuleChain.ICPC_2_Z_DIAGNOSE(ruleData(healthInformation)) shouldBeEqualTo false
        }

        it("Should check rule HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER,should trigger rule") {
            val healthInformation = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = null,
                    annenFraversArsak = null
                )
            )

            ValidationRuleChain.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER(ruleData(healthInformation)) shouldBeEqualTo true
        }

        it("Should check rule HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER,should NOT trigger rule") {
            val healthInformation = generateSykmelding()

            ValidationRuleChain.HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER(ruleData(healthInformation)) shouldBeEqualTo false
        }

        it("Should check rule UKJENT_DIAGNOSEKODETYPE,should trigger rule") {
            val healthInformation = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.9999",
                        kode = "A09",
                        tekst = "Brudd legg/ankel"
                    )
                )
            )

            ValidationRuleChain.UKJENT_DIAGNOSEKODETYPE(ruleData(healthInformation)) shouldBeEqualTo true
        }

        it("Should check rule UKJENT_DIAGNOSEKODETYPE,should NOT trigger rule") {
            val healthInformation = generateSykmelding()

            ValidationRuleChain.UKJENT_DIAGNOSEKODETYPE(ruleData(healthInformation)) shouldBeEqualTo false
        }

        it("Should check rule UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE, wrong kodeverk for hoveddiagnose") {
            val healthInformation = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.7110",
                        kode = "Z09",
                        tekst = "Brudd legg/ankel"
                    )
                )
            )

            ValidationRuleChain.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE(ruleData(healthInformation)) shouldBeEqualTo true
        }

        it("Should not trigger rule UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE, wrong kodeverk for hoveddiagnose") {
            val healthInformation = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = Diagnose(
                        system = "2.16.578.1.12.4.1.1.7170",
                        kode = "L92",
                        tekst = "Brudd legg/ankel"
                    )
                )
            )

            ValidationRuleChain.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE(ruleData(healthInformation)) shouldBeEqualTo false
        }

        it("Should not trigger rule UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE, wrong kodeverk for hoveddiagnose") {
            val healthInformation = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    annenFraversArsak = AnnenFraversArsak(
                        "innlagt på instuisjon",
                        listOf(AnnenFraverGrunn.GODKJENT_HELSEINSTITUSJON)
                    )
                )
            )

            ValidationRuleChain.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE(ruleData(healthInformation)) shouldBeEqualTo false
        }

        it("Should not trigger rule UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE, wrong kodeverk for hoveddiagnose") {
            val healthInformation = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    hovedDiagnose = null,
                    annenFraversArsak = AnnenFraversArsak(
                        "innlagt på instuisjon",
                        listOf(AnnenFraverGrunn.GODKJENT_HELSEINSTITUSJON)
                    )
                )
            )

            ValidationRuleChain.UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE(ruleData(healthInformation)) shouldBeEqualTo false
        }

        it("Should check rule UGYLDIG_KODEVERK_FOR_BIDIAGNOSE, wrong kodeverk for biDiagnoser") {
            val healthInformation = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    bidiagnoser = listOf(
                        Diagnose(
                            system = "2.16.578.1.12.4.1.1.7110",
                            kode = "Z09",
                            tekst = "Brudd legg/ankel"
                        )
                    )
                )
            )

            ValidationRuleChain.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE(ruleData(healthInformation)) shouldBeEqualTo true
        }

        it("Should check rule UGYLDIG_KODEVERK_FOR_BIDIAGNOSE, correct kodeverk for biDiagnoser") {
            val healthInformation = generateSykmelding(
                medisinskVurdering = generateMedisinskVurdering(
                    bidiagnoser = listOf(
                        Diagnose(
                            system = "2.16.578.1.12.4.1.1.7170",
                            kode = "L92",
                            tekst = "Brudd legg/ankel"
                        )
                    )
                )
            )

            ValidationRuleChain.UGYLDIG_KODEVERK_FOR_BIDIAGNOSE(ruleData(healthInformation)) shouldBeEqualTo false
        }

//        it("Should check rule MANGLENDE_PAKREVDE_DYNAMISKE_SPORSMAL, missing utdypendeOpplysninger") {
//            val healthInformation = generateSykmelding(perioder = listOf(
//                    generatePeriode(
//                            fom = LocalDate.of(2018, 1, 7),
//                            tom = LocalDate.of(2018, 3, 9)
//                    )
//            ))
//
//            ValidationRuleChain.MANGLENDE_PAKREVDE_DYNAMISKE_SPORSMAL(ruleData(healthInformation)) shouldEqual true
//        }
//
//        it("MANGLENDE_PAKREVDE_DYNAMISKE_SPORSMAL should not hit whenever there is no period longer then 8 weeks") {
//            val healthInformation = generateSykmelding(perioder = listOf(
//                    generatePeriode(
//                            fom = LocalDate.of(2018, 1, 7),
//                            tom = LocalDate.of(2018, 1, 9)
//                    )
//            ))
//
//            ValidationRuleChain.MANGLENDE_PAKREVDE_DYNAMISKE_SPORSMAL(ruleData(healthInformation)) shouldEqual false
//        }
//
//        it("Should check rule MANGLENDE_PAKREVDE_DYNAMISKE_SPORSMAL, should NOT trigger rule") {
//            val healthInformation = generateSykmelding(
//                    perioder = listOf(
//                            generatePeriode(
//                                    fom = LocalDate.of(2018, 1, 7),
//                                    tom = LocalDate.of(2018, 3, 9)
//                            )
//                    ),
//                    utdypendeOpplysninger = mapOf(
//                            QuestionGroup.GROUP_6_2.spmGruppeId to mapOf(
//                                    QuestionId.ID_6_2_1.spmId to SporsmalSvar("Pasienten er syk", listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)),
//                                    QuestionId.ID_6_2_2.spmId to SporsmalSvar("Pasienten er syk", listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)),
//                                    QuestionId.ID_6_2_3.spmId to SporsmalSvar("Pasienten er syk", listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)),
//                                    QuestionId.ID_6_2_4.spmId to SporsmalSvar("Pasienten er syk", listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER))
//                            )
//                    )
//            )
//
//            ValidationRuleChain.MANGLENDE_PAKREVDE_DYNAMISKE_SPORSMAL(ruleData(healthInformation)) shouldEqual false
//        }
//
//        it("Should check rule UGYLDIG_REGELSETTVERSJON, should trigger rule") {
//            ValidationRuleChain.UGYLDIG_REGELSETTVERSJON(ruleData(generateSykmelding(), rulesetVersion = "999")) shouldEqual true
//        }
//
//        it("Should check rule UGYLDIG_REGELSETTVERSJON, should NOT trigger rule") {
//            ValidationRuleChain.UGYLDIG_REGELSETTVERSJON(ruleData(generateSykmelding(), rulesetVersion = "2")) shouldEqual false
//        }
//
//        it("Should check rule MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_17, should trigger rule") {
//            val healthInformation = generateSykmelding(
//                    perioder = listOf(generatePeriode(
//                            fom = LocalDate.of(2018, 1, 7),
//                            tom = LocalDate.of(2018, 9, 9)
//                    )),
//                    utdypendeOpplysninger = mapOf(
//                            QuestionGroup.GROUP_6_3.spmGruppeId to mapOf(
//                                    QuestionId.ID_6_3_1.spmId to SporsmalSvar("Pasienten er syk", listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER))
//                            )
//                    )
//            )
//
//            ValidationRuleChain.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_17(ruleData(healthInformation, rulesetVersion = "2")) shouldEqual true
//        }
//
//        it("Should check rule MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_7, should NOT trigger rule") {
//            val healthInformation = generateSykmelding(
//                    perioder = listOf(generatePeriode(
//                            fom = LocalDate.of(2018, 1, 7),
//                            tom = LocalDate.of(2018, 9, 9)
//                    )),
//                    utdypendeOpplysninger = mapOf(
//                            QuestionGroup.GROUP_6_3.spmGruppeId to mapOf(
//                                    QuestionId.ID_6_3_1.spmId to SporsmalSvar("Pasienten er syk", listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)),
//                                    QuestionId.ID_6_3_2.spmId to SporsmalSvar("Pasienten er syk", listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER))
//                            )
//                    )
//            )
//
//            ValidationRuleChain.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_7(ruleData(healthInformation, rulesetVersion = "2")) shouldEqual false
//        }
//
//        it("MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_7 should trigger on missing UtdypendeOpplysninger") {
//            val healthInformation = generateSykmelding(
//                    perioder = listOf(generatePeriode(
//                            fom = LocalDate.of(2018, 1, 7),
//                            tom = LocalDate.of(2018, 9, 9)
//                    ))
//            )
//
//            ValidationRuleChain.MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_7(ruleData(healthInformation, rulesetVersion = "2")) shouldEqual true
//        }

        it("UGYLDIG_ORGNR_LENGDE should trigger on when orgnr lengt is not 9") {
            val healthInformation = generateSykmelding()

            ValidationRuleChain.UGYLDIG_ORGNR_LENGDE(
                ruleData(
                    healthInformation,
                    legekontorOrgNr = "1234567890"
                )
            ) shouldBeEqualTo true
        }

        it("UGYLDIG_ORGNR_LENGDE should not trigger on when orgnr is 9") {
            val healthInformation = generateSykmelding()

            ValidationRuleChain.UGYLDIG_ORGNR_LENGDE(
                ruleData(
                    healthInformation,
                    legekontorOrgNr = "123456789"
                )
            ) shouldBeEqualTo false
        }

        it("AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR should trigger on when avsender fnr and pasient fnr is the same") {
            val healthInformation = generateSykmelding()

            ValidationRuleChain.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(
                ruleData(
                    healthInformation,
                    patientPersonNumber = "123456789",
                    avsenderfnr = "123456789"
                )
            ) shouldBeEqualTo true
        }

        it("AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR should not trigger on when avsender fnr and pasient fnr is diffrent") {
            val healthInformation = generateSykmelding()

            ValidationRuleChain.AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(
                ruleData(
                    healthInformation,
                    patientPersonNumber = "645646666",
                    avsenderfnr = "123456789"
                )
            ) shouldBeEqualTo false
        }

        it("BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR should trigger on when behandler fnr and pasient fnr is the same") {
            val behandlerFnr = "123456789"
            val healthInformation = generateSykmelding(
                behandler = generateBehandler(
                    "Per", "", "Hansen", "134", "113", behandlerFnr
                )
            )

            ValidationRuleChain.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR(
                ruleData(
                    healthInformation,
                    patientPersonNumber = behandlerFnr
                )
            ) shouldBeEqualTo true
        }

        it("BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR should not trigger on when behandler fnr and pasient fnr is diffrent") {
            val behandlerFnr = "123456789"
            val healthInformation = generateSykmelding(
                behandler = generateBehandler(
                    "Per", "", "Hansen", "134", "113", behandlerFnr
                )
            )

            ValidationRuleChain.BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR(
                ruleData(
                    healthInformation,
                    patientPersonNumber = "645646666"
                )
            ) shouldBeEqualTo false
        }
    }
})

fun generatePersonNumber(bornDate: LocalDate, useDNumber: Boolean = false): String {
    val personDate = bornDate.format(personNumberDateFormat).let {
        if (useDNumber) "${it[0] + 4}${it.substring(1)}" else it
    }
    return (if (bornDate.year >= 2000) (75011..99999) else (11111..50099))
        .map { "$personDate$it" }
        .first {
            validatePersonAndDNumber(it)
        }
}
