package qdvc.countdowns.android.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import qdvc.countdowns.android.app.MainActivity
import qdvc.countdowns.android.app.R
import kotlin.math.absoluteValue

object Notifications {

    const val CHANNEL_DIGEST = "daily_digest"
    const val CHANNEL_REMINDERS = "countdown_reminders"

    private const val GROUP_REMINDERS = "qdvc.countdowns.reminders"

    const val ID_DIGEST = 1
    const val ID_REMINDER_SUMMARY = 2

    /** Fixed, so repeated tests replace each other instead of piling up. */
    const val ID_TEST_REMINDER = 3
    private const val ID_REMINDER_BASE = 1000

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DIGEST,
                context.getString(R.string.channel_digest_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_digest_description)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_reminders_description)
            }
        )
    }

    fun idFor(key: String): Int = ID_REMINDER_BASE + (key.hashCode().absoluteValue % 100_000)

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun postDigest(context: Context, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_digest_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
        post(context, ID_DIGEST, notification)
    }

    fun postReminder(context: Context, id: Int, title: String, text: String, grouped: Boolean) {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .apply { if (grouped) setGroup(GROUP_REMINDERS) }
            .build()
        post(context, id, notification)
    }

    fun postReminderSummary(context: Context, lines: List<String>) {
        val style = NotificationCompat.InboxStyle()
        lines.take(6).forEach { style.addLine(it) }
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_digest_title))
            .setStyle(style)
            .setGroup(GROUP_REMINDERS)
            .setGroupSummary(true)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
        post(context, ID_REMINDER_SUMMARY, notification)
    }

    /**
     * Posting is wrapped because the user can revoke the notification permission
     * at any time; a revoked permission should be a silent no-op, not a crash in
     * a background worker.
     */
    private fun post(context: Context, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Permission not granted. Nothing to do.
        }
    }
}
