package com.ayushojha.levain.domain

import com.ayushojha.levain.data.BakeStep
import com.ayushojha.levain.data.RecipeStepTemplate
import com.ayushojha.levain.data.StepKind
import java.time.Duration
import java.time.Instant

/**
 * Turns a Recipe into a run, and keeps the Projection honest as the run drifts.
 *
 * Both halves are pure: expansion and projection are decided here and merely
 * persisted elsewhere, so the behaviour is testable without a database, a
 * clock, or a device.
 */
object BakePlanner {

    /**
     * Snapshot a Recipe's templates onto a Bake, expanding repeats into
     * concrete Steps — "four folds, thirty minutes apart" becomes four Steps,
     * each with its own due time and its own completion time, so the baker can
     * be late for one and on time for the next.
     */
    fun expand(templates: List<RecipeStepTemplate>, bakeId: Long): List<BakeStep> {
        val steps = mutableListOf<BakeStep>()
        templates.sortedBy { it.position }.forEach { template ->
            val repeats = template.repeatCount.coerceAtLeast(1)
            repeat(repeats) { index ->
                steps += BakeStep(
                    bakeId = bakeId,
                    position = steps.size,
                    title = if (repeats > 1) "${template.title} (${index + 1} of $repeats)" else template.title,
                    instruction = template.instruction,
                    kind = template.kind,
                    cue = template.cue,
                    plannedDurationMinutes = if (repeats > 1) {
                        template.repeatEveryMinutes ?: template.durationMinutes
                    } else {
                        template.durationMinutes
                    },
                )
            }
        }
        return steps
    }

    /**
     * Recalculate every incomplete step's due time from **actual completion
     * times**. Finish a fold forty minutes late and everything after it moves
     * by forty minutes, including the projected end — the plan is a projection,
     * not a contract.
     *
     * An ACTION step takes no time, so it comes due the moment the step before
     * it lands.
     */
    fun project(steps: List<BakeStep>, startedAt: Instant): List<BakeStep> {
        var cursor = startedAt
        return steps.sortedBy { it.position }.map { step ->
            val completedAt = step.completedAtEpochMs
            if (completedAt != null) {
                cursor = Instant.ofEpochMilli(completedAt)
                step
            } else {
                cursor = cursor.plus(Duration.ofMinutes(step.plannedDurationMinutes?.toLong() ?: 0L))
                step.copy(dueAtEpochMs = cursor.toEpochMilli())
            }
        }
    }

    /**
     * When the app should next wake the baker: the first step still to be done
     * that has a duration worth waiting out. An ACTION is instantaneous — there
     * is nothing to wake anyone for.
     */
    fun nextPromptAt(steps: List<BakeStep>): Instant? = steps
        .sortedBy { it.position }
        .firstOrNull { it.completedAtEpochMs == null && it.kind != StepKind.ACTION }
        ?.dueAtEpochMs
        ?.let(Instant::ofEpochMilli)

    /** The projected moment the bread is done — the last step's due time. */
    fun projectedEnd(steps: List<BakeStep>): Instant? = steps
        .maxByOrNull { it.position }
        ?.let { it.dueAtEpochMs ?: it.completedAtEpochMs }
        ?.let(Instant::ofEpochMilli)

    /**
     * Resuming from a Hold: the pause is not a step running long, so the
     * remaining work is re-projected from the moment of resume rather than
     * being counted as lateness.
     */
    fun shiftIncompleteTo(steps: List<BakeStep>, resumeAt: Instant): List<BakeStep> {
        var cursor = resumeAt
        return steps.sortedBy { it.position }.map { step ->
            if (step.completedAtEpochMs != null) {
                step
            } else {
                cursor = cursor.plus(Duration.ofMinutes(step.plannedDurationMinutes?.toLong() ?: 0L))
                step.copy(dueAtEpochMs = cursor.toEpochMilli())
            }
        }
    }
}
