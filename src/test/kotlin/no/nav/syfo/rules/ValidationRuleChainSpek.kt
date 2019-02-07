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
import no.nav.syfo.ICPC2
import no.nav.syfo.Kodeverk
import no.nav.syfo.Rule
import no.nav.syfo.RuleData
import no.nav.syfo.SpmId
import no.nav.syfo.UtdypendeOpplysninger
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
    fun Kodeverk.toCV() = CV().apply {
        dn = text
        s = oid
        v = codeValue
    }

    fun ruleData(
        healthInformation: HelseOpplysningerArbeidsuforhet,
        receivedDate: LocalDateTime = LocalDateTime.now(),
        signatureDate: LocalDateTime = LocalDateTime.now()
    ): RuleData<RuleMetadata> = RuleData(healthInformation, RuleMetadata(signatureDate, receivedDate))


    describe("Testing validation rules and checking the rule outcomes") {

        it("Should check rule INVALID_FNR_SIZE, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
                    fodselsnummer = Ident().apply {
                        id = "3006310441"
                        typeId = CV().apply {
                            dn = "Fødselsnummer"
                            s = "2.16.578.1.12.4.1.1.8116"
                            v = "FNR"
                        }
                    }
                }
            }
            ValidationRuleChain.INVALID_FNR_SIZE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule INVALID_FNR_SIZE, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
                    fodselsnummer = Ident().apply {
                        id = "04030350265"
                        typeId = CV().apply {
                            dn = "Fødselsnummer"
                            s = "2.16.578.1.12.4.1.1.8116"
                            v = "FNR"
                        }
                    }
                }
            }

            ValidationRuleChain.INVALID_FNR_SIZE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule INVALID_FNR,should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
                    fodselsnummer = Ident().apply {
                        id = "30063104424"
                        typeId = CV().apply {
                            dn = "Fødselsnummer"
                            s = "2.16.578.1.12.4.1.1.8116"
                            v = "FNR"
                        }
                    }
                }
            }

            ValidationRuleChain.INVALID_FNR(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule INVALID_FNR,should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
                fodselsnummer = Ident().apply {
                    id = "04030350265"
                    typeId = CV().apply {
                            dn = "Fødselsnummer"
                            s = "2.16.578.1.12.4.1.1.8116"
                            v = "FNR"
                        }
                    }
                }
            }

            ValidationRuleChain.INVALID_FNR(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule YOUNGER_THAN_13,should trigger rule") {
            val person = fairy.person(
                    PersonProperties.ageBetween(PersonProvider.MIN_AGE, 12))

            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
                    fodselsnummer = Ident().apply {
                        id = generatePersonNumber(person.dateOfBirth, false)
                        typeId = CV().apply {
                            dn = "Fødselsnummer"
                            s = "2.16.578.1.12.4.1.1.8116"
                            v = "FNR"
                        }
                    }
                }
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now()
                    })
                }
            }

            ValidationRuleChain.YOUNGER_THAN_13(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule YOUNGER_THAN_13,should NOT trigger rule") {
            val person = fairy.person(
                    PersonProperties.ageBetween(13, 70))

            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
                    fodselsnummer = Ident().apply {
                        id = generatePersonNumber(person.dateOfBirth, false)
                        typeId = CV().apply {
                            dn = "Fødselsnummer"
                            s = "2.16.578.1.12.4.1.1.8116"
                            v = "FNR"
                        }
                    }
                }
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now()
                    })
                }
            }

            ValidationRuleChain.YOUNGER_THAN_13(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule PATIENT_OVER_70_YEARS,should trigger rule") {
            val person = fairy.person(
                    PersonProperties.ageBetween(71, 88))

            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
                    fodselsnummer = Ident().apply {
                        id = generatePersonNumber(person.dateOfBirth, false)
                        typeId = CV().apply {
                            dn = "Fødselsnummer"
                            s = "2.16.578.1.12.4.1.1.8116"
                            v = "FNR"
                        }
                    }
                }
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now()
                    })
                }
            }

            ValidationRuleChain.PATIENT_OVER_70_YEARS(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule PATIENT_OVER_70_YEARS,should NOT trigger rule") {
            val person = fairy.person(
                    PersonProperties.ageBetween(13, 70))

            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
                    fodselsnummer = Ident().apply {
                        id = generatePersonNumber(person.dateOfBirth, false)
                        typeId = CV().apply {
                            dn = "Fødselsnummer"
                            s = "2.16.578.1.12.4.1.1.8116"
                            v = "FNR"
                        }
                    }
                }
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                        periodeFOMDato = LocalDate.now()
                        periodeTOMDato = LocalDate.now()
                    })
                }
            }

            ValidationRuleChain.PATIENT_OVER_70_YEARS(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule ICPC_2_Z_DIAGNOSE,should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
                    hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                        diagnosekode = ICPC2.Z09.toCV()
                    }
                }
            }

            ValidationRuleChain.ICPC_2_Z_DIAGNOSE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule ICPC_2_Z_DIAGNOSE,should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
                    hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                        diagnosekode = ICPC2.A09.toCV()
                    }
                }
            }

            ValidationRuleChain.ICPC_2_Z_DIAGNOSE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule MAIN_DIAGNOSE_MISSING_AND_MISSING_REASON,should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet()

            ValidationRuleChain.MAIN_DIAGNOSE_MISSING_AND_MISSING_REASON(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule MAIN_DIAGNOSE_MISSING_AND_MISSING_REASON,should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
                    hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                        diagnosekode = ICPC2.A09.toCV()
                    }
                }
            }

            ValidationRuleChain.MAIN_DIAGNOSE_MISSING_AND_MISSING_REASON(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule UNKNOWN_DIAGNOSECODE_TYPE,should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
                    hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                        diagnosekode = CV().apply {
                            s = "2.16.578.1.12.4.1.1.9999"
                        }
                    }
                }
            }

            ValidationRuleChain.UNKNOWN_DIAGNOSECODE_TYPE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule UNKNOWN_DIAGNOSECODE_TYPE,should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
                    hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                        diagnosekode = ICPC2.A09.toCV()
                    }
                }
            }

            ValidationRuleChain.UNKNOWN_DIAGNOSECODE_TYPE(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule INVALID_KODEVERK_FOR_MAIN_DIAGNOSE, wrong kodeverk for hoveddiagnose") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
                    hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                        diagnosekode = CV().apply {
                            dn = "Problem med jus/politi"
                            s = "2.16.578.1.12.4.1.1.7110"
                            v = "Z09"
                        }
                    }
                }
            }

            ValidationRuleChain.INVALID_KODEVERK_FOR_MAIN_DIAGNOSE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule INVALID_KODEVERK_FOR_MAIN_DIAGNOSE,no kodeverk for hoveddiagnose") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
                    hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                        diagnosekode = CV().apply {
                            dn = "Problem med jus/politi"
                            v = "Z09"
                        }
                    }
                }
            }

            ValidationRuleChain.INVALID_KODEVERK_FOR_MAIN_DIAGNOSE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule INVALID_KODEVERK_FOR_BI_DIAGNOSE, no kodeverk for biDiagnoser") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
                    biDiagnoser = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.BiDiagnoser().apply {
                        diagnosekode.add(CV().apply {
                            dn = "Problem med jus/politi"
                            v = "Z09"
                        })
                    }
                }
            }

            ValidationRuleChain.INVALID_KODEVERK_FOR_BI_DIAGNOSE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule INVALID_KODEVERK_FOR_BI_DIAGNOSE, wrong kodeverk for biDiagnoser") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
                    biDiagnoser = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.BiDiagnoser().apply {
                        diagnosekode.add(CV().apply {
                            dn = "Problem med jus/politi"
                            s = "2.16.578.1.12.4.1.1.7110"
                            v = "Z09"
                        })
                    }
                }
            }
            ValidationRuleChain.INVALID_KODEVERK_FOR_BI_DIAGNOSE(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule NO_MEDICAL_OR_WORKPLACE_RELATED_REASONS, no medical related resons") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
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
                }
                medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
                    hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                        diagnosekode = ICPC2.Z09.toCV()
                    }
                }
            }

            ValidationRuleChain.NO_MEDICAL_OR_WORKPLACE_RELATED_REASONS(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule NO_MEDICAL_OR_WORKPLACE_RELATED_REASONS, no workplace related reasons") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
                    hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                        diagnosekode = ICPC2.Z09.toCV()
                    }
                }
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
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
                }
            }

            ValidationRuleChain.NO_MEDICAL_OR_WORKPLACE_RELATED_REASONS(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule MISSING_REQUIRED_DYNAMIC_QUESTIONS, missing utdypendeOpplysninger") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                medisinskVurdering = HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
                    hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                        diagnosekode = ICPC2.Z09.toCV()
                    }
                }
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
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
                }
            }

            ValidationRuleChain.MISSING_REQUIRED_DYNAMIC_QUESTIONS(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule MISSING_REQUIRED_DYNAMIC_QUESTIONS, missing dynaRegelgruppe 6.2") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
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
                }

                utdypendeOpplysninger = HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger().apply {
                    spmGruppe.add(HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger.SpmGruppe().apply {
                        spmGruppeId = "6.3"
                    })
                }
            }

            ValidationRuleChain.MISSING_REQUIRED_DYNAMIC_QUESTIONS(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule MISSING_REQUIRED_DYNAMIC_QUESTIONS, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
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
                }
                utdypendeOpplysninger = HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger().apply {
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
            }

            ValidationRuleChain.MISSING_REQUIRED_DYNAMIC_QUESTIONS(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule INVALID_RULESET_VERSION, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                regelSettVersjon = "999"
            }

            ValidationRuleChain.INVALID_RULESET_VERSION(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule INVALID_RULESET_VERSION, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                regelSettVersjon = "2"
            }

            ValidationRuleChain.INVALID_RULESET_VERSION(ruleData(healthInformation)) shouldEqual false
        }

        it("Should check rule MISSING_REQUIRED_DYNAMIC_QUESTIONS_AFTER_RULE_SET_VERSION_2, should trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                regelSettVersjon = "2"
                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
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
                        periodeTOMDato = LocalDate.of(2018, 9, 9)
                    })
                }
                utdypendeOpplysninger = HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger().apply {
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
            }

            ValidationRuleChain.MISSING_REQUIRED_DYNAMIC_QUESTIONS_AFTER_RULE_SET_VERSION_2(ruleData(healthInformation)) shouldEqual true
        }

        it("Should check rule MISSING_REQUIRED_DYNAMIC_QUESTIONS_AFTER_RULE_SET_VERSION_2, should NOT trigger rule") {
            val healthInformation = HelseOpplysningerArbeidsuforhet().apply {
                regelSettVersjon = "2"

                aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                    periode.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
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
                        periodeTOMDato = LocalDate.of(2018, 9, 9)
                    })
                }

                utdypendeOpplysninger = HelseOpplysningerArbeidsuforhet.UtdypendeOpplysninger().apply {
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
            }

            ValidationRuleChain.MISSING_REQUIRED_DYNAMIC_QUESTIONS_AFTER_RULE_SET_VERSION_2(ruleData(healthInformation)) shouldEqual false
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
