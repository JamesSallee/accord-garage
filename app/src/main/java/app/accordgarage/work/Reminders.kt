package app.accordgarage.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.accordgarage.GarageApp
import app.accordgarage.MainActivity
import app.accordgarage.R
import app.accordgarage.domain.DueState
import java.time.LocalDate
import java.util.concurrent.TimeUnit

object Reminders {
    const val CHANNEL = "maintenance"
    private const val WORK = "daily-reminders"

    fun schedule(context: Context) {
        createChannel(context)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Maintenance reminders", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Service due, documents expiring and new recalls"
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun notify(context: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_garage)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }
}

/**
 * Once a day: work that is due soon, overdue or flagged by the Minder; documents
 * expiring within 30 days; and new recall campaigns (checked weekly).
 * Each item notifies at most once a week while it stays the same.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GarageApp
        val c = app.container
        val today = LocalDate.now()
        val prefs = c.prefs

        fun shouldSend(key: String): Boolean {
            val last = prefs.getLong("notified_$key", Long.MIN_VALUE)
            if (last != Long.MIN_VALUE && today.toEpochDay() - last < 7) return false
            prefs.edit().putLong("notified_$key", today.toEpochDay()).apply()
            return true
        }

        val snap = c.snapshotNow()
        val alerting = snap.statuses.filter {
            it.state == DueState.OVERDUE || it.state == DueState.DUE_SOON || it.state == DueState.MINDER
        }
        alerting.forEachIndexed { i, s ->
            if (shouldSend("${s.job.id}_${s.state}")) {
                val detail = when {
                    s.state == DueState.MINDER -> "Maintenance Minder shows it's due."
                    s.milesLeft != null && s.milesLeft < 0 -> "${-s.milesLeft} miles overdue."
                    s.milesLeft != null -> "Due in about ${s.milesLeft} miles."
                    s.dueDate != null -> "Due ${s.dueDate}."
                    else -> "Due."
                }
                Reminders.notify(applicationContext, 1000 + i, "${s.job.name}: ${s.state.label.lowercase()}", detail)
            }
        }

        c.db.documents().allNow().forEach { doc ->
            val expiry = doc.expiryEpochDay ?: return@forEach
            val days = expiry - today.toEpochDay()
            if (days in 0..30 && shouldSend("doc_${doc.id}")) {
                Reminders.notify(applicationContext, 2000 + doc.id.toInt(), "${doc.title} expires soon", "Expires ${LocalDate.ofEpochDay(expiry)}.")
            }
        }

        val lastRecall = c.recalls.lastCheckedEpochDay()
        if (lastRecall == null || today.toEpochDay() - lastRecall >= 7) {
            c.recalls.refresh(today.toEpochDay()).getOrNull()?.forEach { r ->
                Reminders.notify(applicationContext, 3000 + r.campaign.hashCode() % 1000, "New recall: ${r.component}", r.summary)
            }
        }
        return Result.success()
    }
}
