package no.nav.syfo

import java.time.LocalDate
import no.nav.syfo.rules.startedWeeksBetween
import no.nav.syfo.rules.workdaysBetween
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestHelperSpek : Spek({
    fun date(year: Int, month: Int, day: Int) =
            LocalDate.of(year, month, day)
    describe("Test helpers") {
        it("Monday to Sunday should return 4 week days") {
            workdaysBetween(date(2018, 10, 8), date(2018, 10, 14)) shouldEqual 4
        }
        it("Monday to Tuesday should return 0 weekdays") {
            workdaysBetween(date(2018, 10, 8), date(2018, 10, 9)) shouldEqual 0
        }
        it("Friday to Monday should return 0 weekdays") {
            workdaysBetween(date(2018, 10, 12), date(2018, 10, 15)) shouldEqual 0
        }
        it("Thursday to Sunday should return 1 weekday") {
            workdaysBetween(date(2018, 10, 11), date(2018, 10, 14)) shouldEqual 1
        }
        it("Weeks between Monday and tuesday should return 1") {
            (date(2018, 10, 8)..date(2018, 10, 9)).startedWeeksBetween() shouldEqual 1
        }
        it("Monday to Sunday should return 1 week") {
            (date(2018, 10, 8)..date(2018, 10, 14)).startedWeeksBetween() shouldEqual 1
        }
        it("Monday to Monday should return 1 week") {
            (date(2018, 10, 8)..date(2018, 10, 15)).startedWeeksBetween() shouldEqual 2
        }
    }
})
