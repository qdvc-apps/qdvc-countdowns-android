package qdvc.countdowns.android.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import qdvc.countdowns.android.app.data.SettingsRepository

/**
 * WorkManager restores its own jobs after a reboot, but a job's remaining delay
 * is restored as a duration, not as a wall-clock time. After a reboot — and
 * especially after a timezone change — that can leave a "9 AM" digest firing at
 * some other hour, so the schedule is re-anchored to the clock here.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }

        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = SettingsRepository(appContext).current()
                ReminderScheduler.reschedule(appContext, settings)
            } finally {
                pending.finish()
            }
        }
    }
}
