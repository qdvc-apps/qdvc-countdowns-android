package qdvc.countdowns.android.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import qdvc.countdowns.android.app.data.CountdownRepository
import qdvc.countdowns.android.app.data.CountdownsState
import qdvc.countdowns.android.app.data.SettingsRepository
import qdvc.countdowns.android.app.util.Digest
import java.time.LocalDate

/**
 * Does the work for one scheduled slot: reads the CSV fresh, works out what to
 * say, and says it. Both notification kinds share this worker, distinguished by
 * the [KEY_KIND] input.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val settingsRepo = SettingsRepository(context)
        val settings = settingsRepo.current()
        val kind = inputData.getString(KEY_KIND) ?: return Result.success()

        val wanted = when (kind) {
            KIND_DIGEST -> settings.digestEnabled
            KIND_REMINDERS -> settings.remindersEnabled && settings.reminderDays.isNotEmpty()
            else -> false
        }
        if (!wanted) return Result.success()

        Notifications.ensureChannels(context)

        val state = CountdownRepository(context).load(settings.csvUri)
        val today = LocalDate.now()

        when (kind) {
            KIND_DIGEST -> postDigest(context, state, today)
            KIND_REMINDERS -> {
                // A missing or broken file is reported by the digest, not here:
                // a reminder about a specific countdown has nothing to say if
                // there are no countdowns to read.
                if (state is CountdownsState.Loaded) {
                    postReminders(context, settingsRepo, state, today, settings.reminderDays)
                }
            }
        }
        return Result.success()
    }

    private fun postDigest(context: Context, state: CountdownsState, today: LocalDate) {
        val text = NotificationContent.digestText(context, state, today) ?: return
        Notifications.postDigest(context, text)
    }

    private suspend fun postReminders(
        context: Context,
        settingsRepo: SettingsRepository,
        state: CountdownsState.Loaded,
        today: LocalDate,
        reminderDays: List<Int>
    ) {
        val due = Digest.dueForReminder(state.countdowns, today, reminderDays)
        if (due.isEmpty()) return

        val alreadyFired = settingsRepo.firedReminderKeys()
        val fresh = due.filter { (countdown, days) ->
            "${countdown.key}|$days" !in alreadyFired
        }
        if (fresh.isEmpty()) return

        val lines = ArrayList<String>(fresh.size)
        fresh.forEach { (countdown, days) ->
            val when_ = NotificationContent.whenLabel(context, days)
            lines += "$when_: ${countdown.name}"
            Notifications.postReminder(
                context = context,
                id = Notifications.idFor("${countdown.key}|$days"),
                title = countdown.name,
                text = when_,
                grouped = fresh.size > 1
            )
        }
        if (fresh.size > 1) {
            Notifications.postReminderSummary(context, lines)
        }

        settingsRepo.recordFiredReminders(
            newKeys = fresh.mapTo(HashSet()) { (countdown, days) -> "${countdown.key}|$days" },
            keepIfDateOnOrAfter = today.toString()
        )
    }

    companion object {
        const val KEY_KIND = "kind"
        const val KIND_DIGEST = "digest"
        const val KIND_REMINDERS = "reminders"
    }
}
