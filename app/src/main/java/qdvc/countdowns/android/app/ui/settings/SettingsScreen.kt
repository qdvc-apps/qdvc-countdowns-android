package qdvc.countdowns.android.app.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qdvc.countdowns.android.app.BuildConfig
import qdvc.countdowns.android.app.R
import qdvc.countdowns.android.app.data.CountdownsState
import qdvc.countdowns.android.app.model.AppSettings
import qdvc.countdowns.android.app.model.SettingsPage
import qdvc.countdowns.android.app.model.ThemeMode
import qdvc.countdowns.android.app.model.ThemeSpec
import qdvc.countdowns.android.app.model.TimeOfDay
import qdvc.countdowns.android.app.ui.components.ChoiceRow
import qdvc.countdowns.android.app.ui.components.Explainer
import qdvc.countdowns.android.app.ui.components.ListRow
import qdvc.countdowns.android.app.ui.components.NavigationRow
import qdvc.countdowns.android.app.ui.components.Notice
import qdvc.countdowns.android.app.ui.components.SectionHeader
import qdvc.countdowns.android.app.ui.components.SwitchRow
import qdvc.countdowns.android.app.ui.components.hierarchySlide
import qdvc.countdowns.android.app.ui.theme.LocalTextScale
import qdvc.countdowns.android.app.util.Dates

/** Everything the Settings screens can do, gathered so screens stay stateless. */
class SettingsActions(
    val navigate: (SettingsPage) -> Unit,
    val pickFile: () -> Unit,
    val forgetFile: () -> Unit,
    val setDigestEnabled: (Boolean) -> Unit,
    val addDigestTime: (TimeOfDay) -> Unit,
    val removeDigestTime: (TimeOfDay) -> Unit,
    val resetDigestTimes: () -> Unit,
    val sendTestDigest: () -> Unit,
    val setRemindersEnabled: (Boolean) -> Unit,
    val toggleReminderDay: (Int) -> Unit,
    val resetReminderDays: () -> Unit,
    val setReminderTime: (TimeOfDay) -> Unit,
    val sendTestReminder: () -> Unit,
    val setThemeMode: (ThemeMode) -> Unit,
    val setLightTheme: (String) -> Unit,
    val setDarkTheme: (String) -> Unit,
    val nudgeTextScale: (Float) -> Unit,
    val resetTextScale: () -> Unit
)

