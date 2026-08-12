package com.ayushojha.levain.domain

import java.time.Duration
import java.time.Instant

data class ProgramDay(
    val day: Int,
    val title: String,
    val instruction: String,
)

/**
 * The create-a-starter 7-day program: bundled day-by-day instructions for
 * growing a starter from nothing. The current day derives from the program
 * start instant — nothing about progress is stored.
 */
object StarterProgram {

    val DAYS = listOf(
        ProgramDay(1, "Mix it up", "Combine 50g whole-grain flour with 50g lukewarm water in a clean jar. Stir until no dry flour remains, cover loosely, and leave it somewhere warm (24–27°C)."),
        ProgramDay(2, "Patience day", "You might see nothing yet — that's normal. Give it a stir if a crust forms. No feeding today; the wild yeast is waking up."),
        ProgramDay(3, "First feeding", "Bubbles? A funky smell? Good signs. Discard half, then feed with 50g flour and 50g water. From here on, this is the daily rhythm."),
        ProgramDay(4, "Keep the rhythm", "Discard half and feed again: 50g flour, 50g water. Activity may dip today — the bacteria are settling in. Trust the process."),
        ProgramDay(5, "Getting lively", "Feed as usual. You should see it rising and falling between feeds now. Note the smell turning pleasantly sour."),
        ProgramDay(6, "Almost there", "Feed as usual. If it's doubling within 6–8 hours, tomorrow is graduation day. If not, no stress — some starters need a few extra days."),
        ProgramDay(7, "Graduation", "Feed one more time and watch: if it doubles and smells tangy-sweet, it's alive and yours for life. Give it a name if you haven't. Time to think about a first bake!"),
    )

    const val LENGTH_DAYS = 7

    /** 1-based current day, capped at LENGTH_DAYS; the cap keeps day-7 advice up until graduation. */
    fun currentDay(programStartedAt: Instant, now: Instant): Int {
        val elapsed = Duration.between(programStartedAt, now).toDays().toInt() + 1
        return elapsed.coerceIn(1, LENGTH_DAYS)
    }

    fun isComplete(programStartedAt: Instant, now: Instant): Boolean =
        Duration.between(programStartedAt, now).toDays().toInt() + 1 > LENGTH_DAYS
}
