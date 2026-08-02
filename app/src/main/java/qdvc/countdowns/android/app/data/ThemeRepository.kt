package qdvc.countdowns.android.app.data

import android.content.Context
import org.json.JSONObject
import qdvc.countdowns.android.app.model.ThemeSpec

/**
 * Loads every theme in assets/themes, skipping any file that won't parse, and
 * caches the result. Uses org.json, which is part of Android — no dependency.
 */
class ThemeRepository(private val context: Context) {

    @Volatile
    private var cache: List<ThemeSpec>? = null

    fun themes(): List<ThemeSpec> {
        cache?.let { return it }
        val loaded = load()
        cache = loaded
        return loaded
    }

    fun lightThemes(): List<ThemeSpec> = themes().filter { !it.dark }

    fun darkThemes(): List<ThemeSpec> = themes().filter { it.dark }

    fun theme(id: String, dark: Boolean): ThemeSpec? {
        val pool = if (dark) darkThemes() else lightThemes()
        return pool.firstOrNull { it.id == id } ?: pool.firstOrNull()
    }

    private fun load(): List<ThemeSpec> {
        val names = try {
            context.assets.list(DIR)?.filter { it.endsWith(".json") }.orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
        return names.mapNotNull { parse(it) }.sortedBy { it.name.lowercase() }
    }

    private fun parse(fileName: String): ThemeSpec? = try {
        val text = context.assets.open("$DIR/$fileName").use { it.reader().readText() }
        val json = JSONObject(text)
        val colorsJson = json.getJSONObject("colors")
        val colors = HashMap<String, Long>()
        colorsJson.keys().forEach { role ->
            parseHex(colorsJson.getString(role))?.let { colors[role] = it }
        }
        ThemeSpec(
            id = json.optString("id", fileName.removeSuffix(".json")),
            name = json.optString("name", fileName.removeSuffix(".json")),
            dark = json.optBoolean("dark", false),
            colors = colors
        )
    } catch (e: Exception) {
        null
    }

    private fun parseHex(raw: String): Long? {
        val hex = raw.trim().removePrefix("#")
        return when (hex.length) {
            6 -> hex.toLongOrNull(16)?.let { 0xFF000000L or it }
            8 -> hex.toLongOrNull(16)
            else -> null
        }
    }

    private companion object {
        const val DIR = "themes"
    }
}
