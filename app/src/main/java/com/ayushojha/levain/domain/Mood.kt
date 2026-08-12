package com.ayushojha.levain.domain

import com.ayushojha.levain.data.HealthObservation
import com.ayushojha.levain.data.LifecycleState
import com.ayushojha.levain.data.RiseRating
import com.ayushojha.levain.data.Starter
import java.time.Duration
import java.time.Instant

/**
 * The Avatar's face. Derived from real health data, never stored:
 * a glance at the jar should tell the truth about the starter.
 */
enum class Mood { BEAMING, CONTENT, SLEEPY, HUNGRY, RESTING, RETIRED }

object MoodCalculator {

    /** An observation older than this no longer colours the mood. */
    private val OBSERVATION_RELEVANCE: Duration = Duration.ofDays(3)

    fun mood(
        starter: Starter,
        dueness: Dueness,
        lastObservation: HealthObservation?,
        now: Instant,
    ): Mood {
        if (starter.state == LifecycleState.ARCHIVED) return Mood.RETIRED
        if (dueness.status == DueStatus.DUE || dueness.status == DueStatus.OVERDUE) return Mood.HUNGRY

        val recent = lastObservation?.takeIf {
            Duration.between(Instant.ofEpochMilli(it.timestampEpochMs), now) <= OBSERVATION_RELEVANCE
        }
        return when {
            recent?.riseRating == RiseRating.SLUGGISH || recent?.riseRating == RiseRating.FLAT -> Mood.SLEEPY
            starter.state == LifecycleState.DORMANT -> Mood.RESTING
            recent?.riseRating == RiseRating.PEAKED -> Mood.BEAMING
            else -> Mood.CONTENT
        }
    }
}
