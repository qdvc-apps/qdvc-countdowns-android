package qdvc.countdowns.android.app.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * The app's tactile vocabulary. Naming the sensations lets a shared row take one
 * as a parameter instead of every call site reaching for a constant.
 *
 * `Step`, `PickUp` and `Drop` have no call site in this app: they serve drag and
 * swipe gestures, and there are none here. They are kept so the vocabulary stays
 * enumerable and whoever adds the first gesture does not have to re-derive them.
 */
enum class Sensation { Tap, Step, PickUp, Drop, Confirm, Reject }

/**
 * Everything goes through [View.performHapticFeedback], never `Vibrator` or
 * `VibrationEffect`. Three reasons, each sufficient on its own: it honours the
 * system's own touch-feedback setting, so a user who turned haptics off is not
 * overridden; it needs no `VIBRATE` permission; and it quietly does nothing on a
 * device with no vibrator. `FLAG_IGNORE_GLOBAL_SETTING` is deliberately not passed.
 *
 * The three intensity rungs, recorded here so nobody has to re-derive them to move
 * a sensation up or down:
 *
 * | Constants                      | Sensation                            |
 * | ------------------------------ | ------------------------------------ |
 * | `CONTEXT_CLICK`, `CLOCK_TICK`  | faint tick                           |
 * | `VIRTUAL_KEY`, `KEYBOARD_TAP`  | normal click — a standard button press |
 * | `LONG_PRESS`                   | heavy click                          |
 *
 * Taps take `VIRTUAL_KEY`. `CONTEXT_CLICK` looks like the natural choice for "a
 * light tick on tap" and is wrong: it is the platform's *lightest* predefined
 * effect, barely perceptible on many devices, so the app reads as having broken
 * haptics while every other app on the phone feels correct.
 */
class Haptics(private val view: View) {

    fun tap() = performConstant(HapticFeedbackConstants.VIRTUAL_KEY)

    fun step() = performConstant(HapticFeedbackConstants.CLOCK_TICK)

    fun pickUp() = performConstant(
        api30(HapticFeedbackConstants.GESTURE_START, HapticFeedbackConstants.LONG_PRESS)
    )

    fun drop() = performConstant(
        api30(HapticFeedbackConstants.GESTURE_END, HapticFeedbackConstants.CONTEXT_CLICK)
    )

    fun confirm() = performConstant(
        api30(HapticFeedbackConstants.CONFIRM, HapticFeedbackConstants.LONG_PRESS)
    )

    fun reject() = performConstant(
        api30(HapticFeedbackConstants.REJECT, HapticFeedbackConstants.LONG_PRESS)
    )

    fun perform(sensation: Sensation) = when (sensation) {
        Sensation.Tap -> tap()
        Sensation.Step -> step()
        Sensation.PickUp -> pickUp()
        Sensation.Drop -> drop()
        Sensation.Confirm -> confirm()
        Sensation.Reject -> reject()
    }

    /**
     * `GESTURE_START`, `GESTURE_END`, `CONFIRM` and `REJECT` are all API 30+, so
     * each needs a guard at `minSdk 26`. Check any constant you add against
     * `minSdk`; the compiler will not.
     */
    private fun api30(api30: Int, fallback: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) api30 else fallback

    private fun performConstant(constant: Int) {
        view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
