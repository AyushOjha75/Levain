package com.ayushojha.levain

import com.ayushojha.levain.domain.StarterProgram
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A baker following a seven-day program thinks in calendar days — "what do I
 * do today" — not in elapsed hours since they happened to mix flour and water.
 */
class StarterProgramTest {

    private val london = ZoneId.of("Europe/London")

    @Test
    fun `the day rolls over at local midnight, not 24 hours after starting`() {
        val startedAt = LocalDateTime.of(2026, 3, 2, 16, 0).atZone(london).toInstant()

        // Same evening, seven hours in: still day one's instruction.
        assertEquals(1, StarterProgram.currentDay(startedAt, startedAt.plus(Duration.ofHours(7)), london))

        // 01:00 the next morning — nine hours in, but a new day for the baker.
        assertEquals(2, StarterProgram.currentDay(startedAt, startedAt.plus(Duration.ofHours(9)), london))

        // The old behaviour would still call this day one until 16:00 tomorrow.
        assertEquals(2, StarterProgram.currentDay(startedAt, startedAt.plus(Duration.ofHours(23)), london))
    }

    @Test
    fun `day counting survives a daylight-saving change`() {
        val newYork = ZoneId.of("America/New_York")
        // Clocks jump forward on 8 March 2026, so 7-9 March is 47 hours, not 48.
        val startedAt = LocalDateTime.of(2026, 3, 7, 10, 0).atZone(newYork).toInstant()
        val twoCalendarDaysLater = LocalDateTime.of(2026, 3, 9, 10, 0).atZone(newYork).toInstant()

        assertEquals(3, StarterProgram.currentDay(startedAt, twoCalendarDaysLater, newYork))
    }

    @Test
    fun `the program completes the day after day seven`() {
        val startedAt = LocalDateTime.of(2026, 3, 2, 16, 0).atZone(london).toInstant()
        val daySeven = LocalDateTime.of(2026, 3, 8, 9, 0).atZone(london).toInstant()
        val dayEight = LocalDateTime.of(2026, 3, 9, 9, 0).atZone(london).toInstant()

        assertEquals(7, StarterProgram.currentDay(startedAt, daySeven, london))
        assertFalse(StarterProgram.isComplete(startedAt, daySeven, london))
        assertTrue(StarterProgram.isComplete(startedAt, dayEight, london))
    }

    @Test
    fun `every program day has content`() {
        assertEquals(StarterProgram.LENGTH_DAYS, StarterProgram.DAYS.size)
        StarterProgram.DAYS.forEachIndexed { index, day ->
            assertEquals(index + 1, day.day)
            assertTrue(day.instruction.isNotBlank())
        }
    }
}
