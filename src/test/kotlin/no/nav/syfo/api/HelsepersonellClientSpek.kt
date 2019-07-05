package no.nav.syfo.api

import no.nhn.schemas.reg.common.no.Kode
import no.nhn.schemas.reg.hprv2.ArrayOfGodkjenning
import no.nhn.schemas.reg.hprv2.Godkjenning
import no.nhn.schemas.reg.hprv2.Person
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object HelsepersonellClientSpek : Spek({
    describe("HelsepersonellClient") {
        it("Mapper ws-respons til domeneobjekt") {
            val person = Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(Godkjenning().apply {
                        helsepersonellkategori = Kode().apply {
                            autorisasjon = Kode().apply {
                                isAktiv = false
                                oid = 7704
                                verdi = "1"
                            }
                            isAktiv = true
                            verdi = "PL"
                        }
                    })
                }
            }
            val lege = ws2Lege(person)

            lege.godkjenninger.size shouldEqual 1
            lege.godkjenninger[0].helsepersonellkategori?.aktiv shouldEqual true
            lege.godkjenninger[0].helsepersonellkategori?.verdi shouldEqual "PL"

            lege.godkjenninger[0].autorisasjon?.aktiv shouldEqual false
            lege.godkjenninger[0].autorisasjon?.oid shouldEqual 7704
            lege.godkjenninger[0].autorisasjon?.verdi shouldEqual "1"
        }
    }
})
