package qdvc.countdowns.android.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import qdvc.countdowns.android.app.R
import qdvc.countdowns.android.app.model.Tab

/**
 * [onSelect] reports whether the tap actually changed anything, which is what
 * decides the haptic. Feedback marks a change, not a touch: re-tapping the current
 * slot buzzes when it collapses a detail view or a Settings sub-page back to the
 * slot's root, and stays silent when the slot was already at its root and nothing
 * moved.
 */
@Composable
fun BottomBar(
    current: Tab,
    onSelect: (Tab) -> Boolean
) {
    val haptics = rememberHaptics()
    // containerColor = surface and no tonal elevation, so the bar is the same
    // colour as the top app bar. That is what lets one colour match both system
    // bars (see Theme.kt).
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = current == tab,
                onClick = { if (onSelect(tab)) haptics.tap() },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            Tab.COUNTDOWNS -> Icons.Filled.HourglassTop
                            Tab.PAST -> Icons.Filled.History
                            Tab.SETTINGS -> Icons.Filled.Settings
                        },
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(tab.labelRes())) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

private fun Tab.labelRes(): Int = when (this) {
    Tab.COUNTDOWNS -> R.string.tab_countdowns
    Tab.PAST -> R.string.tab_past
    Tab.SETTINGS -> R.string.tab_settings
}
