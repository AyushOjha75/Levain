package com.ayushojha.levain.domain

import com.ayushojha.levain.data.Feeding
import com.ayushojha.levain.data.LifecycleState
import com.ayushojha.levain.data.Starter
import java.time.Duration
import java.time.Instant

data class Vitals(
    /** Consecutive on-time feedings, most recent run. 0 after a late feeding. */
    val feedingStreak: Int,
    val ageDays: Long,
    /** Non-null on the exact day a milestone is hit — the celebration hook. */
    val milestoneToday: String?,
)

/**
 * Streaks and age, derived from the Timeline — never stored. On-time means the
 * fed timestamp (not log time) is within the interval since the previous
 * feeding, measured against the starter's current-state interval. Feeding
 * history doesn't record past lifecycle states, so a DORMANT starter's streak
 * is judged against its dormant interval — feeding a fridge starter weekly
 * keeps the streak alive rather than breaking it ("pauses, not breaks").
 */
object StreakCalculator {

    private val AGE_MILESTONES = listOf(7L, 30L, 50L, 100L, 365L)

    fun vitals(starter: Starter, feedings: List<Feeding>, now: Instant): Vitals {
        val ageDays = Duration.between(Instant.ofEpochMilli(starter.createdAtEpochMs), now).toDays()
        if (starter.state == LifecycleState.ARCHIVED) {
            return Vitals(feedingStreak = 0, ageDays = ageDays, milestoneToday = null)
        }

        return Vitals(
            feedingStreak = streak(starter, feedings, now),
            ageDays = ageDays,
            milestoneToday = milestone(starter.name, ageDays),
        )
    }

    private fun streak(starter: Starter, feedings: List<Feeding>, now: Instant): Int {
        val interval = DueCalculator.intervalFor(starter) ?: return 0
        val ordered = feedings.sortedBy { it.timestampEpochMs }
        if (ordered.isEmpty()) return 0

        // A currently-overdue starter has no live streak.
        val lastFed = Instant.ofEpochMilli(ordered.last().timestampEpochMs)
        if (Duration.between(lastFed, now) > interval.plus(DueCalculator.OVERDUE_THRESHOLD)) return 0

        var run = 1 // the first feeding of any run is on time by definition
        for (i in 1 until ordered.size) {
            val gap = Duration.ofMillis(ordered[i].timestampEpochMs - ordered[i - 1].timestampEpochMs)
            run = if (gap <= interval.plus(DueCalculator.OVERDUE_THRESHOLD)) run + 1 else 1
        }
        return run
    }

    private fun milestone(name: String, ageDays: Long): String? {
        val hit = AGE_MILESTONES.contains(ageDays) || (ageDays > 365 && ageDays % 100 == 0L)
        return if (hit) "$name is $ageDays days old today! 🎂" else null
    }
}
