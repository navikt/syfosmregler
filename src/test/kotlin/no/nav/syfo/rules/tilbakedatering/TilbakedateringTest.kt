package no.nav.syfo.rules.tilbakedatering

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.generateSykmelding
import no.nav.syfo.model.KontaktMedPasient
import no.nav.syfo.model.MedisinskVurdering
import no.nav.syfo.model.RuleMetadata
import no.nav.syfo.model.Status
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.rules.RuleMetadataSykmelding
import no.nav.syfo.sm.Diagnosekoder
import no.nav.syfo.toDiagnose
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate

class TilbakedateringTest : FunSpec({
    var ruleTree = RuleTree()

    context("Test tilbakedateringsregler mindre enn 9 dager") {
        test("ikke tilbakedatert, Status OK") {
            val sykmelding = generateSykmelding(
                fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 3).atStartOfDay()
            )
            val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
            val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)
            status shouldBeEqualTo Status.OK
        }

        test("tilbakedatert forlengelse") {
            val sykmelding = generateSykmelding(
                fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                behandletTidspunkt = LocalDate.of(2020, 1, 9).atStartOfDay(),
                kontaktMedPasient = KontaktMedPasient(null, null)
            )
            val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, true)
            val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)
            status shouldBeEqualTo Status.OK
        }

        context("Tilbakedatert") {
            context("med begrunnelse") {
                test("Med begrunnelse OK") {
                    val sykmelding = generateSykmelding(
                        fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                        behandletTidspunkt = LocalDate.of(2020, 1, 6).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, "arst"),
                    )
                    val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
                    val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)
                    status shouldBeEqualTo Status.OK
                }
                test("Med kontaktdato OK") {
                    val sykmelding = generateSykmelding(
                        fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                        behandletTidspunkt = LocalDate.of(2020, 1, 6).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(LocalDate.now(), null),
                    )
                    val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
                    val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)
                    status shouldBeEqualTo Status.OK
                }
            }

            context("Uten Begrunnelse") {
                test("Forlengelse, OK") {
                    val sykmelding = generateSykmelding(
                        fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                        behandletTidspunkt = LocalDate.of(2020, 1, 6).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, null),
                    )
                    val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), false, false)
                    val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)
                    status shouldBeEqualTo Status.OK
                }
                test("Ikke forlengelse, INVALID") {
                    val sykmelding = generateSykmelding(
                        fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                        behandletTidspunkt = LocalDate.of(2020, 1, 6).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, ""),
                    )
                    val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
                    val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)

                    status shouldBeEqualTo Status.INVALID
                }

                test("Ikke forlengelse, men fra spesialishelsetjenesten, OK") {
                    val sykmelding = generateSykmelding(
                        fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                        behandletTidspunkt = LocalDate.of(2020, 1, 6).atStartOfDay(),
                        kontaktMedPasient = KontaktMedPasient(null, ""),
                        medisinskVurdering = fraSpesialhelsetjenesten()
                    )
                    val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
                    val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)

                    status shouldBeEqualTo Status.OK
                }
            }
        }
    }

    context("Test tilbakedatering mellog 8 og 30 dager") {
        context("uten begrunnelse") {
            test("Fra Spesialhelsetjenesten, MANUELL") {
                val sykmelding = generateSykmelding(
                    fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                    behandletTidspunkt = LocalDate.of(2020, 1, 11).atStartOfDay(),
                    kontaktMedPasient = KontaktMedPasient(null, ""),
                    medisinskVurdering = fraSpesialhelsetjenesten()
                )
                val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
                val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)

                status shouldBeEqualTo Status.MANUAL_PROCESSING
            }
            test("Ikke fra spesialhelsetjenesten, INVALID") {
                val sykmelding = generateSykmelding(
                    fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                    behandletTidspunkt = LocalDate.of(2020, 1, 21).atStartOfDay(),
                    kontaktMedPasient = KontaktMedPasient(null, null)
                )
                val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
                val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)

                status shouldBeEqualTo Status.INVALID
            }
        }
        context("Med Begrunnelse") {
            test("ikke god nok begrunnelse, INVALID") {
                val sykmelding = generateSykmelding(
                    fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                    behandletTidspunkt = LocalDate.of(2020, 1, 11).atStartOfDay(),
                    kontaktMedPasient = KontaktMedPasient(null, "12344123112341232....,,,..12"),
                )
                val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
                val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)

                status shouldBeEqualTo Status.INVALID
            }
            test("Forlengelse, OK") {
                val sykmelding = generateSykmelding(
                    fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                    behandletTidspunkt = LocalDate.of(2020, 1, 11).atStartOfDay(),
                    kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmnopq")
                )
                val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), false, false)
                val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)

                status shouldBeEqualTo Status.OK
            }
            test("Ikke forlengelse, MANUELL") {
                val sykmelding = generateSykmelding(
                    fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 17),
                    behandletTidspunkt = LocalDate.of(2020, 1, 11).atStartOfDay(),
                    kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmnopq")
                )
                val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
                val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)

                status shouldBeEqualTo Status.MANUAL_PROCESSING
            }

            test("Innenfor arbeidsgiverperioden, OK") {
                val sykmelding = generateSykmelding(
                    fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 2),
                    behandletTidspunkt = LocalDate.of(2020, 1, 11).atStartOfDay(),
                    kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmnopq")
                )
                val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
                val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)

                status shouldBeEqualTo Status.OK
            }
            test("Utenfor arbeidsgiverperioden, MANUELL") {
                val sykmelding = generateSykmelding(
                    fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 20),
                    behandletTidspunkt = LocalDate.of(2020, 1, 11).atStartOfDay(),
                    kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmnopq")
                )
                val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
                val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)

                status shouldBeEqualTo Status.MANUAL_PROCESSING
            }

            test("Fra spesialisthelsetjenesten, OK") {
                val sykmelding = generateSykmelding(
                    fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 20),
                    behandletTidspunkt = LocalDate.of(2020, 1, 11).atStartOfDay(),
                    kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmnopq"),
                    medisinskVurdering = fraSpesialhelsetjenesten()
                )
                val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
                val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)

                status shouldBeEqualTo Status.OK
            }
        }
    }

    context("Over 30 dager") {
        test("Med begrunnelse, MANUELL") {
            val sykmelding = generateSykmelding(
                fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 20),
                behandletTidspunkt = LocalDate.of(2020, 2, 11).atStartOfDay(),
                kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmnopq")
            )
            val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
            val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)

            status shouldBeEqualTo Status.MANUAL_PROCESSING
        }

        test("Ikke god nok begrunnelse, INVALID") {
            val sykmelding = generateSykmelding(
                fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 20),
                behandletTidspunkt = LocalDate.of(2020, 1, 11).atStartOfDay(),
                kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmno")
            )
            val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
            val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)

            status shouldBeEqualTo Status.INVALID
        }

        test("Fra spesialisthelsetjenesten, MANUELL") {
            val sykmelding = generateSykmelding(
                fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2020, 1, 20),
                behandletTidspunkt = LocalDate.of(2020, 1, 11).atStartOfDay(),
                kontaktMedPasient = KontaktMedPasient(null, "abcdefghijklmno"),
                medisinskVurdering = fraSpesialhelsetjenesten()

            )
            val sykmeldingMetadata = RuleMetadataSykmelding(ruleMetadata = sykmelding.toRuleMetadata(), true, false)
            val status = ruleTree.runRuleChain(sykmelding, sykmeldingMetadata)

            status shouldBeEqualTo Status.MANUAL_PROCESSING
        }
    }
})

private fun fraSpesialhelsetjenesten() = MedisinskVurdering(
    hovedDiagnose =
    Diagnosekoder.icd10.values.first().toDiagnose(),
    emptyList(), false, false, null, null
)

fun Sykmelding.toRuleMetadata() = RuleMetadata(
    signatureDate = signaturDato,
    receivedDate = signaturDato,
    behandletTidspunkt = behandletTidspunkt,
    patientPersonNumber = "1",
    rulesetVersion = null,
    legekontorOrgnr = null,
    tssid = null,
    avsenderFnr = "2",
    pasientFodselsdato = LocalDate.now()
)
