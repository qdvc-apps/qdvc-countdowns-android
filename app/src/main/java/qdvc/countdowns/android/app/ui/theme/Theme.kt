package qdvc.countdowns.android.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import qdvc.countdowns.android.app.model.Category
import qdvc.countdowns.android.app.model.ThemeMode
import qdvc.countdowns.android.app.model.ThemeSpec

/** Multiplier applied to the sizes that carry content, set in Settings. */
val LocalTextScale = compositionLocalOf { 1.0f }

@Composable
fun resolveDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.AUTOMATIC -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * A category's colour comes from the active theme rather than a fixed palette,
 * so a new theme restyles the whole app -- list badges, the detail ring, chips --
 * without touching any of that code.
 */
@Composable
fun categoryColor(category: Category): Color = when (category) {
    Category.EVENT -> MaterialTheme.colorScheme.primary
    Category.DEADLINE_INTERNAL -> MaterialTheme.colorScheme.secondary
    Category.DEADLINE_EXTERNAL -> MaterialTheme.colorScheme.error
    Category.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun categoryOnColor(category: Category): Color = when (category) {
    Category.EVENT -> MaterialTheme.colorScheme.onPrimary
    Category.DEADLINE_INTERNAL -> MaterialTheme.colorScheme.onSecondary
    Category.DEADLINE_EXTERNAL -> MaterialTheme.colorScheme.onError
    Category.OTHER -> MaterialTheme.colorScheme.surface
}

@Composable
fun QdvcCountdownsTheme(
    spec: ThemeSpec?,
    darkTheme: Boolean,
    textScale: Float,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(spec, darkTheme) { buildColorScheme(spec, darkTheme) }
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            // The top app bar and the bottom navigation bar both use `surface`, so
            // one colour matches both system bars and the whole frame of the
            // screen reads as a single continuous surface.
            val barColor = colorScheme.surface.toArgb()
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = barColor
            @Suppress("DEPRECATION")
            window.navigationBarColor = barColor
            val controller = WindowInsetsControllerCompat(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalTextScale provides textScale) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

private fun buildColorScheme(spec: ThemeSpec?, darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    if (spec == null) return base

    fun role(name: String, fallback: Color): Color =
        spec.colors[name]?.let { Color(it.toInt()) } ?: fallback

    val background = role("background", base.background)
    val onBackground = role("onBackground", base.onBackground)
    val surfaceVariant = role("surfaceVariant", base.surfaceVariant)
    val outline = role("outline", base.outline)

    return base.copy(
        background = background,
        onBackground = onBackground,
        surface = role("surface", base.surface),
        // Pointing onSurface at the theme's onBackground keeps text legible on
        // both roles without asking every theme file to specify it twice.
        onSurface = onBackground,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = role("onSurfaceVariant", base.onSurfaceVariant),
        surfaceContainer = surfaceVariant,
        surfaceContainerLow = role("surface", base.surfaceContainerLow),
        surfaceContainerHigh = surfaceVariant,
        surfaceContainerHighest = surfaceVariant,
        outline = outline,
        outlineVariant = outline,
        primary = role("primary", base.primary),
        onPrimary = role("onPrimary", base.onPrimary),
        primaryContainer = surfaceVariant,
        onPrimaryContainer = onBackground,
        secondary = role("secondary", base.secondary),
        onSecondary = role("onSecondary", base.onSecondary),
        secondaryContainer = surfaceVariant,
        onSecondaryContainer = onBackground,
        tertiary = role("secondary", base.tertiary),
        onTertiary = role("onSecondary", base.onTertiary),
        error = role("error", base.error),
        onError = if (darkTheme) background else Color.White,
        errorContainer = surfaceVariant,
        onErrorContainer = onBackground
    )
}
