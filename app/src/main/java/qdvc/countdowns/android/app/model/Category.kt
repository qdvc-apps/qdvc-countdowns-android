package qdvc.countdowns.android.app.model

/**
 * The three categories the CSV's `category` column is expected to hold, plus a
 * fallback for anything else so an unfamiliar value never loses a whole row.
 */
enum class Category(val id: String) {
    EVENT("event"),
    DEADLINE_INTERNAL("deadline_internal"),
    DEADLINE_EXTERNAL("deadline_external"),
    OTHER("other");

    companion object {
        fun fromId(id: String?): Category = entries.firstOrNull { it.id == id } ?: OTHER

        /**
         * Matching is deliberately forgiving: case, surrounding whitespace, and
         * the exact punctuation of "Deadline (internal)" all vary in real files.
         */
        fun fromCsv(raw: String?): Category {
            val n = raw?.lowercase()?.replace(Regex("[^a-z]+"), " ")?.trim() ?: return OTHER
            return when {
                n.isEmpty() -> OTHER
                n.contains("internal") -> DEADLINE_INTERNAL
                n.contains("external") -> DEADLINE_EXTERNAL
                n.contains("event") -> EVENT
                n.contains("deadline") -> DEADLINE_INTERNAL
                else -> OTHER
            }
        }
    }
}
