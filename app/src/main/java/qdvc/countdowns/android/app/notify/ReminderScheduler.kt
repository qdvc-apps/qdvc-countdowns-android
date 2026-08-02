package qdvc.countdowns.android.app.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import qdvc.countdowns.android.app.model.AppSettings
import qdvc.countdowns.android.app.model.TimeOfDay
import qdvc.countdowns.android.app.util.Dates
import java.util.concurrent.TimeUnit

/**
 * Turns the notification settings into scheduled work.
 *
 * WorkManager rather than AlarmManager: a digest at "about 9 AM" is exactly the
 * kind of job WorkManager is for, it survives reboots on its own, and it doesn't
 * need the exact-alarm permission. The trade-off is that delivery can drift by
 * minutes, and longer if the device is in Doze — acceptable for a daily nudge,
 * and worth documenting so nobody "fixes" it with an exact alarm later.
 *
 * One periodic job per digest time, plus one for the per-countdown reminders.
 * Every call clears the previous schedule first, so this is safe to invoke
 * whenever settings change and on every launch.
 */
object ReminderScheduler {

    private const val TAG = "qdvc-countdowns-schedule"
    private const val NAME_REMINDERS = "qdvc-reminders"
    private fun digestName(time: TimeOfDay) = "qdvc-digest-${time.encode()}"

    fun reschedule(context: Context, settings: AppSettings) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(TAG)

        if (settings.digestEnabled) {
            settings.digestTimes.forEach { time ->
                enqueue(
                    context = context,
                    uniqueName = digestName(time),
                    kind = ReminderWorker.KIND_DIGEST,
                    time = time
                )
            }
        }

        if (settings.remindersEnabled && settings.reminderDays.isNotEmpty()) {
            enqueue(
                context = context,
                uniqueName = NAME_REMINDERS,
                kind = ReminderWorker.KIND_REMINDERS,
                time = settings.reminderTime
            )
        }
    }

    private fun enqueue(
        context: Context,
        uniqueName: String,
        kind: String,
        time: TimeOfDay
    ) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Dates.millisUntilNext(time), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_KIND to kind))
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
