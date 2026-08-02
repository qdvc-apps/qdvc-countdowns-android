package qdvc.countdowns.android.app.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import qdvc.countdowns.android.app.model.TimeOfDay

object Dates {

    private val LONG_DATE: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")
    private val MEDIUM_DATE: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE d MMM yyyy")

    fun longDate(date: LocalDate): String = LONG_DATE.format(date)

    fun mediumDate(date: LocalDate): String = MEDIUM_DATE.format(date)

    fun timeOfDay(time: TimeOfDay): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .format(LocalTime.of(time.hour, time.minute))

    /**
     * Milliseconds from now until the next occurrence of [time] in the device's
     * zone. Always strictly positive, so a slot scheduled at its own time of day
     * lands tomorrow rather than firing instantly.
     */
    fun millisUntilNext(time: TimeOfDay, zone: ZoneId = ZoneId.systemDefault()): Long {
        val now = LocalDateTime.now(zone)
        var next = now.toLocalDate().atTime(time.hour, time.minute)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.atZone(zone).toInstant().toEpochMilli() -
            now.atZone(zone).toInstant().toEpochMilli()
    }
}
