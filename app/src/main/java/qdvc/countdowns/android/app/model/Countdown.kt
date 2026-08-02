package qdvc.countdowns.android.app.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * One row of the user's CSV. [extras] holds every column that isn't date, name
 * or category, in file order, so the detail screen can show the whole row.
 */
data class Countdown(
    val date: LocalDate,
    val name: String,
    val category: Category,
    val extras: List<Pair<String, String>> = emptyList(),
    val rowNumber: Int = 0
) {
    /** Negative once the date has passed. */
    fun daysFrom(today: LocalDate): Long = ChronoUnit.DAYS.between(today, date)

    fun isUpcoming(today: LocalDate): Boolean = !date.isBefore(today)

    /**
     * Identity for navigation and for remembering which reminders have fired.
     * Derived from the content because a CSV row has no id of its own; editing a
     * row's date or name therefore makes it a new countdown, which is the
     * behaviour we want for reminders.
     */
    val key: String get() = "$date|$name"
}
