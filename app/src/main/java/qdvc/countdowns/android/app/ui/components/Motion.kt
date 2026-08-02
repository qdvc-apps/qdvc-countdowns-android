package qdvc.countdowns.android.app.ui.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private const val DURATION_MS = 280

/**
 * The app's one animation vocabulary: a clean horizontal slide, used for every
 * hierarchical step (list to detail, Settings root to a sub-page).
 *
 * The size transform is not decoration. AnimatedContent's default animates the
 * container's height at the same time as the slide, so when two screens differ in
 * height the content drifts in diagonally from a corner instead of sliding
 * straight across. Snapping the size makes the height change instantly and leaves
 * only the slide moving.
 *
 * Note the shape of this function. The obvious way to attach a SizeTransform is
 * `(enter togetherWith exit).using(sizeTransform)`, but `using` is a member of
 * `AnimatedContentTransitionScope`, not a top-level extension, so it does not
 * resolve outside a `transitionSpec` lambda. `ContentTransform.sizeTransform` is
 * no help either: it is a `var` with an `internal set`. Passing it to the
 * constructor is the only route from outside, and it keeps this a plain function
 * that any caller can share.
 */
fun hierarchySlide(deeper: Boolean): ContentTransform {
    val enter = if (deeper) {
        slideInHorizontally(tween(DURATION_MS)) { width -> width } + fadeIn(tween(DURATION_MS))
    } else {
        slideInHorizontally(tween(DURATION_MS)) { width -> -width / 4 } + fadeIn(tween(DURATION_MS))
    }
    val exit = if (deeper) {
        slideOutHorizontally(tween(DURATION_MS)) { width -> -width / 4 } + fadeOut(tween(DURATION_MS))
    } else {
        slideOutHorizontally(tween(DURATION_MS)) { width -> width } + fadeOut(tween(DURATION_MS))
    }
    return ContentTransform(
        targetContentEnter = enter,
        initialContentExit = exit,
        sizeTransform = SizeTransform(clip = false) { _, _ -> snap() }
    )
}
