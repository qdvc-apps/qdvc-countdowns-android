package qdvc.countdowns.android.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import qdvc.countdowns.android.app.model.Category
import qdvc.countdowns.android.app.util.CsvParser
import java.time.LocalDate

class CsvParserTest {

    @Test
    fun `reads the three required columns`() {
        val result = CsvParser.parse(
            """
            date,name,category
            2026-09-01,Board meeting,Event
            2026-08-15,Tax return,Deadline (external)
            """.trimIndent()
        )

        assertEquals(2, result.countdowns.size)
        assertEquals(0, result.skippedRows)
        assertTrue(result.missingColumns.isEmpty())
        // Sorted by date, so the August row comes first.
        assertEquals(LocalDate.of(2026, 8, 15), result.countdowns[0].date)
        assertEquals(Category.DEADLINE_EXTERNAL, result.countdowns[0].category)
        assertEquals(Category.EVENT, result.countdowns[1].category)
    }

    @Test
    fun `column order does not matter and extra columns are kept`() {
        val result = CsvParser.parse(
            """
            owner,category,date,name,notes
            Priya,Deadline (internal),2026-10-05,Draft ready,Second pass
            """.trimIndent()
        )

        val row = result.countdowns.single()
        assertEquals("Draft ready", row.name)
        assertEquals(Category.DEADLINE_INTERNAL, row.category)
        assertEquals(listOf("owner" to "Priya", "notes" to "Second pass"), row.extras)
    }

    @Test
    fun `quoted fields may contain commas and quotes`() {
        val result = CsvParser.parse(
            "date,name,category\n2026-01-02,\"Smith, Jones and \"\"co\"\"\",Event"
        )
        assertEquals("Smith, Jones and \"co\"", result.countdowns.single().name)
    }

    @Test
    fun `quoted fields may contain newlines`() {
        val result = CsvParser.parse(
            "date,name,category\n2026-01-02,\"Two\nlines\",Event\n2026-01-03,Next,Event"
        )
        assertEquals(2, result.countdowns.size)
        assertEquals("Two\nlines", result.countdowns[0].name)
    }

    @Test
    fun `rows with an unusable date are skipped and counted`() {
        val result = CsvParser.parse(
            """
            date,name,category
            01/02/2026,Wrong format,Event
            ,Missing date,Event
            2026-13-45,Not a date,Event
            2026-03-03,Fine,Event
            """.trimIndent()
        )
        assertEquals(1, result.countdowns.size)
        assertEquals(3, result.skippedRows)
    }

    @Test
    fun `missing columns are reported rather than crashing`() {
        val result = CsvParser.parse("date\n2026-04-04")
        assertEquals(listOf("name", "category"), result.missingColumns)
        val row = result.countdowns.single()
        // With no name column, the date stands in so the row is still openable.
        assertEquals("2026-04-04", row.name)
        assertEquals(Category.OTHER, row.category)
    }

    @Test
    fun `a missing date column yields no countdowns`() {
        val result = CsvParser.parse("name,category\nSomething,Event")
        assertTrue(result.countdowns.isEmpty())
        assertTrue(result.missingColumns.contains("date"))
    }

    @Test
    fun `handles a byte order mark, CRLF endings and trailing blank lines`() {
        val result = CsvParser.parse(
            "\uFEFFdate,name,category\r\n2026-05-05,Launch,Event\r\n\r\n"
        )
        assertEquals(1, result.countdowns.size)
        assertEquals("Launch", result.countdowns.single().name)
    }

    @Test
    fun `category matching tolerates case and punctuation`() {
        assertEquals(Category.EVENT, Category.fromCsv("  EVENT "))
        assertEquals(Category.DEADLINE_INTERNAL, Category.fromCsv("deadline [internal]"))
        assertEquals(Category.DEADLINE_EXTERNAL, Category.fromCsv("External Deadline"))
        assertEquals(Category.OTHER, Category.fromCsv("something else"))
        assertEquals(Category.OTHER, Category.fromCsv(null))
    }

    @Test
    fun `row numbers point at the line in the file`() {
        val result = CsvParser.parse(
            """
            date,name,category
            2026-01-01,First,Event
            2026-01-02,Second,Event
            """.trimIndent()
        )
        assertEquals(2, result.countdowns[0].rowNumber)
        assertEquals(3, result.countdowns[1].rowNumber)
    }
}
