package qdvc.countdowns.android.app.model

/**
 * A colour theme as read from a JSON file in assets/themes. Adding a theme is a
 * data change: drop in a file and it appears in the relevant Settings list.
 */
data class ThemeSpec(
    val id: String,
    val name: String,
    val dark: Boolean,
    val colors: Map<String, Long>
) {
    fun color(role: String, fallback: Long): Long = colors[role] ?: fallback
}
