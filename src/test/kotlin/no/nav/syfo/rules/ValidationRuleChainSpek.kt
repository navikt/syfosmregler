package no.nav.syfo.rules

import com.devskiller.jfairy.Fairy
import com.devskiller.jfairy.producer.person.PersonProperties
import com.devskiller.jfairy.producer.person.PersonProvider
import no.nav.helse.sm2013.ArsakType
import no.nav.helse.sm2013.CS
import no.nav.helse.sm2013.CV
import no.nav.helse.sm2013.DynaSvarType
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.helse.sm2013.Ident
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.SpmId
import no.nav.syfo.UtdypendeOpplysninger
import no.nav.syfo.executeFlow
import no.nav.syfo.validation.validatePersonAndDNumber
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val fairy: Fairy = Fairy.create(Locale("no", "NO"))
val personNumberDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy")

object ValidationRuleChainSpek : Spek({
    fun deafaultHelseOpplysningerArbeidsuforhet() = HelseOpplysningerArbeidsuforhet().apply {
        aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
            periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                periodeFOMDato = LocalDate.of(2018, 1, 7)
                periodeTOMDato = LocalDate.of(2018, 1, 9)
            })
        }

        pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
            fodselsnummer = Ident().apply {
                id = generatePersonNumber(LocalDate.of(1991, 1, 1), false)
                typeId = CV().apply {
                    dn = "Fødselsnummer"
                    s = "2.16.578.1.12.4.1.1.8116"
                    v = "FNR"
                }
            }
        }
        medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
        }
    }

    fun deafaultRuleMetadata() = RuleMetadata(receivedDate = LocalDateTime.now(),
            signatureDate = LocalDateTime.now())

    describe("Testing validation rules and checking the rule outcomes") {

        it("Should check rule 1002") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.pasient.fodselsnummer.id = "3006310441"

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.INVALID_FNR_SIZE } shouldEqual true
        }

        it("Should check rule 1006") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.pasient.fodselsnummer.id = "30063104424"

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.INVALID_FNR } shouldEqual true
        }

        it("Should check rule 1101") {
            val person = fairy.person(
                    PersonProperties.ageBetween(PersonProvider.MIN_AGE, 12))

            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.pasient.fodselsnummer.id = generatePersonNumber(person.dateOfBirth, false)

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.YOUNGER_THAN_13 } shouldEqual true
        }

        it("Should check rule 1102") {
            val person = fairy.person(
                    PersonProperties.ageBetween(71, 88))

            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.pasient.fodselsnummer.id = generatePersonNumber(person.dateOfBirth, false)

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.PATIENT_OVER_70_YEARS } shouldEqual true
        }

        it("Should check rule 1132") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.medisinskVurdering.hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    dn = "Problem med jus/politi"
                    s = "2.16.578.1.12.4.1.1.7170"
                    v = "Z09"
                }
            }

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.ICPC_2_Z_DIAGNOSE } shouldEqual true
        }

        it("Should check rule 1133") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.MAIN_DIAGNOSE_MISSING_AND_MISSING_REASON } shouldEqual true
        }

        it("Should check rule 1137") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.medisinskVurdering.hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    s = "2.16.578.1.12.4.1.1.9999"
                }
            }

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.UNKNOWN_DIAGNOSECODE_TYPE } shouldEqual true
        }

        it("Should check rule 1540, worng kodeverk for hoveddiagnose") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.medisinskVurdering.hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    dn = "Problem med jus/politi"
                    s = "2.16.578.1.12.4.1.1.7110"
                    v = "Z09"
                }
            }

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.INVALID_KODEVERK_FOR_MAIN_DIAGNOSE } shouldEqual true
        }

        it("Should check rule 1540,no kodeverk for hoveddiagnose") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.medisinskVurdering.hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    dn = "Problem med jus/politi"
                    v = "Z09"
                }
            }

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.INVALID_KODEVERK_FOR_MAIN_DIAGNOSE } shouldEqual true
        }

        it("Should check rule 1541, no kodeverk for biDiagnoser") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.medisinskVurdering.hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    dn = "Problem med jus/politi"
                    s = "2.16.578.1.12.4.1.1.7170"
                    v = "Z09"
                }
            }
            healthInformation.medisinskVurdering.biDiagnoser = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.BiDiagnoser().apply {
                diagnosekode.add(CV().apply {
                    dn = "Problem med jus/politi"
                    v = "Z09"
                })
            }

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.INVALID_KODEVERK_FOR_BI_DIAGNOSE } shouldEqual true
        }

        it("Should check rule 1541, worng kodeverk for biDiagnoser") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.medisinskVurdering.hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    dn = "Problem med jus/politi"
                    s = "2.16.578.1.12.4.1.1.7170"
                    v = "Z09"
                }
            }
            healthInformation.medisinskVurdering.biDiagnoser = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.BiDiagnoser().apply {
                diagnosekode.add(CV().apply {
                    dn = "Problem med jus/politi"
                    s = "2.16.578.1.12.4.1.1.7110"
                    v = "Z09"
                })
            }

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.INVALID_KODEVERK_FOR_BI_DIAGNOSE } shouldEqual true
        }

        it("Should check rule 1706, no medical related resons") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.medisinskVurdering.hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    dn = "Problem med jus/politi"
                    s = "2.16.578.1.12.4.1.1.7170"
                    v = "Z09"
                }
            }
            healthInformation.aktivitet.periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                aktivitetIkkeMulig = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AktivitetIkkeMulig().apply {
                    arbeidsplassen = ArsakType().apply {
                        arsakskode.add(CS().apply {
                            dn = "Helsetilstanden hindrer pasienten i å være i aktivitet"
                            v = "1"
                        })
                        beskriv = "Tungt arbeid"
                    }
                }
                periodeFOMDato = LocalDate.of(2018, 1, 7)
                periodeTOMDato = LocalDate.of(2018, 1, 9)
            })

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.NO_MEDICAL_OR_WORKPLACE_RELATED_REASONS } shouldEqual true
        }

        it("Should check rule 1706, no workplace related resons") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.medisinskVurdering.hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    dn = "Problem med jus/politi"
                    s = "2.16.578.1.12.4.1.1.7170"
                    v = "Z09"
                }
            }

            healthInformation.aktivitet.periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                aktivitetIkkeMulig = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AktivitetIkkeMulig().apply {
                    medisinskeArsaker = ArsakType().apply {
                        arsakskode.add(CS().apply {
                            dn = "Helsetilstanden hindrer pasienten i å være i aktivitet"
                            v = "1"
                        })
                        beskriv = "Tungt arbeid"
                    }
                }
                periodeFOMDato = LocalDate.of(2018, 1, 7)
                periodeTOMDato = LocalDate.of(2018, 1, 9)
            })

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.NO_MEDICAL_OR_WORKPLACE_RELATED_REASONS } shouldEqual true
        }

        it("Should check rule 1707, missing utdypendeOpplysninger") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.medisinskVurdering.hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    dn = "Problem med jus/politi"
                    s = "2.16.578.1.12.4.1.1.7170"
                    v = "Z09"
                }
            }

            healthInformation.aktivitet.periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                aktivitetIkkeMulig = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AktivitetIkkeMulig().apply {
                    medisinskeArsaker = ArsakType().apply {
                        arsakskode.add(CS().apply {
                            dn = "Helsetilstanden hindrer pasienten i å være i aktivitet"
                            v = "1"
                        })
                        beskriv = "Tungt arbeid"
                    }
                }
                periodeFOMDato = LocalDate.of(2018, 1, 7)
                periodeTOMDato = LocalDate.of(2018, 3, 9)
            })

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.MISSING_REQUIRED_DYNAMIC_QUESTIONS } shouldEqual true
        }

        it("Should check rule 1707, missing dynaRegelgruppe 6.2") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.medisinskVurdering.hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    dn = "Problem med jus/politi"
                    s = "2.16.578.1.12.4.1.1.7170"
                    v = "Z09"
                }
            }

            healthInformation.aktivitet.periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                aktivitetIkkeMulig = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AktivitetIkkeMulig().apply {
                    medisinskeArsaker = ArsakType().apply {
                        arsakskode.add(CS().apply {
                            dn = "Helsetilstanden hindrer pasienten i å være i aktivitet"
                            v = "1"
                        })
                        beskriv = "Tungt arbeid"
                    }
                }
                periodeFOMDato = LocalDate.of(2018, 1, 7)
                periodeTOMDato = LocalDate.of(2018, 3, 9)
            })

            healthInformation.utdypendeOpplysninger = HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger().apply {
                spmGruppe.add(HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger.SpmGruppe().apply {
                    spmGruppeId = "6.3"
                })
            }

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.MISSING_REQUIRED_DYNAMIC_QUESTIONS } shouldEqual true
        }

        it("Should check rule 1707, should not trigger rule") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.medisinskVurdering.hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    dn = "Problem med jus/politi"
                    s = "2.16.578.1.12.4.1.1.7170"
                    v = "Z09"
                }
            }
            healthInformation.aktivitet.periode.first().aktivitetIkkeMulig = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AktivitetIkkeMulig().apply {
                    medisinskeArsaker = ArsakType().apply {
                        arsakskode.add(CS().apply {
                            dn = "Helsetilstanden hindrer pasienten i å være i aktivitet"
                            v = "1"
                        })
                        beskriv = "Tungt arbeid"
                    }
                }

            healthInformation.aktivitet.periode.first().periodeFOMDato = LocalDate.of(2018, 1, 7)
            healthInformation.aktivitet.periode.first().periodeTOMDato = LocalDate.of(2018, 9, 9)

            healthInformation.utdypendeOpplysninger = HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger().apply {
                spmGruppe.add(HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger.SpmGruppe().apply {
                    spmGruppeId = UtdypendeOpplysninger.DYNAGRUPPE6_2.spmGruppeId
                    spmGruppeTekst = UtdypendeOpplysninger.DYNAGRUPPE6_2.spmGruppeTekst
                    spmSvar.add(DynaSvarType().apply {
                        spmId = SpmId.SpmId6_2_1.spmId
                        spmTekst = SpmId.SpmId6_2_1.spmTekst
                        restriksjon = DynaSvarType.Restriksjon().apply {
                            restriksjonskode.add(CS().apply {
                                dn = SpmId.SpmId6_2_1.restriksjon.text
                                v = SpmId.SpmId6_2_1.restriksjon.codeValue
                            })
                        }
                        svarTekst = "Pasienten har vært meget syk, og er ofte syk"
                    })

                    spmSvar.add(DynaSvarType().apply {
                        spmId = SpmId.SpmId6_2_2.spmId
                        spmTekst = SpmId.SpmId6_2_2.spmTekst
                        restriksjon = DynaSvarType.Restriksjon().apply {
                            restriksjonskode.add(CS().apply {
                                dn = SpmId.SpmId6_2_2.restriksjon.text
                                v = SpmId.SpmId6_2_2.restriksjon.codeValue
                            })
                        }
                        svarTekst = "Pasienten har vært meget syk, og er ofte syk"
                    })

                    spmSvar.add(DynaSvarType().apply {
                        spmId = SpmId.SpmId6_2_3.spmId
                        spmTekst = SpmId.SpmId6_2_3.spmTekst
                        restriksjon = DynaSvarType.Restriksjon().apply {
                            restriksjonskode.add(CS().apply {
                                dn = SpmId.SpmId6_2_3.restriksjon.text
                                v = SpmId.SpmId6_2_3.restriksjon.codeValue
                            })
                        }
                        svarTekst = "Pasienten har vært meget syk, og er ofte syk"
                    })

                    spmSvar.add(DynaSvarType().apply {
                        spmId = SpmId.SpmId6_2_4.spmId
                        spmTekst = SpmId.SpmId6_2_4.spmTekst
                        restriksjon = DynaSvarType.Restriksjon().apply {
                            restriksjonskode.add(CS().apply {
                                dn = SpmId.SpmId6_2_4.restriksjon.text
                                v = SpmId.SpmId6_2_4.restriksjon.codeValue
                            })
                        }
                        svarTekst = "Pasienten har vært meget syk, og er ofte syk"
                    })
                })
            }

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.MISSING_REQUIRED_DYNAMIC_QUESTIONS } shouldEqual false
        }

        it("Should check rule 1708, should trigger rule") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.regelSettVersjon = "999"

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.INVALID_RULESET_VERSION } shouldEqual true
        }

        it("Should check rule 1709, should trigger rule") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.regelSettVersjon = "2"

            healthInformation.medisinskVurdering.hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    dn = "Problem med jus/politi"
                    s = "2.16.578.1.12.4.1.1.7170"
                    v = "Z09"
                }
            }
            healthInformation.aktivitet.periode.first().aktivitetIkkeMulig = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AktivitetIkkeMulig().apply {
                medisinskeArsaker = ArsakType().apply {
                    arsakskode.add(CS().apply {
                        dn = "Helsetilstanden hindrer pasienten i å være i aktivitet"
                        v = "1"
                    })
                    beskriv = "Tungt arbeid"
                }
            }

            healthInformation.aktivitet.periode.first().periodeFOMDato = LocalDate.of(2018, 1, 7)
            healthInformation.aktivitet.periode.first().periodeTOMDato = LocalDate.of(2018, 9, 9)

            healthInformation.utdypendeOpplysninger = HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger().apply {
                spmGruppe.add(HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger.SpmGruppe().apply {
                    spmGruppeId = UtdypendeOpplysninger.DYNAGRUPPE6_3.spmGruppeId
                    spmGruppeTekst = UtdypendeOpplysninger.DYNAGRUPPE6_3.spmGruppeTekst
                    spmSvar.add(DynaSvarType().apply {
                        spmId = SpmId.SpmId6_3_1.spmId
                        spmTekst = SpmId.SpmId6_3_1.spmTekst
                        restriksjon = DynaSvarType.Restriksjon().apply {
                            restriksjonskode.add(CS().apply {
                                dn = SpmId.SpmId6_3_1.restriksjon.text
                                v = SpmId.SpmId6_3_1.restriksjon.codeValue
                            })
                        }
                        svarTekst = "Pasienten har vært meget syk, og er ofte syk"
                    })

                    spmSvar.add(DynaSvarType().apply {
                        spmId = SpmId.SpmId6_3_2.spmId
                        spmTekst = SpmId.SpmId6_3_2.spmTekst
                        restriksjon = DynaSvarType.Restriksjon().apply {
                            restriksjonskode.add(CS().apply {
                                dn = SpmId.SpmId6_3_2.restriksjon.text
                                v = SpmId.SpmId6_3_2.restriksjon.codeValue
                            })
                        }
                    })
                })
            }

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.MISSING_REQUIRED_DYNAMIC_QUESTIONS_AFTER_RULE_SET_VERSION_2 } shouldEqual true
        }

        it("Should check rule 1709, should not trigger rule") {
            val healthInformation = deafaultHelseOpplysningerArbeidsuforhet()
            healthInformation.regelSettVersjon = "2"

            healthInformation.medisinskVurdering.hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = CV().apply {
                    dn = "Problem med jus/politi"
                    s = "2.16.578.1.12.4.1.1.7170"
                    v = "Z09"
                }
            }
            healthInformation.aktivitet.periode.first().aktivitetIkkeMulig = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AktivitetIkkeMulig().apply {
                medisinskeArsaker = ArsakType().apply {
                    arsakskode.add(CS().apply {
                        dn = "Helsetilstanden hindrer pasienten i å være i aktivitet"
                        v = "1"
                    })
                    beskriv = "Tungt arbeid"
                }
            }

            healthInformation.aktivitet.periode.first().periodeFOMDato = LocalDate.of(2018, 1, 7)
            healthInformation.aktivitet.periode.first().periodeTOMDato = LocalDate.of(2018, 9, 9)

            healthInformation.utdypendeOpplysninger = HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger().apply {
                spmGruppe.add(HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger.SpmGruppe().apply {
                    spmGruppeId = UtdypendeOpplysninger.DYNAGRUPPE6_3.spmGruppeId
                    spmGruppeTekst = UtdypendeOpplysninger.DYNAGRUPPE6_3.spmGruppeTekst
                    spmSvar.add(DynaSvarType().apply {
                        spmId = SpmId.SpmId6_3_1.spmId
                        spmTekst = SpmId.SpmId6_3_1.spmTekst
                        restriksjon = DynaSvarType.Restriksjon().apply {
                            restriksjonskode.add(CS().apply {
                                dn = SpmId.SpmId6_3_1.restriksjon.text
                                v = SpmId.SpmId6_3_1.restriksjon.codeValue
                            })
                        }
                        svarTekst = "Pasienten har vært meget syk, og er ofte syk"
                    })

                    spmSvar.add(DynaSvarType().apply {
                        spmId = SpmId.SpmId6_3_2.spmId
                        spmTekst = SpmId.SpmId6_3_2.spmTekst
                        restriksjon = DynaSvarType.Restriksjon().apply {
                            restriksjonskode.add(CS().apply {
                                dn = SpmId.SpmId6_3_2.restriksjon.text
                                v = SpmId.SpmId6_3_2.restriksjon.codeValue
                            })
                        }
                        svarTekst = "Pasienten har vært meget syk, og er ofte syk"
                    })
                })
            }

            val ruleMetadata = deafaultRuleMetadata()

            val validationRuleChainResults = listOf<List<Rule<RuleData<RuleMetadata>>>>(
                    ValidationRuleChain.values().toList()
            ).flatten().executeFlow(healthInformation, ruleMetadata)

            validationRuleChainResults.any { it == ValidationRuleChain.MISSING_REQUIRED_DYNAMIC_QUESTIONS_AFTER_RULE_SET_VERSION_2 } shouldEqual false
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