@Composable
fun SettingsScreen(
    page: SettingsPage,
    settings: AppSettings,
    countdownsState: CountdownsState,
    lightThemes: List<ThemeSpec>,
    darkThemes: List<ThemeSpec>,
    notificationsBlocked: Boolean,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = page,
        transitionSpec = { hierarchySlide(targetState.depth > initialState.depth) },
        label = "settings",
        modifier = modifier.fillMaxSize()
    ) { target ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when (target) {
                SettingsPage.ROOT -> RootPage(settings, lightThemes, darkThemes, actions)
                SettingsPage.FILE -> FilePage(settings, countdownsState, actions)
                SettingsPage.DIGEST -> DigestPage(settings, notificationsBlocked, actions)
                SettingsPage.REMINDERS -> RemindersPage(settings, notificationsBlocked, actions)
                SettingsPage.APPEARANCE -> AppearancePage(settings, actions)
                SettingsPage.LIGHT_STYLE -> ThemePage(
                    themes = lightThemes,
                    selectedId = settings.lightThemeId,
                    onSelect = actions.setLightTheme
                )
                SettingsPage.DARK_STYLE -> ThemePage(
                    themes = darkThemes,
                    selectedId = settings.darkThemeId,
                    onSelect = actions.setDarkTheme
                )
                SettingsPage.TEXT_SIZE -> TextSizePage(settings, actions)
                SettingsPage.ABOUT -> AboutPage()
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RootPage(
    settings: AppSettings,
    lightThemes: List<ThemeSpec>,
    darkThemes: List<ThemeSpec>,
    actions: SettingsActions
) {
    // The countdowns file comes first: nothing else in the app means anything
    // until one is chosen.
    NavigationRow(
        title = stringResource(R.string.settings_file),
        subtitle = settings.csvName ?: stringResource(R.string.settings_file_none),
        icon = Icons.Filled.Description,
        onClick = { actions.navigate(SettingsPage.FILE) }
    )
    NavigationRow(
        title = stringResource(R.string.settings_daily_digest),
        subtitle = digestSubtitle(settings),
        icon = Icons.Filled.Today,
        onClick = { actions.navigate(SettingsPage.DIGEST) }
    )
    NavigationRow(
        title = stringResource(R.string.settings_specific_reminders),
        subtitle = remindersSubtitle(settings),
        icon = Icons.Filled.NotificationsActive,
        onClick = { actions.navigate(SettingsPage.REMINDERS) }
    )
    NavigationRow(
        title = stringResource(R.string.settings_appearance),
        subtitle = stringResource(
            when (settings.themeMode) {
                ThemeMode.AUTOMATIC -> R.string.appearance_automatic
                ThemeMode.LIGHT -> R.string.appearance_light
                ThemeMode.DARK -> R.string.appearance_dark
            }
        ),
        icon = Icons.Filled.Contrast,
        onClick = { actions.navigate(SettingsPage.APPEARANCE) }
    )
    NavigationRow(
        title = stringResource(R.string.settings_light_style),
        subtitle = lightThemes.firstOrNull { it.id == settings.lightThemeId }?.name
            ?: lightThemes.firstOrNull()?.name.orEmpty(),
        icon = Icons.Filled.LightMode,
        onClick = { actions.navigate(SettingsPage.LIGHT_STYLE) }
    )
    NavigationRow(
        title = stringResource(R.string.settings_dark_style),
        subtitle = darkThemes.firstOrNull { it.id == settings.darkThemeId }?.name
            ?: darkThemes.firstOrNull()?.name.orEmpty(),
        icon = Icons.Filled.DarkMode,
        onClick = { actions.navigate(SettingsPage.DARK_STYLE) }
    )
    NavigationRow(
        title = stringResource(R.string.settings_text_size),
        subtitle = "${(settings.textScale * 100).toInt()}%",
        icon = Icons.Filled.FormatSize,
        onClick = { actions.navigate(SettingsPage.TEXT_SIZE) }
    )
    NavigationRow(
        title = stringResource(R.string.settings_about),
        subtitle = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
        icon = Icons.Filled.Info,
        onClick = { actions.navigate(SettingsPage.ABOUT) }
    )
}

@Composable
private fun digestSubtitle(settings: AppSettings): String =
    if (!settings.digestEnabled || settings.digestTimes.isEmpty()) {
        stringResource(R.string.settings_off)
    } else {
        settings.digestTimes.joinToString(", ") { Dates.timeOfDay(it) }
    }

@Composable
private fun remindersSubtitle(settings: AppSettings): String =
    if (!settings.remindersEnabled || settings.reminderDays.isEmpty()) {
        stringResource(R.string.settings_off)
    } else {
        stringResource(
            R.string.settings_reminders_summary,
            settings.reminderDays.sortedDescending().joinToString(", ")
        )
    }

@Composable
private fun FilePage(
    settings: AppSettings,
    state: CountdownsState,
    actions: SettingsActions
) {
    var confirmForget by remember { mutableStateOf(false) }

    Explainer(stringResource(R.string.file_explainer))

    if (settings.hasFile) {
        ListRow(
            title = settings.csvName.orEmpty(),
            subtitle = when (state) {
                is CountdownsState.Loaded ->
                    stringResource(R.string.file_row_count, state.countdowns.size)
                is CountdownsState.Failed -> state.reason
                else -> null
            },
            icon = Icons.Filled.Description
        )
        ListRow(
            title = stringResource(R.string.file_replace),
            onClick = actions.pickFile
        )
        ListRow(
            title = stringResource(R.string.file_forget),
            onClick = { confirmForget = true }
        )
    } else {
        ListRow(
            title = stringResource(R.string.file_choose),
            icon = Icons.Filled.Add,
            onClick = actions.pickFile
        )
    }

    if (confirmForget) {
        AlertDialog(
            onDismissRequest = { confirmForget = false },
            title = { Text(stringResource(R.string.file_forget_confirm_title)) },
            text = { Text(stringResource(R.string.file_forget_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmForget = false
                    actions.forgetFile()
                }) {
                    Text(
                        text = stringResource(R.string.file_forget_confirm_action),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmForget = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun DigestPage(
    settings: AppSettings,
    notificationsBlocked: Boolean,
    actions: SettingsActions
) {
    var pickingTime by remember { mutableStateOf(false) }

    SwitchRow(
        title = stringResource(R.string.digest_switch),
        checked = settings.digestEnabled,
        onCheckedChange = actions.setDigestEnabled
    )
    Explainer(stringResource(R.string.digest_explainer))

    // Shown whenever the permission is missing, not only when the switch is on, so
    // a test that delivers nothing is always explained.
    if (notificationsBlocked) {
        Notice(stringResource(R.string.permission_needed), MaterialTheme.colorScheme.error)
    }

    SectionHeader(stringResource(R.string.digest_times_header))

    if (settings.digestTimes.isEmpty()) {
        Explainer(stringResource(R.string.digest_no_times))
    }
    settings.digestTimes.forEach { time ->
        ListRow(title = Dates.timeOfDay(time)) {
            IconButton(onClick = { actions.removeDigestTime(time) }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.digest_remove_time),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    ListRow(
        title = stringResource(R.string.digest_add_time),
        icon = Icons.Filled.Add,
        onClick = { pickingTime = true }
    )
    ListRow(
        title = stringResource(R.string.digest_reset_times),
        onClick = actions.resetDigestTimes
    )

    SectionHeader(stringResource(R.string.notif_test_header))
    ListRow(
        title = stringResource(R.string.digest_send_test),
        icon = Icons.Filled.NotificationsNone,
        onClick = actions.sendTestDigest
    )
    Explainer(stringResource(R.string.notif_test_explainer))

    if (pickingTime) {
        TimePickerDialog(
            initial = TimeOfDay(9, 0),
            onDismiss = { pickingTime = false },
            onConfirm = {
                pickingTime = false
                actions.addDigestTime(it)
            }
        )
    }
}

@Composable
private fun RemindersPage(
    settings: AppSettings,
    notificationsBlocked: Boolean,
    actions: SettingsActions
) {
    var pickingTime by remember { mutableStateOf(false) }
    var addingDay by remember { mutableStateOf(false) }

    SwitchRow(
        title = stringResource(R.string.specific_switch),
        checked = settings.remindersEnabled,
        onCheckedChange = actions.setRemindersEnabled
    )
    Explainer(stringResource(R.string.specific_explainer))

    if (notificationsBlocked) {
        Notice(stringResource(R.string.permission_needed), MaterialTheme.colorScheme.error)
    }

    SectionHeader(stringResource(R.string.specific_days_header))

    if (settings.reminderDays.isEmpty()) {
        Explainer(stringResource(R.string.specific_no_days))
    }

    // The recommended set is always listed so it can be turned back on, plus any
    // day the user has added themselves. Descending, because that is the order
    // the reminders will actually arrive in.
    val shown = (AppSettings.DEFAULT_REMINDER_DAYS + settings.reminderDays)
        .distinct()
        .sortedDescending()

    shown.forEach { days ->
        val checked = days in settings.reminderDays
        ListRow(
            title = when (days) {
                0 -> stringResource(R.string.specific_day_on_the_day)
                1 -> stringResource(R.string.specific_day_one)
                else -> stringResource(R.string.specific_day_n, days)
            },
            onClick = { actions.toggleReminderDay(days) }
        ) {
            Checkbox(checked = checked, onCheckedChange = { actions.toggleReminderDay(days) })
        }
    }
    ListRow(
        title = stringResource(R.string.specific_add_day),
        icon = Icons.Filled.Add,
        onClick = { addingDay = true }
    )
    ListRow(
        title = stringResource(R.string.specific_reset_days),
        onClick = actions.resetReminderDays
    )

    SectionHeader(stringResource(R.string.specific_time_header))
    ListRow(
        title = Dates.timeOfDay(settings.reminderTime),
        onClick = { pickingTime = true }
    )

    SectionHeader(stringResource(R.string.notif_test_header))
    ListRow(
        title = stringResource(R.string.specific_send_test),
        icon = Icons.Filled.NotificationsNone,
        onClick = actions.sendTestReminder
    )
    Explainer(stringResource(R.string.notif_test_explainer))

    if (pickingTime) {
        TimePickerDialog(
            initial = settings.reminderTime,
            onDismiss = { pickingTime = false },
            onConfirm = {
                pickingTime = false
                actions.setReminderTime(it)
            }
        )
    }

    if (addingDay) {
        AddDayDialog(
            onDismiss = { addingDay = false },
            onConfirm = {
                addingDay = false
                if (it !in settings.reminderDays) actions.toggleReminderDay(it)
            }
        )
    }
}

@Composable
private fun AppearancePage(settings: AppSettings, actions: SettingsActions) {
    ChoiceRow(
        title = stringResource(R.string.appearance_automatic),
        subtitle = stringResource(R.string.appearance_automatic_subtitle),
        selected = settings.themeMode == ThemeMode.AUTOMATIC,
        onClick = { actions.setThemeMode(ThemeMode.AUTOMATIC) }
    )
    ChoiceRow(
        title = stringResource(R.string.appearance_light),
        selected = settings.themeMode == ThemeMode.LIGHT,
        onClick = { actions.setThemeMode(ThemeMode.LIGHT) }
    )
    ChoiceRow(
        title = stringResource(R.string.appearance_dark),
        selected = settings.themeMode == ThemeMode.DARK,
        onClick = { actions.setThemeMode(ThemeMode.DARK) }
    )
}

@Composable
private fun ThemePage(
    themes: List<ThemeSpec>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    themes.forEach { theme ->
        ChoiceRow(
            title = theme.name,
            selected = theme.id == selectedId,
            onClick = { onSelect(theme.id) }
        )
    }
}

@Composable
private fun TextSizePage(settings: AppSettings, actions: SettingsActions) {
    val scale = LocalTextScale.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.text_size_preview),
            fontSize = (28 * scale).sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    ListRow(
        title = stringResource(R.string.settings_text_size),
        subtitle = "${(settings.textScale * 100).toInt()}%"
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { actions.nudgeTextScale(-AppSettings.TEXT_SCALE_STEP) },
                enabled = settings.textScale > AppSettings.TEXT_SCALE_MIN
            ) {
                Text("\u2212", fontSize = 20.sp)
            }
            TextButton(
                onClick = { actions.nudgeTextScale(AppSettings.TEXT_SCALE_STEP) },
                enabled = settings.textScale < AppSettings.TEXT_SCALE_MAX
            ) {
                Text("+", fontSize = 20.sp)
            }
        }
    }
    ListRow(
        title = stringResource(R.string.reset),
        onClick = actions.resetTextScale
    )
}

@Composable
private fun AboutPage() {
    ListRow(
        title = stringResource(R.string.app_name),
        subtitle = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
        icon = Icons.Filled.Info
    )
    Explainer(stringResource(R.string.about_body))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: TimeOfDay,
    onDismiss: () -> Unit,
    onConfirm: (TimeOfDay) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(TimeOfDay(state.hour, state.minute)) }) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun AddDayDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val value = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.specific_add_day_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter { it.isDigit() }.take(3) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { value?.let(onConfirm) },
                enabled = value != null && value in 0..365
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
