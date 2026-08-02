package qdvc.countdowns.android.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import qdvc.countdowns.android.app.model.Countdown
import qdvc.countdowns.android.app.model.SettingsPage
import qdvc.countdowns.android.app.model.Tab
import qdvc.countdowns.android.app.notify.Notifications
import qdvc.countdowns.android.app.ui.components.AppTopBar
import qdvc.countdowns.android.app.ui.components.BottomBar
import qdvc.countdowns.android.app.ui.components.hierarchySlide
import qdvc.countdowns.android.app.ui.countdowns.CountdownDetailScreen
import qdvc.countdowns.android.app.ui.countdowns.CountdownListScreen
import qdvc.countdowns.android.app.ui.settings.SettingsActions
import qdvc.countdowns.android.app.ui.settings.SettingsScreen
import qdvc.countdowns.android.app.ui.theme.QdvcCountdownsTheme
import qdvc.countdowns.android.app.ui.theme.resolveDarkTheme

/**
 * The single Activity. Tabs and screens are Composables; this class only hosts
 * them, owns the system-level plumbing (edge-to-edge, the file picker, the
 * notification permission), and collects state from the ViewModel.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifications.ensureChannels(this)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = resolveDarkTheme(settings.themeMode)
            val spec = remember(settings.lightThemeId, settings.darkThemeId, darkTheme) {
                viewModel.themeRepo.theme(
                    id = if (darkTheme) settings.darkThemeId else settings.lightThemeId,
                    dark = darkTheme
                )
            }

            QdvcCountdownsTheme(
                spec = spec,
                darkTheme = darkTheme,
                textScale = settings.textScale
            ) {
                AppRoot(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The file may have been edited elsewhere, and the date may have rolled over.
        viewModel.onResumed()
    }
}

@Composable
private fun AppRoot(viewModel: AppViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val nav by viewModel.nav.collectAsStateWithLifecycle()
    val state by viewModel.countdowns.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()

    var notificationsBlocked by remember { mutableStateOf(false) }
    fun refreshNotificationPermission() {
        notificationsBlocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshNotificationPermission() }

    LaunchedEffect(Unit) {
        refreshNotificationPermission()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            notificationsBlocked &&
            (settings.digestEnabled || settings.remindersEnabled)
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Any CSV, however the provider labels it. Some file managers report a plain
    // text or octet-stream MIME type for .csv, so filtering on text/csv alone
    // would hide the user's file from the picker.
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::setCsvFile) }

    fun pickFile() {
        filePicker.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
    }

    // One lambda for the toolbar arrow and the system back button, so the two can
    // never diverge. Enabled everywhere except the home root, where back closes
    // the app.
    BackHandler(enabled = nav.canGoBack) { viewModel.goBack() }

    val selectedKey = when (nav.tab) {
        Tab.PAST -> nav.pastSelection
        else -> nav.upcomingSelection
    }
    val selected: Countdown? = viewModel.countdownByKey(selectedKey)
    val inDetail = selected != null
    val showBackArrow = inDetail ||
        (nav.tab == Tab.SETTINGS && nav.settingsPage != SettingsPage.ROOT)

    Scaffold(
        // The TopAppBar consumes the status-bar inset itself; taking only the
        // bottom inset here avoids doubling the top padding.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = topBarTitle(nav.tab, nav.settingsPage, selected?.name),
                onBack = if (showBackArrow) ({ viewModel.goBack(); Unit }) else null,
                backContentDescription = stringResource(R.string.back),
                actions = {
                    if (!inDetail && nav.tab != Tab.SETTINGS && settings.hasFile) {
                        IconButton(onClick = viewModel::reload) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.reload)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomBar(current = nav.tab, onSelect = viewModel::selectTab)
        }
    ) { padding ->
        val content = Modifier.padding(padding)
        when (nav.tab) {
            Tab.COUNTDOWNS, Tab.PAST -> {
                val past = nav.tab == Tab.PAST
                AnimatedContent(
                    targetState = selected,
                    transitionSpec = {
                        hierarchySlide(deeper = targetState != null && initialState == null)
                    },
                    label = "countdowns",
                    modifier = content
                ) { target ->
                    if (target != null) {
                        CountdownDetailScreen(countdown = target, today = today)
                    } else {
                        CountdownListScreen(
                            state = state,
                            countdowns = if (past) viewModel.past() else viewModel.upcoming(),
                            today = today,
                            past = past,
                            onOpen = { viewModel.openCountdown(it.key) },
                            onOpenSettings = { viewModel.openSettingsPage(SettingsPage.FILE) }
                        )
                    }
                }
            }

            Tab.SETTINGS -> SettingsScreen(
                page = nav.settingsPage,
                settings = settings,
                countdownsState = state,
                lightThemes = viewModel.themeRepo.lightThemes(),
                darkThemes = viewModel.themeRepo.darkThemes(),
                notificationsBlocked = notificationsBlocked,
                modifier = content,
                actions = SettingsActions(
                    navigate = viewModel::openSettingsPage,
                    pickFile = ::pickFile,
                    forgetFile = viewModel::forgetCsvFile,
                    setDigestEnabled = { enabled ->
                        viewModel.setDigestEnabled(enabled)
                        if (enabled) requestPermissionIfNeeded(
                            notificationsBlocked
                        ) { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    },
                    addDigestTime = viewModel::addDigestTime,
                    removeDigestTime = viewModel::removeDigestTime,
                    resetDigestTimes = viewModel::resetDigestTimes,
                    setRemindersEnabled = { enabled ->
                        viewModel.setRemindersEnabled(enabled)
                        if (enabled) requestPermissionIfNeeded(
                            notificationsBlocked
                        ) { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    },
                    toggleReminderDay = viewModel::toggleReminderDay,
                    resetReminderDays = viewModel::resetReminderDays,
                    setReminderTime = viewModel::setReminderTime,
                    setThemeMode = viewModel::setThemeMode,
                    setLightTheme = viewModel::setLightTheme,
                    setDarkTheme = viewModel::setDarkTheme,
                    nudgeTextScale = viewModel::nudgeTextScale,
                    resetTextScale = viewModel::resetTextScale
                )
            )
        }
    }

    // Keep the "notifications are switched off" notice honest if the user changes
    // the permission in Android settings and comes back.
    LaunchedEffect(nav.settingsPage, state) { refreshNotificationPermission() }
}

private fun requestPermissionIfNeeded(blocked: Boolean, request: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && blocked) request()
}

@Composable
private fun topBarTitle(tab: Tab, page: SettingsPage, detailName: String?): String {
    if (detailName != null && tab != Tab.SETTINGS) return detailName
    return when (tab) {
        Tab.COUNTDOWNS -> stringResource(R.string.tab_countdowns)
        Tab.PAST -> stringResource(R.string.tab_past)
        Tab.SETTINGS -> when (page) {
            SettingsPage.ROOT -> stringResource(R.string.tab_settings)
            SettingsPage.FILE -> stringResource(R.string.settings_file)
            SettingsPage.DIGEST -> stringResource(R.string.settings_daily_digest)
            SettingsPage.REMINDERS -> stringResource(R.string.settings_specific_reminders)
            SettingsPage.APPEARANCE -> stringResource(R.string.settings_appearance)
            SettingsPage.LIGHT_STYLE -> stringResource(R.string.settings_light_style)
            SettingsPage.DARK_STYLE -> stringResource(R.string.settings_dark_style)
            SettingsPage.TEXT_SIZE -> stringResource(R.string.settings_text_size)
            SettingsPage.ABOUT -> stringResource(R.string.settings_about)
        }
    }
}
