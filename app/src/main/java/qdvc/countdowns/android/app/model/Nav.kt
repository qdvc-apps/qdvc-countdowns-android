package qdvc.countdowns.android.app.model

/** The three bottom-bar destinations. Tab 1 is the app's home root. */
enum class Tab { COUNTDOWNS, PAST, SETTINGS }

/**
 * Settings is hierarchical: a root list of rows that open one sub-page each.
 * [depth] drives the slide animation's direction, exactly as the countdown
 * list -> detail step does.
 */
enum class SettingsPage {
    ROOT,
    FILE,
    DIGEST,
    REMINDERS,
    APPEARANCE,
    LIGHT_STYLE,
    DARK_STYLE,
    TEXT_SIZE,
    ABOUT;

    val depth: Int get() = if (this == ROOT) 0 else 1
}

data class NavState(
    val tab: Tab = Tab.COUNTDOWNS,
    val upcomingSelection: String? = null,
    val pastSelection: String? = null,
    val settingsPage: SettingsPage = SettingsPage.ROOT
) {
    /**
     * True whenever back should be handled inside the app. False only at the home
     * root (Tab 1's list), which is the one place system back closes the app.
     */
    val canGoBack: Boolean
        get() = when (tab) {
            Tab.COUNTDOWNS -> upcomingSelection != null
            Tab.PAST -> true
            Tab.SETTINGS -> true
        }
}
