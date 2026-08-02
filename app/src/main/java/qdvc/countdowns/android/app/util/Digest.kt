package qdvc.countdowns.android.app.util

import qdvc.countdowns.android.app.model.Countdown
import java.time.LocalDate

/**
 * What the daily digest should say. Pure logic so the cascade can be unit-tested
 * without a device.
 */
sealed interface DigestSummary {
    data class Week(val count: Int) : DigestSummary
    data class Fortnight(val count: Int) : DigestSummary
    data class Month(val count: Int) : DigestSummary
    /** Nothing within a month: nudge the user to check the file is current. */
    data object Nothing : DigestSummary
}

object Digest {

    const val WEEK = 7L
    const val FORTNIGHT = 14L
    const val MONTH = 30L

    /**
     * Counts countdowns falling between today and the horizon inclusive, then
     * widens the horizon until something is found.
     */
    fun summarise(countdowns: List<Countdown>, today: LocalDate): DigestSummary {
        fun within(days: Long) = countdowns.count { c ->
            val d = c.daysFrom(today)
            d in 0..days
        }

        val week = within(WEEK)
        if (week > 0) return DigestSummary.Week(week)
        val fortnight = within(FORTNIGHT)
        if (fortnight > 0) return DigestSummary.Fortnight(fortnight)
        val month = within(MONTH)
        if (month > 0) return DigestSummary.Month(month)
        return DigestSummary.Nothing
    }

    /** The countdowns that are exactly [days] away, for one of [reminderDays]. */
    fun dueForReminder(
        countdowns: List<Countdown>,
        today: LocalDate,
        reminderDays: List<Int>
    ): List<Pair<Countdown, Int>> {
        val wanted = reminderDays.filter { it >= 0 }.toSet()
        if (wanted.isEmpty()) return emptyList()
        return countdowns.mapNotNull { c ->
            val d = c.daysFrom(today)
            if (d in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() && d.toInt() in wanted) {
                c to d.toInt()
            } else {
                null
            }
        }.sortedBy { it.second }
    }
}
