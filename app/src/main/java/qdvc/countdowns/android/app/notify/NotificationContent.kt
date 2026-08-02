package qdvc.countdowns.android.app.notify

import android.content.Context
import qdvc.countdowns.android.app.R
import qdvc.countdowns.android.app.data.CountdownsState
import qdvc.countdowns.android.app.util.Digest
import qdvc.countdowns.android.app.util.DigestSummary
import java.time.LocalDate

/**
 * What the notifications say. Extracted so the scheduled worker and the "send a
 * test" buttons in Settings share one implementation — a test that showed
 * different wording from the real thing would be worse than no test at all.
 */
object NotificationContent {

    /** Null only while the file is still being read, when there is nothing to say. */
    fun digestText(context: Context, state: CountdownsState, today: LocalDate): String? =
        when (state) {
            is CountdownsState.NoFile -> context.getString(R.string.notif_digest_no_file)
            is CountdownsState.Failed -> context.getString(R.string.notif_digest_unreadable)
            is CountdownsState.Loading -> null
            is CountdownsState.Loaded -> when (val summary = Digest.summarise(state.countdowns, today)) {
                is DigestSummary.Week -> context.resources.getQuantityString(
                    R.plurals.notif_digest_week, summary.count, summary.count
                )
                is DigestSummary.Fortnight -> context.resources.getQuantityString(
                    R.plurals.notif_digest_fortnight, summary.count, summary.count
                )
                is DigestSummary.Month -> context.resources.getQuantityString(
                    R.plurals.notif_digest_month, summary.count, summary.count
                )
                DigestSummary.Nothing -> context.getString(R.string.notif_digest_nothing_month)
            }
        }

    /** How far off a countdown is, as a reminder states it. */
    fun whenLabel(context: Context, days: Int): String = when (days) {
        0 -> context.getString(R.string.notif_reminder_today)
        1 -> context.getString(R.string.notif_reminder_tomorrow)
        else -> context.getString(R.string.notif_reminder_days, days)
    }

    /** Days remaining for the stand-in used when there is no countdown to show. */
    const val EXAMPLE_DAYS = 3
}
