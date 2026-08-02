package qdvc.countdowns.android.app.model

enum class ThemeMode(val id: String) {
    AUTOMATIC("automatic"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: AUTOMATIC
    }
}

/** A wall-clock time of day, stored as "HH:mm". */
data class TimeOfDay(val hour: Int, val minute: Int) : Comparable<TimeOfDay> {

    fun encode(): String = "%02d:%02d".format(hour, minute)

    val minutesOfDay: Int get() = hour * 60 + minute

    override fun compareTo(other: TimeOfDay): Int = minutesOfDay - other.minutesOfDay

    companion object {
        fun decode(raw: String): TimeOfDay? {
            val parts = raw.trim().split(":")
            if (parts.size != 2) return null
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            if (h !in 0..23 || m !in 0..59) return null
            return TimeOfDay(h, m)
        }

        fun decodeList(raw: String): List<TimeOfDay> =
            raw.split(",").mapNotNull { decode(it) }.distinct().sorted()

        fun encodeList(times: List<TimeOfDay>): String =
            times.distinct().sorted().joinToString(",") { it.encode() }
    }
}

data class AppSettings(
    val csvUri: String? = null,
    val csvName: String? = null,

    val digestEnabled: Boolean = true,
    val digestTimes: List<TimeOfDay> = DEFAULT_DIGEST_TIMES,

    val remindersEnabled: Boolean = true,
    /** Days-remaining values at which a countdown gets its own notification. */
    val reminderDays: List<Int> = DEFAULT_REMINDER_DAYS,
    val reminderTime: TimeOfDay = DEFAULT_REMINDER_TIME,

    val themeMode: ThemeMode = ThemeMode.AUTOMATIC,
    val lightThemeId: String = DEFAULT_LIGHT_THEME,
    val darkThemeId: String = DEFAULT_DARK_THEME,
    val textScale: Float = 1.0f
) {
    val hasFile: Boolean get() = csvUri != null

    companion object {
        val DEFAULT_DIGEST_TIMES = listOf(TimeOfDay(9, 0), TimeOfDay(15, 0))
        val DEFAULT_REMINDER_DAYS = listOf(0, 1, 2, 3, 7, 10)
        val DEFAULT_REMINDER_TIME = TimeOfDay(9, 0)
        const val DEFAULT_LIGHT_THEME = "daylight"
        const val DEFAULT_DARK_THEME = "midnight"

        const val TEXT_SCALE_MIN = 0.85f
        const val TEXT_SCALE_MAX = 1.40f
        const val TEXT_SCALE_STEP = 0.05f
    }
}
