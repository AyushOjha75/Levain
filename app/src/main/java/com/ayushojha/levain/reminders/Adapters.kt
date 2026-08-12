package com.ayushojha.levain.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import com.ayushojha.levain.MainActivity
import com.ayushojha.levain.R
import com.ayushojha.levain.appContainer
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AlarmManager adapter for [DueScheduler]. Dumb by design — decisions live in ReminderCoordinator. */
class AlarmDueScheduler(private val context: Context) : DueScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Every (re)schedule means dueness changed — refresh the home-screen
    // widget so it never shows a starter as overdue after it was just fed.
    private fun refreshWidget() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { com.ayushojha.levain.widget.DueWidget().updateAll(context) }
        }
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, DueAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    override fun scheduleExact(at: Instant) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        refreshWidget()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pendingIntent())
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pendingIntent())
        }
    }

    override fun cancel() {
        refreshWidget()
        alarmManager.cancel(pendingIntent())
    }
}

/** NotificationManager adapter for [NotificationPresenter]. */
class DueNotificationPresenter(private val context: Context) : NotificationPresenter {

    private val channelId = "feeding_due"

    override fun showDueNotification(starterNames: List<String>) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Feeding due", NotificationManager.IMPORTANCE_DEFAULT)
        )

        val title = if (starterNames.size == 1) {
            "${starterNames.single()} is due for a feeding"
        } else {
            "${starterNames.size} starters are due for a feeding"
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(starterNames.joinToString(", "))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        // One coalesced notification: reusing the id replaces rather than stacks.
        const val NOTIFICATION_ID = 1
    }
}

class DueAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                context.appContainer.repository.onAlarmFired()
            } finally {
                result.finish()
            }
        }
    }
}

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val result = goAsync()
        val container = context.appContainer
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.repository.onAlarmFired()
            } finally {
                result.finish()
            }
        }
    }
}
