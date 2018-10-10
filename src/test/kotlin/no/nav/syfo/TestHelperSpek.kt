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
        it("Monday to sunday should return 4 week days") {
            workdaysBetween(date(2018, 10, 8), date(2018, 10, 14)) shouldEqual 4
        }
        it("Monday to tuesday should return 0 weekdays") {
            workdaysBetween(date(2018, 10, 8), date(2018, 10, 9)) shouldEqual 0
        }
        it("Friday to monday should return 0 weekdays") {
            workdaysBetween(date(2018, 10, 12), date(2018, 10, 15)) shouldEqual 0
        }
        it("Thursday to sunday should return 1 weekday") {
            workdaysBetween(date(2018, 10, 11), date(2018, 10, 14)) shouldEqual 1
        }
    }
})
