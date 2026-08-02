package qdvc.countdowns.android.app.ui.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

private const val DURATION_MS = 280

/**
 * The app's one animation vocabulary: a clean horizontal slide, used for every
 * hierarchical step (list to detail, Settings root to a sub-page).
 *
 * `SizeTransform { _, _ -> snap() }` is not optional. AnimatedContent's default
 * size transform animates the container's height at the same time as the slide,
 * so when two screens differ in height the content appears to drift in
 * diagonally from a corner instead of sliding straight across. Snapping the size
 * makes the height change instantly and leaves only the horizontal slide moving.
 */
fun hierarchySlide(deeper: Boolean): ContentTransform {
    val spec = if (deeper) {
        (slideInHorizontally(tween(DURATION_MS)) { it } + fadeIn(tween(DURATION_MS))) togetherWith
            (slideOutHorizontally(tween(DURATION_MS)) { -it / 4 } + fadeOut(tween(DURATION_MS)))
    } else {
        (slideInHorizontally(tween(DURATION_MS)) { -it / 4 } + fadeIn(tween(DURATION_MS))) togetherWith
            (slideOutHorizontally(tween(DURATION_MS)) { it } + fadeOut(tween(DURATION_MS)))
    }
    return spec.using(SizeTransform(clip = false) { _, _ -> snap() })
}
