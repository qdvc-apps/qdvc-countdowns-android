package qdvc.countdowns.android.app.util

import qdvc.countdowns.android.app.model.Category
import qdvc.countdowns.android.app.model.Countdown
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class CsvParseResult(
    val countdowns: List<Countdown>,
    /** Rows dropped because the date column was missing or unparseable. */
    val skippedRows: Int,
    /** Of "date", "name", "category": those with no matching header. */
    val missingColumns: List<String>
)

/**
 * Reads the user's CSV. Pure Kotlin with no Android dependencies, so it is
 * cheap to unit-test — which matters, because this is the one place where a
 * file the app doesn't control turns into the app's own data.
 */
object CsvParser {

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val DATE_HEADERS = setOf("date")
    private val NAME_HEADERS = setOf("name", "description", "title")
    private val CATEGORY_HEADERS = setOf("category", "type")

    fun parse(text: String): CsvParseResult {
        val rows = splitRows(text.removePrefix("\uFEFF"))
        if (rows.isEmpty()) {
            return CsvParseResult(emptyList(), 0, listOf("date", "name", "category"))
        }

        val header = rows.first().map { it.trim() }
        val lower = header.map { it.lowercase() }
        val dateIdx = lower.indexOfFirst { it in DATE_HEADERS }
        val nameIdx = lower.indexOfFirst { it in NAME_HEADERS }
        val categoryIdx = lower.indexOfFirst { it in CATEGORY_HEADERS }

        val missing = buildList {
            if (dateIdx < 0) add("date")
            if (nameIdx < 0) add("name")
            if (categoryIdx < 0) add("category")
        }
        if (dateIdx < 0) {
            return CsvParseResult(emptyList(), rows.size - 1, missing)
        }

        val known = setOfNotNull(
            dateIdx.takeIf { it >= 0 },
            nameIdx.takeIf { it >= 0 },
            categoryIdx.takeIf { it >= 0 }
        )

        var skipped = 0
        val out = ArrayList<Countdown>(rows.size)

        rows.drop(1).forEachIndexed { i, row ->
            if (row.all { it.isBlank() }) return@forEachIndexed
            val date = parseDate(row.getOrNull(dateIdx))
            if (date == null) {
                skipped++
                return@forEachIndexed
            }
            val name = row.getOrNull(nameIdx)?.trim().orEmpty()
            val extras = header.indices
                .filter { it !in known }
                .mapNotNull { idx ->
                    val value = row.getOrNull(idx)?.trim().orEmpty()
                    val label = header.getOrNull(idx)?.trim().orEmpty()
                    if (value.isEmpty() || label.isEmpty()) null else label to value
                }
            out += Countdown(
                date = date,
                name = name.ifEmpty { date.toString() },
                category = Category.fromCsv(row.getOrNull(categoryIdx)),
                extras = extras,
                // +2: one for the header row, one because humans count from 1.
                rowNumber = i + 2
            )
        }

        out.sortWith(compareBy({ it.date }, { it.name.lowercase() }))
        return CsvParseResult(out, skipped, missing)
    }

    private fun parseDate(raw: String?): LocalDate? {
        val v = raw?.trim().orEmpty()
        if (v.isEmpty()) return null
        return try {
            LocalDate.parse(v, DATE_FORMAT)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    /**
     * RFC 4180-style splitting: double quotes protect commas and newlines, and a
     * doubled quote inside a quoted field is a literal quote. Handles LF, CRLF
     * and lone CR line endings.
     */
    fun splitRows(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var fields = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        fun endField() {
            fields.add(field.toString())
            field.setLength(0)
        }

        fun endRow() {
            endField()
            rows.add(fields)
            fields = ArrayList()
        }

        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == ',' -> endField()
                !inQuotes && (c == '\n' || c == '\r') -> {
                    endRow()
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || fields.isNotEmpty()) endRow()

        return rows.filter { row -> row.any { it.isNotBlank() } }
    }
}
