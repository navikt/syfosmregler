package no.nav.syfo

import no.nav.syfo.rules.startedWeeksBetween
import no.nav.syfo.rules.workdaysBetween
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

object TestHelperSpek : Spek({
    fun date(year: Int, month: Int, day: Int) =
        LocalDate.of(year, month, day)
    describe("Test helpers") {
        it("Monday to Sunday should return 4 week days") {
            workdaysBetween(date(2018, 10, 8), date(2018, 10, 14)) shouldBeEqualTo 4
        }
        it("Monday to Tuesday should return 0 weekdays") {
            workdaysBetween(date(2018, 10, 8), date(2018, 10, 9)) shouldBeEqualTo 0
        }
        it("Friday to Monday should return 0 weekdays") {
            workdaysBetween(date(2018, 10, 12), date(2018, 10, 15)) shouldBeEqualTo 0
        }
        it("Thursday to Sunday should return 1 weekday") {
            workdaysBetween(date(2018, 10, 11), date(2018, 10, 14)) shouldBeEqualTo 1
        }
        it("Weeks between Monday and tuesday should return 1") {
            (date(2018, 10, 8)..date(2018, 10, 9)).startedWeeksBetween() shouldBeEqualTo 1
        }
        it("Monday to Sunday should return 1 week") {
            (date(2018, 10, 8)..date(2018, 10, 14)).startedWeeksBetween() shouldBeEqualTo 1
        }
        it("Monday to Monday should return 1 week") {
            (date(2018, 10, 8)..date(2018, 10, 15)).startedWeeksBetween() shouldBeEqualTo 2
        }
    }
})
