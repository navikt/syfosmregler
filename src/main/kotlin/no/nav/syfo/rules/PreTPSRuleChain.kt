package no.nav.syfo.rules

import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.model.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.Diagnosekode
import no.nav.syfo.Rule
import no.nav.syfo.RuleChain
import no.nav.syfo.contains
import no.nav.syfo.get
import no.nav.syfo.model.Status
import no.nav.syfo.validation.extractBornDate
import no.nav.syfo.validation.validatePersonAndDNumber
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import no.trygdeetaten.xml.eiff._1.XMLMottakenhetBlokk
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.xml.datatype.XMLGregorianCalendar

val validationChain = RuleChain<HelseOpplysningerArbeidsuforhet>(
        name = "Validation rule chain",
        description = "Rules for the payload that can be validated before doing any network calls.",
        rules = listOf(
                Rule(
                        name = "Patient is over 70 years old",
                        ruleId = 1102,
                        status = Status.INVALID,
                        description = "This is a rule that hits whenever the patients born date + 70 years are lower then the current date"
                ) {
                    extractBornDate(it.pasient.fodselsnummer.id).plusYears(70) < LocalDate.now()
                },
                Rule(
                        name = "Patient is younger then 13 years old",
                        ruleId = 1101,
                        status = Status.INVALID,
                        description = "This is a rule that hits whenever the patients born date + 13 years are higher then the current date"
                ) {
                    extractBornDate(it.pasient.fodselsnummer.id).plusYears(13) > LocalDate.now()
                },
                Rule(
                        name = "Invalid FNR",
                        ruleId = 1006,
                        status = Status.INVALID,
                        description = "Checks if the supplied FNR/DNR for the patient it valid"
                ) {
                    validatePersonAndDNumber(it.pasient.fodselsnummer.id)
                },
                Rule(
                        name = "Invalid code-system",
                        ruleId = 1137,
                        status = Status.INVALID,
                        description = "Validates if the code system is one of the accepted ones"
                ) {
                    it.medisinskVurdering.hovedDiagnose.diagnosekode.s !in Diagnosekode.values()
                }
        ))

val fellesformatValidationChain = RuleChain<XMLEIFellesformat>(
        name = "Fellesformat validation rule chain",
        description = "Rules for the fellesformat that can be validated before doing any network calls.",
        rules = listOf(
                Rule(
                        name = "Signature date is after its been received",
                        ruleId = 1110,
                        status = Status.INVALID,
                        description = "Validates if the date in the signature field is too new compared to the one set by emottak when receiving the message"
                ) {
                    val mottakenhetBlokk: XMLMottakenhetBlokk = it.get()
                    val msgHead: XMLMsgHead = it.get()
                    mottakenhetBlokk.mottattDatotid.toGregorianCalendar().toZonedDateTime().toLocalDateTime() < msgHead.msgInfo.genDate
                }
        )
)

val periodLogicRuleChain = RuleChain<HelseOpplysningerArbeidsuforhet>(
        name = "Period logic rule chain",
        description = "Does validation based on time ranges associated with the sick leave",
        rules = listOf(
                Rule(
                        name = "No period has been added",
                        ruleId = 1200,
                        status = Status.INVALID,
                        description = "Checks if there is a defined sick leave period"
                ) {
                    it.aktivitet.periode == null || it.aktivitet.periode.isEmpty()
                },
                Rule(
                        name = "From date is newer then to date in activity period",
                        ruleId = 1201,
                        status = Status.INVALID,
                        description = "The from date is newer then the to date of an activity"
                ) {
                    it.aktivitet.periode.any {
                        it.periodeFOMDato.toGregorianCalendar() > it.periodeTOMDato.toGregorianCalendar()
                    }
                },
                Rule(
                        name = "Overlapping periods",
                        ruleId = 1202,
                        status = Status.INVALID,
                        description = "Check if any of the activity periods are overlapping"
                ) {
                    it.aktivitet.periode.any { periodA ->
                        it.aktivitet.periode
                                .filter { periodB ->
                                    periodB != periodA
                                }
                                .any { periodB ->
                                    periodA.periodeFOMDato in periodB.periodeFOMDato..periodB.periodeTOMDato
                                            || periodA.periodeTOMDato in periodB.periodeFOMDato..periodB.periodeTOMDato
                                }
                    }
                },
                Rule(
                        name = "Gaps in activity periods",
                        ruleId = 1203,
                        status = Status.INVALID,
                        description = "Checks if there is any gaps in the activity periods"
                ) {
                    val ranges = it.aktivitet.periode.sortedBy { it.periodeFOMDato.toGregorianCalendar() }
                            .map { it.periodeFOMDato.toGregorianCalendar().toZonedDateTime() to it.periodeTOMDato.toGregorianCalendar().toZonedDateTime() }

                    for (i in 1..(ranges.size-1)) {
                        if (workdaysBetween(ranges[i-1].first, ranges[i].second) > 0) {
                            return@Rule true
                        }
                    }
                    false
                },
                Rule(
                        name = "Backdated more then one year without any description",
                        ruleId =  1206,
                        status = Status.INVALID,
                        description = "Checks if the period is backdated for more then one year without any activity"
                ) {
                    it.meldingTilNav == null && it.aktivitet.periode.any {
                        it.periodeFOMDato.toGregorianCalendar().toZonedDateTime().isBefore(ZonedDateTime.now().minusYears(1))
                    }
                },
                Rule(
                        name = "Pending sick leave combined with other types",
                        ruleId = 1240,
                        status = Status.INVALID,
                        description = "A pending sick leave can't be combined with other periods"
                ) {
                    it.aktivitet.periode.size > 1 && it.aktivitet.periode.any { it.avventendeSykmelding != null }
                },
                // TODO: Can we just update the schema for this?
                Rule(
                        name = "Missing additional information to employer in pending period",
                        ruleId = 1241,
                        status = Status.INVALID,
                        description = "The field to the workplace for tilretteleging should be filled for pending sick leaves"
                ) {
                    it.aktivitet.periode.any { it.avventendeSykmelding != null && it.avventendeSykmelding.innspillTilArbeidsgiver == null }
                }
        )
)

fun workdaysBetween(a: ZonedDateTime, b: ZonedDateTime): Int = (1..(ChronoUnit.DAYS.between(a, b) - 1))
        .map { a.plusDays(it) }
        .filter { it.dayOfWeek !in arrayOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
        .count()

operator fun XMLGregorianCalendar.rangeTo(tom: XMLGregorianCalendar): Pair<XMLGregorianCalendar, XMLGregorianCalendar> = this to tom
operator fun Pair<XMLGregorianCalendar, XMLGregorianCalendar>.contains(v: XMLGregorianCalendar) =
        v.toGregorianCalendar() > first.toGregorianCalendar() || v.toGregorianCalendar() < second.toGregorianCalendar()
