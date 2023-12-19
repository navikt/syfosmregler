package no.nav.syfo

import java.time.LocalDate

class TestDates {
    companion object {
        internal fun Int.january(year: Int) = LocalDate.of(year, 1, this)

        internal fun Int.february(year: Int) = LocalDate.of(year, 2, this)

        internal fun Int.march(year: Int) = LocalDate.of(year, 3, this)

        internal fun Int.april(year: Int) = LocalDate.of(year, 4, this)

        internal fun Int.may(year: Int) = LocalDate.of(year, 5, this)

        internal fun Int.june(year: Int) = LocalDate.of(year, 6, this)

        internal fun Int.july(year: Int) = LocalDate.of(year, 7, this)

        internal fun Int.august(year: Int) = LocalDate.of(year, 8, this)

        internal fun Int.september(year: Int) = LocalDate.of(year, 9, this)

        internal fun Int.october(year: Int) = LocalDate.of(year, 10, this)

        internal fun Int.november(year: Int) = LocalDate.of(year, 11, this)

        internal fun Int.december(year: Int) = LocalDate.of(year, 12, this)
    }
}
