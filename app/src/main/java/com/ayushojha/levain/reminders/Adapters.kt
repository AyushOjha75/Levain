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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * What an alarm is for. Each purpose needs its **own request code and action**:
 * PendingIntent matching uses `Intent.filterEquals`, which ignores extras, so
 * two purposes sharing a request code and component are the *same*
 * PendingIntent — arming one would silently cancel the other.
 */
enum class AlarmPurpose(val requestCode: Int, val action: String) {
    FEEDING_DUE(1, "com.ayushojha.levain.action.FEEDING_DUE"),
    BAKE_STEP_DUE(2, "com.ayushojha.levain.action.BAKE_STEP_DUE"),
}

/** AlarmManager adapter for [DueScheduler]. Dumb by design — decisions live in ReminderCoordinator. */
class AlarmDueScheduler(
    private val context: Context,
    private val purpose: AlarmPurpose = AlarmPurpose.FEEDING_DUE,
) : DueScheduler {

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
        purpose.requestCode,
        Intent(context, DueAlarmReceiver::class.java).setAction(purpose.action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    override fun scheduleExact(at: Instant) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        refreshWidget()
        when {
            // A live bake step is genuinely time-critical and the baker is
            // awake and waiting, so it earns setAlarmClock: uncapped, exempt
            // from Doze, and immune to the App Standby demotion that would
            // otherwise stretch an overnight hold's alarm to the next hour.
            // The status-bar alarm icon is the honest price.
            purpose == AlarmPurpose.BAKE_STEP_DUE && canExact ->
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(at.toEpochMilli(), showAppIntent()),
                    pendingIntent(),
                )
            // A feeding reminder tolerates a few minutes, and does not deserve
            // to sit in the status bar as an alarm all day.
            canExact ->
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pendingIntent())
            else ->
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pendingIntent())
        }
    }

    private fun showAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        purpose.requestCode,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

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
        val container = context.appContainer
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    AlarmPurpose.BAKE_STEP_DUE.action -> {
                        val bake = container.bakeSessions.observeActive().first() ?: return@launch
                        val step = container.database.levainDao().getBakeSteps(bake.id)
                            .firstOrNull { it.completedAtEpochMs == null } ?: return@launch
                        BakeStepNotifier(context).show(step.title, step.cue ?: step.instruction)
                    }
                    // Anything else is the feeding reminder, including the
                    // actionless intent older scheduled alarms were built with.
                    else -> container.repository.onAlarmFired()
                }
            } finally {
                result.finish()
            }
        }
    }
}

/** Tells the baker a step has come due. Separate channel: a bake is not a chore. */
class BakeStepNotifier(private val context: Context) {

    fun show(title: String, body: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Bake steps", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(
            context,
            AlarmPurpose.BAKE_STEP_DUE.requestCode,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    private companion object {
        const val CHANNEL = "bake_steps"
        const val NOTIFICATION_ID = 2
    }
}

/**
 * On API 31+ the user can revoke exact-alarm permission at any moment, and the
 * system silently cancels every exact alarm the app has already scheduled
 * without telling it. Without this receiver, reminders would simply stop and
 * nothing would ever notice.
 */
class ExactAlarmPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val container = context.appContainer
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.repository.rescheduleReminders()
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
