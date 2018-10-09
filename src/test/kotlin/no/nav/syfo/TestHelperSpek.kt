package no.nav.syfo

import no.nav.syfo.rules.workdaysBetween
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDateTime
import java.time.ZoneId

object TestHelperSpek : Spek({
    fun date(year: Int, month: Int, day: Int) =
            LocalDateTime.of(year, month, day, 12, 0).atZone(ZoneId.systemDefault())
    describe("Test helpers") {
        it("A duration of 5 days with one weekend day should return 4 weekdays") {
            workdaysBetween(date(2018, 10, 8), date(2018, 10, 14)) shouldEqual 4
        }
    }
})
