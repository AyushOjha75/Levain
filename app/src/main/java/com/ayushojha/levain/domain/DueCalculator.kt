package com.ayushojha.levain.domain

import com.ayushojha.levain.data.Feeding
import com.ayushojha.levain.data.LifecycleState
import com.ayushojha.levain.data.Starter
import java.time.Duration
import java.time.Instant

enum class DueStatus { OK, DUE, OVERDUE, NEVER_DUE }

data class Dueness(
    val status: DueStatus,
    /** When the starter comes (or came) due. Null for ARCHIVED starters. */
    val dueAt: Instant?,
)

/**
 * Due/overdue is derived, never stored: last Feeding time (or creation time if
 * never fed) plus the interval for the Starter's current lifecycle state.
 */
object DueCalculator {

    /** Past-due by more than this reads as OVERDUE rather than DUE. */
    val OVERDUE_THRESHOLD: Duration = Duration.ofHours(1)

    fun intervalFor(starter: Starter): Duration? = when (starter.state) {
        LifecycleState.ACTIVE -> Duration.ofHours(starter.activeIntervalHours.toLong())
        LifecycleState.DORMANT -> Duration.ofHours(starter.dormantIntervalHours.toLong())
        LifecycleState.ARCHIVED -> null
    }

    fun dueAt(starter: Starter, lastFeeding: Feeding?): Instant? {
        val interval = intervalFor(starter) ?: return null
        val baseline = lastFeeding?.timestampEpochMs ?: starter.createdAtEpochMs
        return Instant.ofEpochMilli(baseline).plus(interval)
    }

    fun dueness(starter: Starter, lastFeeding: Feeding?, now: Instant): Dueness {
        val dueAt = dueAt(starter, lastFeeding)
            ?: return Dueness(DueStatus.NEVER_DUE, null)
        val status = when {
            now.isBefore(dueAt) -> DueStatus.OK
            now.isBefore(dueAt.plus(OVERDUE_THRESHOLD)) -> DueStatus.DUE
            else -> DueStatus.OVERDUE
        }
        return Dueness(status, dueAt)
    }
}
