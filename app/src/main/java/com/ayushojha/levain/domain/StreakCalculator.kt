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
 * fed timestamp (not log time — back-filling preserves it) is within the
 * interval since the previous feeding, judged against the interval each
 * feeding recorded when it was logged ([Feeding.intervalHoursAtFeeding]).
 * Dormant eras are judged by their dormant interval even after the starter
 * returns to the counter — "pauses, not breaks", per the glossary.
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
        val currentInterval = DueCalculator.intervalFor(starter) ?: return 0
        val ordered = feedings.sortedBy { it.timestampEpochMs }
        if (ordered.isEmpty()) return 0

        // A currently-overdue starter has no live streak.
        val lastFed = Instant.ofEpochMilli(ordered.last().timestampEpochMs)
        if (Duration.between(lastFed, now) > currentInterval.plus(DueCalculator.OVERDUE_THRESHOLD)) return 0

        var run = 1 // the first feeding of any run is on time by definition
        for (i in 1 until ordered.size) {
            val gap = Duration.ofMillis(ordered[i].timestampEpochMs - ordered[i - 1].timestampEpochMs)
            run = if (gap <= expectedFor(ordered[i - 1], ordered[i], currentInterval)) run + 1 else 1
        }
        return run
    }

    /**
     * The expectation for a gap. Lifecycle state can change mid-gap (fed →
     * moved to fridge, or taken out → fed), so a gap is on time if it meets
     * EITHER endpoint's recorded interval — transitions pause streaks, never
     * break them. Pre-v3 rows have no stamp and fall back to the current interval.
     */
    internal fun expectedFor(previous: Feeding, current: Feeding, fallback: Duration): Duration {
        val stamps = listOfNotNull(
            previous.intervalHoursAtFeeding?.let { Duration.ofHours(it.toLong()) },
            current.intervalHoursAtFeeding?.let { Duration.ofHours(it.toLong()) },
        )
        val expected = stamps.maxOrNull() ?: fallback
        return expected.plus(DueCalculator.OVERDUE_THRESHOLD)
    }

    private fun milestone(name: String, ageDays: Long): String? {
        val hit = AGE_MILESTONES.contains(ageDays) || (ageDays > 365 && ageDays % 100 == 0L)
        return if (hit) "$name is $ageDays days old today! 🎂" else null
    }
}
