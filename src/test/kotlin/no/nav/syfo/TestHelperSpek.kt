package no.nav.syfo

import io.kotest.core.spec.style.FunSpec
import java.time.LocalDate
import no.nav.syfo.rules.periodvalidering.startedWeeksBetween
import no.nav.syfo.rules.periodvalidering.workdaysBetween
import org.amshove.kluent.shouldBeEqualTo

object TestHelperSpek :
    FunSpec({
        fun date(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)
        context("Test helpers") {
            test("Monday to Sunday should return 4 week days") {
                workdaysBetween(date(2018, 10, 8), date(2018, 10, 14)) shouldBeEqualTo 4
            }
            test("Monday to Tuesday should return 0 weekdays") {
                workdaysBetween(date(2018, 10, 8), date(2018, 10, 9)) shouldBeEqualTo 0
            }
            test("Friday to Monday should return 0 weekdays") {
                workdaysBetween(date(2018, 10, 12), date(2018, 10, 15)) shouldBeEqualTo 0
            }
            test("Thursday to Sunday should return 1 weekday") {
                workdaysBetween(date(2018, 10, 11), date(2018, 10, 14)) shouldBeEqualTo 1
            }
            test("Weeks between Monday and tuesday should return 1") {
                (date(2018, 10, 8)..date(2018, 10, 9)).startedWeeksBetween() shouldBeEqualTo 1
            }
            test("Monday to Sunday should return 1 week") {
                (date(2018, 10, 8)..date(2018, 10, 14)).startedWeeksBetween() shouldBeEqualTo 1
            }
            test("Monday to Monday should return 1 week") {
                (date(2018, 10, 8)..date(2018, 10, 15)).startedWeeksBetween() shouldBeEqualTo 2
            }
        }
    })
