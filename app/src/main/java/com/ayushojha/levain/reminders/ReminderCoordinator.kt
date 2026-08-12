package com.ayushojha.levain.reminders

import com.ayushojha.levain.data.LevainDao
import com.ayushojha.levain.domain.DueCalculator
import java.time.Duration
import java.time.Instant

/** Schedules a single exact wake-up. Implemented by an AlarmManager adapter. */
interface DueScheduler {
    fun scheduleExact(at: Instant)
    fun cancel()
}

/** Presents the coalesced "starters due" notification. Implemented by a NotificationManager adapter. */
interface NotificationPresenter {
    fun showDueNotification(starterNames: List<String>)
}

/**
 * Owns reminder behaviour: one notification per due event, fired once, never
 * re-nagging; starters coming due together are coalesced into one notification.
 * All decisions live here (testable); the adapters are dumb.
 */
class ReminderCoordinator(
    private val dao: LevainDao,
    private val scheduler: DueScheduler,
    private val presenter: NotificationPresenter,
) {

    /** Starters whose dueAt falls within this window of the alarm firing share its notification. */
    private val coalesceWindow: Duration = Duration.ofMinutes(15)

    /**
     * Recompute and (re)schedule the next alarm. Call after any mutation that
     * changes dueness: feedings, starter creation, state or interval changes.
     */
    suspend fun reschedule(now: Instant) {
        val nextDueAt = dao.getStarters()
            .mapNotNull { starter ->
                val dueAt = DueCalculator.dueAt(starter, dao.getLastFeeding(starter.id))
                // Already-notified due events don't get a second alarm.
                dueAt?.takeIf { starter.lastNotifiedDueAtEpochMs != it.toEpochMilli() }
            }
            .minOrNull() // past-due events still need a firing; clamped to now below

        if (nextDueAt == null) {
            scheduler.cancel()
        } else {
            scheduler.scheduleExact(maxOf(nextDueAt, now))
        }
    }

    /**
     * Alarm fired: notify for every starter due now (or coming due within the
     * coalescing window), once per due event, in a single notification.
     */
    suspend fun onAlarmFired(now: Instant) {
        val horizon = now.plus(coalesceWindow)
        val due = dao.getStarters().mapNotNull { starter ->
            val dueAt = DueCalculator.dueAt(starter, dao.getLastFeeding(starter.id)) ?: return@mapNotNull null
            val alreadyNotified = starter.lastNotifiedDueAtEpochMs == dueAt.toEpochMilli()
            if (!dueAt.isAfter(horizon) && !alreadyNotified) starter to dueAt else null
        }

        if (due.isNotEmpty()) {
            presenter.showDueNotification(due.map { it.first.name })
            due.forEach { (starter, dueAt) ->
                dao.updateStarter(starter.copy(lastNotifiedDueAtEpochMs = dueAt.toEpochMilli()))
            }
        }

        reschedule(now)
    }
}
