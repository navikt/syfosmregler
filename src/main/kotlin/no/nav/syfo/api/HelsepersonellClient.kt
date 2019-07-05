package no.nav.syfo.api

import com.ctc.wstx.exc.WstxException
import no.nav.syfo.helpers.retry
import no.nav.syfo.ws.createPort
import no.nhn.schemas.reg.hprv2.IHPR2Service
import no.nhn.schemas.reg.hprv2.Person
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor
import org.apache.cxf.message.Message
import org.apache.cxf.phase.Phase
import org.apache.cxf.ws.addressing.WSAddressingFeature
import java.io.IOException
import java.util.GregorianCalendar

class HelsepersonellClient(
    helsepersonellv1EndpointURL: String,
    serviceuserUsername: String,
    serviceuserPassword: String,
    securityTokenServiceURL: String
) {
    val helsePersonellV1 = createPort<IHPR2Service>(helsepersonellv1EndpointURL) {
        proxy {
            // TODO: Contact someone about this hacky workaround
            // talk to HDIR about HPR about they claim to send a ISO-8859-1 but its really UTF-8 payload
            val interceptor = object : AbstractSoapInterceptor(Phase.RECEIVE) {
                override fun handleMessage(message: SoapMessage?) {
                    if (message != null)
                        message[Message.ENCODING] = "utf-8"
                }
            }

            inInterceptors.add(interceptor)
            inFaultInterceptors.add(interceptor)
            features.add(WSAddressingFeature())
        }

        port { withSTS(serviceuserUsername, serviceuserPassword, securityTokenServiceURL) }
    }

    suspend fun hentLege(doctorIdent: String): Lege =
        retry(
            callName = "hpr_hent_person_med_personnummer",
            retryIntervals = arrayOf(500L, 1000L, 3000L, 5000L, 10000L),
            legalExceptions = *arrayOf(IOException::class, WstxException::class)
        ) {
            val wsHelsePerson: Person = helsePersonellV1.hentPersonMedPersonnummer(
                doctorIdent,
                datatypeFactory.newXMLGregorianCalendar(GregorianCalendar())
            )
            ws2Lege(wsHelsePerson)
                .also {
                    log.info("fant lege i register med {} godkjenninger", it.godkjenninger.size)
                    it.godkjenninger.forEach {godkjenning ->
                        log.info("autorisasjon a/v/o: {}/{}/[}", godkjenning.autorisasjon?.aktiv, godkjenning.autorisasjon?.verdi, godkjenning.autorisasjon?.oid  )
                        log.info("helsepersonell a/v/o: {}/{}/[}", godkjenning.helsepersonellkategori?.aktiv, godkjenning.helsepersonellkategori?.verdi, godkjenning.helsepersonellkategori?.oid  )
                    }
                }
        }
}

fun ws2Lege(person: Person): Lege =
    Lege(godkjenninger = person.godkjenninger.godkjenning.map { ws2Godkjenning(it) })

fun ws2Godkjenning(godkjenning: no.nhn.schemas.reg.hprv2.Godkjenning): Godkjenning =
    Godkjenning(
        helsepersonellkategori = ws2Kode(godkjenning.helsepersonellkategori),
        autorisasjon = ws2Kode(godkjenning.autorisasjon)
    )

fun ws2Kode(kode: no.nhn.schemas.reg.common.no.Kode): Kode =
    Kode(
        aktiv = kode.isAktiv,
        oid = kode.oid,
        verdi = kode.verdi
    )

data class Lege(
    val godkjenninger: List<Godkjenning> = emptyList()
)

data class Godkjenning(
    val helsepersonellkategori: Kode? = null,
    val autorisasjon: Kode? = null
)

data class Kode(
    val aktiv: Boolean?,
    val oid: Int? = null,
    val verdi: String
)
