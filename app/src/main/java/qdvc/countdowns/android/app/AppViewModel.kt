package qdvc.countdowns.android.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import qdvc.countdowns.android.app.data.CountdownRepository
import qdvc.countdowns.android.app.data.CountdownsState
import qdvc.countdowns.android.app.data.SettingsRepository
import qdvc.countdowns.android.app.data.ThemeRepository
import qdvc.countdowns.android.app.model.AppSettings
import qdvc.countdowns.android.app.model.Countdown
import qdvc.countdowns.android.app.model.NavState
import qdvc.countdowns.android.app.model.SettingsPage
import qdvc.countdowns.android.app.model.Tab
import qdvc.countdowns.android.app.model.ThemeMode
import qdvc.countdowns.android.app.model.TimeOfDay
import qdvc.countdowns.android.app.notify.NotificationContent
import qdvc.countdowns.android.app.notify.Notifications
import qdvc.countdowns.android.app.notify.ReminderScheduler
import java.time.LocalDate

/**
 * Owns every piece of application state and exposes plain callbacks to change it.
 * Screens are stateless projections of what lives here.
 *
 * Reading rule: *what state exists* is answered here; *how it is stored or
 * fetched* is answered by the repository the state came from.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val countdownRepo = CountdownRepository(application)
    val themeRepo = ThemeRepository(application)

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _countdowns = MutableStateFlow<CountdownsState>(CountdownsState.Loading)
    val countdowns: StateFlow<CountdownsState> = _countdowns.asStateFlow()

    private val _nav = MutableStateFlow(NavState())
    val nav: StateFlow<NavState> = _nav.asStateFlow()

    /** Held in state rather than read inline so a day boundary can move the list. */
    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    private var loadedUri: String? = null
    private var scheduledSignature: String? = null

    init {
        viewModelScope.launch {
            settingsRepo.settings.collect { s ->
                _settings.value = s
                if (s.csvUri != loadedUri) {
                    loadedUri = s.csvUri
                    reload()
                }
                val signature = notificationSignature(s)
                if (signature != scheduledSignature) {
                    scheduledSignature = signature
                    ReminderScheduler.reschedule(getApplication(), s)
                }
            }
        }
    }

    private fun notificationSignature(s: AppSettings): String = listOf(
        s.digestEnabled,
        TimeOfDay.encodeList(s.digestTimes),
        s.remindersEnabled,
        s.reminderDays.joinToString(","),
        s.reminderTime.encode()
    ).joinToString("/")

    // --- Countdowns -------------------------------------------------------

    fun reload() {
        val uri = _settings.value.csvUri
        viewModelScope.launch {
            if (uri != null) _countdowns.value = CountdownsState.Loading
            _countdowns.value = countdownRepo.load(uri)
        }
    }

    /** Called when the app comes back to the foreground: the file or the date may have moved on. */
    fun onResumed() {
        _today.value = LocalDate.now()
        reload()
    }

    fun upcoming(): List<Countdown> = loaded()?.filter { it.isUpcoming(_today.value) }.orEmpty()

    fun past(): List<Countdown> =
        loaded()?.filterNot { it.isUpcoming(_today.value) }?.sortedByDescending { it.date }.orEmpty()

    private fun loaded(): List<Countdown>? =
        (_countdowns.value as? CountdownsState.Loaded)?.countdowns

    fun countdownByKey(key: String?): Countdown? =
        if (key == null) null else loaded()?.firstOrNull { it.key == key }

    // --- Navigation -------------------------------------------------------

    fun selectTab(tab: Tab) {
        val current = _nav.value
        _nav.value = if (current.tab == tab) {
            // Re-tapping the current tab returns it to its own root.
            when (tab) {
                Tab.COUNTDOWNS -> current.copy(upcomingSelection = null)
                Tab.PAST -> current.copy(pastSelection = null)
                Tab.SETTINGS -> current.copy(settingsPage = SettingsPage.ROOT)
            }
        } else {
            current.copy(tab = tab)
        }
    }

    fun openCountdown(key: String) {
        _nav.value = when (_nav.value.tab) {
            Tab.PAST -> _nav.value.copy(pastSelection = key)
            else -> _nav.value.copy(upcomingSelection = key)
        }
    }

    fun openSettingsPage(page: SettingsPage) {
        _nav.value = _nav.value.copy(tab = Tab.SETTINGS, settingsPage = page)
    }

    /**
     * The single back handler. Both the toolbar arrow and the Android system back
     * button call this, so the two can never diverge. Returns false only at the
     * home root, where the caller should let the system close the app.
     */
    fun goBack(): Boolean {
        val current = _nav.value
        when {
            current.tab == Tab.SETTINGS && current.settingsPage != SettingsPage.ROOT ->
                _nav.value = current.copy(settingsPage = SettingsPage.ROOT)

            current.tab == Tab.COUNTDOWNS && current.upcomingSelection != null ->
                _nav.value = current.copy(upcomingSelection = null)

            current.tab == Tab.PAST && current.pastSelection != null ->
                _nav.value = current.copy(pastSelection = null)

            current.tab != Tab.COUNTDOWNS ->
                _nav.value = current.copy(tab = Tab.COUNTDOWNS)

            else -> return false
        }
        return true
    }

    // --- Test notifications -----------------------------------------------

    /**
     * Both senders post immediately rather than going through the scheduler, so
     * the notification appears while the user is still looking at the button.
     *
     * They deliberately ignore the master switch: the point of a preview is to see
     * what a notification looks like *before* deciding to turn it on. They also
     * leave the fired-reminder record alone, so sending a test cannot stop a real
     * reminder from arriving later.
     */
    fun sendTestDigest() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            Notifications.ensureChannels(context)
            val text = NotificationContent.digestText(context, _countdowns.value, _today.value)
                ?: return@launch
            Notifications.postDigest(context, text)
        }
    }

    fun sendTestReminder() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            Notifications.ensureChannels(context)
            // The soonest real countdown makes the most representative preview.
            // With none to hand, a stand-in still shows the shape of the thing.
            val next = upcoming().firstOrNull()
            val days = next?.daysFrom(_today.value)?.toInt() ?: NotificationContent.EXAMPLE_DAYS
            val name = next?.name ?: context.getString(R.string.notif_test_example_name)
            Notifications.postReminder(
                context = context,
                id = Notifications.ID_TEST_REMINDER,
                title = name,
                text = NotificationContent.whenLabel(context, days),
                grouped = false
            )
        }
    }

    // --- Settings mutators ------------------------------------------------

    fun setCsvFile(uri: Uri) {
        countdownRepo.takePersistablePermission(uri)
        val name = countdownRepo.displayName(uri)
        viewModelScope.launch { settingsRepo.setCsv(uri.toString(), name) }
    }

    fun forgetCsvFile() {
        val existing = _settings.value.csvUri
        viewModelScope.launch {
            existing?.let { countdownRepo.releasePermission(Uri.parse(it)) }
            settingsRepo.setCsv(null, null)
        }
    }

    fun setDigestEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDigestEnabled(enabled) }
    }

    fun addDigestTime(time: TimeOfDay) {
        val times = (_settings.value.digestTimes + time).distinct().sorted()
        viewModelScope.launch { settingsRepo.setDigestTimes(times) }
    }

    fun removeDigestTime(time: TimeOfDay) {
        val times = _settings.value.digestTimes.filterNot { it == time }
        viewModelScope.launch { settingsRepo.setDigestTimes(times) }
    }

    fun resetDigestTimes() {
        viewModelScope.launch { settingsRepo.setDigestTimes(AppSettings.DEFAULT_DIGEST_TIMES) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setRemindersEnabled(enabled) }
    }

    fun toggleReminderDay(days: Int) {
        val current = _settings.value.reminderDays
        val next = if (days in current) current - days else current + days
        viewModelScope.launch { settingsRepo.setReminderDays(next.sorted()) }
    }

    fun resetReminderDays() {
        viewModelScope.launch { settingsRepo.setReminderDays(AppSettings.DEFAULT_REMINDER_DAYS) }
    }

    fun setReminderTime(time: TimeOfDay) {
        viewModelScope.launch { settingsRepo.setReminderTime(time) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    fun setLightTheme(id: String) {
        viewModelScope.launch { settingsRepo.setLightTheme(id) }
    }

    fun setDarkTheme(id: String) {
        viewModelScope.launch { settingsRepo.setDarkTheme(id) }
    }

    fun nudgeTextScale(delta: Float) {
        val next = _settings.value.textScale + delta
        viewModelScope.launch { settingsRepo.setTextScale(next) }
    }

    fun resetTextScale() {
        viewModelScope.launch { settingsRepo.setTextScale(1.0f) }
    }
}
