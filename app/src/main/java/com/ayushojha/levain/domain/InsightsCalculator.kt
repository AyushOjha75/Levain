package com.ayushojha.levain.domain

import com.ayushojha.levain.data.Bake
import com.ayushojha.levain.data.Feeding
import com.ayushojha.levain.data.HealthObservation
import com.ayushojha.levain.data.RiseRating
import java.time.Duration

enum class RiseTrend { IMPROVING, STEADY, DECLINING }

data class Insights(
    /** Average gap between recent feedings, in hours. Null with fewer than 2 feedings. */
    val avgGapHours: Int?,
    /** Share of recent feedings that were on time (against their own era). Null with fewer than 2. */
    val onTimePercent: Int?,
    /** Direction of recent rise ratings vs the ones before. Null with fewer than 4 observations. */
    val riseTrend: RiseTrend?,
    val bakeCount: Int,
    /** Mean outcome rating across all bakes. Null with none. */
    val avgBakeRating: Double?,
)

/** Turns a Starter's history into the detail screen's insight numbers. Pure derivation. */
object InsightsCalculator {

    private const val RECENT_FEEDINGS = 10

    fun insights(feedings: List<Feeding>, observations: List<HealthObservation>, bakes: List<Bake>): Insights {
        val ordered = feedings.sortedBy { it.timestampEpochMs }.takeLast(RECENT_FEEDINGS)

        val gaps = ordered.zipWithNext { a, b -> b.timestampEpochMs - a.timestampEpochMs }
        val avgGapHours = gaps.takeIf { it.isNotEmpty() }
            ?.let { Duration.ofMillis(it.sum() / it.size).toHours().toInt() }

        // Same era rule as streaks: either endpoint's expectation satisfies the
        // gap. Gaps with no stamp at all (pre-v3 data) are excluded, not assumed
        // on time — an inflated rate is worse than a smaller sample.
        val judgeable = ordered.zipWithNext().withIndex().filter { (_, pair) ->
            pair.first.intervalHoursAtFeeding != null || pair.second.intervalHoursAtFeeding != null
        }
        val onTimePercent = judgeable.takeIf { it.isNotEmpty() }?.let { pairs ->
            val onTime = pairs.count { (i, pair) ->
                Duration.ofMillis(gaps[i]) <= StreakCalculator.expectedFor(pair.first, pair.second, Duration.ZERO)
            }
            onTime * 100 / pairs.size
        }

        val riseTrend = riseTrend(observations)

        return Insights(
            avgGapHours = avgGapHours,
            onTimePercent = onTimePercent,
            riseTrend = riseTrend,
            bakeCount = bakes.size,
            avgBakeRating = bakes.takeIf { it.isNotEmpty() }
                ?.let { list -> list.sumOf { it.outcomeRating }.toDouble() / list.size },
        )
    }

    private fun score(rating: RiseRating): Int = when (rating) {
        RiseRating.PEAKED -> 3
        RiseRating.RISING -> 2
        RiseRating.SLUGGISH -> 1
        RiseRating.FLAT -> 0
    }

    private fun riseTrend(observations: List<HealthObservation>): RiseTrend? {
        val ordered = observations.sortedBy { it.timestampEpochMs }.map { score(it.riseRating) }
        if (ordered.size < 4) return null
        val half = ordered.size / 2
        val older = ordered.take(ordered.size - half).average()
        val recent = ordered.takeLast(half).average()
        return when {
            recent - older > 0.4 -> RiseTrend.IMPROVING
            older - recent > 0.4 -> RiseTrend.DECLINING
            else -> RiseTrend.STEADY
        }
    }
}
