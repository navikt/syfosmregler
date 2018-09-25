package no.nav.syfo.rules

import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.model.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.Diagnosekode
import no.nav.syfo.OutcomeType
import no.nav.syfo.Rule
import no.nav.syfo.RuleChain
import no.nav.syfo.contains
import no.nav.syfo.get
import no.nav.syfo.validation.extractBornDate
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import no.trygdeetaten.xml.eiff._1.XMLMottakenhetBlokk
import java.time.LocalDate

val validationChain = RuleChain<HelseOpplysningerArbeidsuforhet>(
        name = "Validation rule chain",
        description = "Rules for the payload that can be validated before doing any network calls.",
        rules = listOf(
                Rule(
                        name = "Patient is over 70 years old",
                        outcomeType = OutcomeType.PATIENT_OLDER_THAN_70,
                        description = "This is a rule that hits whenever the patients born date + 70 years are lower then the current date"
                ) {
                    extractBornDate(it.pasient.fodselsnummer.id).plusYears(70) < LocalDate.now()
                },
                Rule(
                        name = "Patient is younger then 13 years old",
                        outcomeType = OutcomeType.PATIENT_YOUNGER_THAN_13,
                        description = "This is a rule that hits whenever the patients born date + 13 years are higher then the current date"
                ) {
                    extractBornDate(it.pasient.fodselsnummer.id).plusYears(13) > LocalDate.now()
                },
                Rule(
                        name = "Invalid code-system",
                        outcomeType = OutcomeType.INVALID_CODE_SYSTEM,
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
                        outcomeType = OutcomeType.SIGNATURE_DATE_TOO_OLD,
                        description = "Validates if the date in the signature field is too new compared to the one set by emottak when receiving the message"
                ) {
                    val mottakenhetBlokk: XMLMottakenhetBlokk = it.get()
                    val msgHead: XMLMsgHead = it.get()
                    mottakenhetBlokk.mottattDatotid.toGregorianCalendar().toZonedDateTime().toLocalDateTime() < msgHead.msgInfo.genDate
                }
        )
)
