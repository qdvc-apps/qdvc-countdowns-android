package qdvc.countdowns.android.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import qdvc.countdowns.android.app.model.Category
import qdvc.countdowns.android.app.model.Countdown
import qdvc.countdowns.android.app.model.TimeOfDay
import qdvc.countdowns.android.app.util.Digest
import qdvc.countdowns.android.app.util.DigestSummary
import java.time.LocalDate

class DigestTest {

    private val today = LocalDate.of(2026, 6, 1)

    private fun at(offsetDays: Long, name: String = "x") = Countdown(
        date = today.plusDays(offsetDays),
        name = name,
        category = Category.EVENT
    )

    @Test
    fun `counts what falls inside the next week`() {
        val summary = Digest.summarise(listOf(at(0), at(7), at(8)), today)
        assertEquals(DigestSummary.Week(2), summary)
    }

    @Test
    fun `widens to a fortnight when the week is empty`() {
        val summary = Digest.summarise(listOf(at(8), at(14), at(15)), today)
        assertEquals(DigestSummary.Fortnight(2), summary)
    }

    @Test
    fun `widens to a month when the fortnight is empty`() {
        val summary = Digest.summarise(listOf(at(20), at(30), at(31)), today)
        assertEquals(DigestSummary.Month(2), summary)
    }

    @Test
    fun `falls through to the nudge when nothing is within a month`() {
        assertEquals(DigestSummary.Nothing, Digest.summarise(listOf(at(45)), today))
        assertEquals(DigestSummary.Nothing, Digest.summarise(emptyList(), today))
    }

    @Test
    fun `past countdowns never count towards the digest`() {
        assertEquals(DigestSummary.Nothing, Digest.summarise(listOf(at(-1), at(-100)), today))
    }

    @Test
    fun `reminders match only the configured days remaining`() {
        val countdowns = listOf(at(0, "today"), at(1, "tomorrow"), at(4, "four"), at(7, "seven"))
        val due = Digest.dueForReminder(countdowns, today, listOf(0, 1, 7))

        assertEquals(listOf("today" to 0, "tomorrow" to 1, "seven" to 7), due.map { (c, d) -> c.name to d })
    }

    @Test
    fun `no configured days means no reminders`() {
        assertTrue(Digest.dueForReminder(listOf(at(1)), today, emptyList()).isEmpty())
    }

    @Test
    fun `reminders ignore countdowns that have passed`() {
        assertTrue(Digest.dueForReminder(listOf(at(-1)), today, listOf(0, 1)).isEmpty())
    }

    @Test
    fun `time of day round trips and rejects nonsense`() {
        assertEquals(TimeOfDay(9, 5), TimeOfDay.decode("09:05"))
        assertEquals("09:05", TimeOfDay(9, 5).encode())
        assertNull(TimeOfDay.decode("24:00"))
        assertNull(TimeOfDay.decode("9"))
        assertNull(TimeOfDay.decode("nine o'clock"))
    }

    @Test
    fun `time lists are de-duplicated and sorted`() {
        assertEquals(
            listOf(TimeOfDay(9, 0), TimeOfDay(15, 0)),
            TimeOfDay.decodeList("15:00,09:00,15:00")
        )
        assertEquals("09:00,15:00", TimeOfDay.encodeList(listOf(TimeOfDay(15, 0), TimeOfDay(9, 0))))
    }
}
