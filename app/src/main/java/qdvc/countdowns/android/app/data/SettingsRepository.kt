package qdvc.countdowns.android.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import qdvc.countdowns.android.app.model.AppSettings
import qdvc.countdowns.android.app.model.ThemeMode
import qdvc.countdowns.android.app.model.TimeOfDay

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Every persisted preference lives here. Nothing about the user's countdowns is
 * stored — the CSV is re-read from its own location on each launch and on each
 * notification, so what the app shows is never a stale copy.
 *
 * The one exception is [firedReminderKeys], which records *that* a reminder was
 * delivered so the same one isn't delivered twice.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val CSV_URI = stringPreferencesKey("csv_uri")
        val CSV_NAME = stringPreferencesKey("csv_name")
        val DIGEST_ENABLED = booleanPreferencesKey("digest_enabled")
        val DIGEST_TIMES = stringPreferencesKey("digest_times")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_DAYS = stringPreferencesKey("reminder_days")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LIGHT_THEME = stringPreferencesKey("light_theme")
        val DARK_THEME = stringPreferencesKey("dark_theme")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        val FIRED_KEYS = stringSetPreferencesKey("fired_reminder_keys")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = context.dataStore.data.first().toSettings()

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            csvUri = this[Keys.CSV_URI],
            csvName = this[Keys.CSV_NAME],
            digestEnabled = this[Keys.DIGEST_ENABLED] ?: defaults.digestEnabled,
            digestTimes = this[Keys.DIGEST_TIMES]
                ?.let { TimeOfDay.decodeList(it) }
                ?: defaults.digestTimes,
            remindersEnabled = this[Keys.REMINDERS_ENABLED] ?: defaults.remindersEnabled,
            reminderDays = this[Keys.REMINDER_DAYS]
                ?.let { decodeDays(it) }
                ?: defaults.reminderDays,
            reminderTime = this[Keys.REMINDER_TIME]
                ?.let { TimeOfDay.decode(it) }
                ?: defaults.reminderTime,
            themeMode = ThemeMode.fromId(this[Keys.THEME_MODE]),
            lightThemeId = this[Keys.LIGHT_THEME] ?: defaults.lightThemeId,
            darkThemeId = this[Keys.DARK_THEME] ?: defaults.darkThemeId,
            textScale = this[Keys.TEXT_SCALE] ?: defaults.textScale
        )
    }

    // An empty stored value is meaningful ("no times / no days set"), which is why
    // these encode to an empty string rather than being removed.
    private fun decodeDays(raw: String): List<Int> =
        raw.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it >= 0 }.distinct().sorted()

    suspend fun setCsv(uri: String?, name: String?) = context.dataStore.edit { p ->
        if (uri == null) {
            p.remove(Keys.CSV_URI)
            p.remove(Keys.CSV_NAME)
        } else {
            p[Keys.CSV_URI] = uri
            p[Keys.CSV_NAME] = name ?: uri.substringAfterLast('/')
        }
        // A different file means the old delivery record is meaningless.
        p.remove(Keys.FIRED_KEYS)
    }

    suspend fun setDigestEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.DIGEST_ENABLED] = enabled }

    suspend fun setDigestTimes(times: List<TimeOfDay>) =
        context.dataStore.edit { it[Keys.DIGEST_TIMES] = TimeOfDay.encodeList(times) }

    suspend fun setRemindersEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }

    suspend fun setReminderDays(days: List<Int>) = context.dataStore.edit {
        it[Keys.REMINDER_DAYS] = days.filter { d -> d >= 0 }.distinct().sorted().joinToString(",")
    }

    suspend fun setReminderTime(time: TimeOfDay) =
        context.dataStore.edit { it[Keys.REMINDER_TIME] = time.encode() }

    suspend fun setThemeMode(mode: ThemeMode) =
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.id }

    suspend fun setLightTheme(id: String) =
        context.dataStore.edit { it[Keys.LIGHT_THEME] = id }

    suspend fun setDarkTheme(id: String) =
        context.dataStore.edit { it[Keys.DARK_THEME] = id }

    suspend fun setTextScale(scale: Float) = context.dataStore.edit {
        it[Keys.TEXT_SCALE] =
            scale.coerceIn(AppSettings.TEXT_SCALE_MIN, AppSettings.TEXT_SCALE_MAX)
    }

    suspend fun firedReminderKeys(): Set<String> =
        context.dataStore.data.first()[Keys.FIRED_KEYS] ?: emptySet()

    /**
     * Records deliveries and drops records for dates that have gone by, so the
     * set can't grow without bound.
     */
    suspend fun recordFiredReminders(newKeys: Set<String>, keepIfDateOnOrAfter: String) {
        context.dataStore.edit { p ->
            val merged = (p[Keys.FIRED_KEYS] ?: emptySet()) + newKeys
            p[Keys.FIRED_KEYS] = merged.filterTo(HashSet()) { key ->
                key.substringBefore('|') >= keepIfDateOnOrAfter
            }
        }
    }
}
