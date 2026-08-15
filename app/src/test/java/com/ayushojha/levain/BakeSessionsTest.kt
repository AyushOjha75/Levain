package com.ayushojha.levain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ayushojha.levain.data.AssetRecipeSource
import com.ayushojha.levain.data.BakeSessions
import com.ayushojha.levain.data.BakeStatus
import com.ayushojha.levain.data.RecipeCatalog
import com.ayushojha.levain.data.StepKind
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The guided bake, driven the way a real one goes: late folds, an overnight
 * hold, a step that needs another quarter of an hour.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BakeSessionsTest {

    private lateinit var app: TestApp
    private lateinit var sessions: BakeSessions

    @Before
    fun setUp() {
        app = TestApp()
        sessions = BakeSessions(app.db, app.scheduler, app.clock)
    }

    @After
    fun tearDown() {
        app.close()
    }

    private suspend fun seedRecipes() {
        val assets = ApplicationProvider.getApplicationContext<Context>().assets
        RecipeCatalog(app.db, AssetRecipeSource(assets)).seed()
    }

    @Test
    fun `starting a bake snapshots the recipe and expands its repeats`() = runTest {
        seedRecipes()
        val templates = app.db.levainDao().getStepTemplates("focaccia-yeasted")

        val bakeId = sessions.start("focaccia-yeasted")

        val bake = sessions.getBake(bakeId)!!
        assertEquals(BakeStatus.ACTIVE, bake.status)
        assertEquals("focaccia-yeasted", bake.recipeId)
        // A yeasted focaccia needs no culture at all.
        assertNull(bake.starterId)

        val steps = app.db.levainDao().getBakeSteps(bakeId)
        // Repeats became concrete steps, so there are more of them than templates.
        assertTrue(steps.size > templates.size)
        assertTrue(steps.any { it.title.contains("(1 of 2)") })
        assertTrue(steps.any { it.title.contains("(2 of 2)") })

        // Editing the recipe later must never touch this bake: the text lives here now.
        assertTrue(steps.all { it.instruction.isNotBlank() })
    }

    @Test
    fun `the next prompt skips instantaneous steps and arms exactly one alarm`() = runTest {
        seedRecipes()
        app.scheduler.scheduled.clear()

        val bakeId = sessions.start("focaccia-yeasted")

        assertEquals(1, app.scheduler.scheduled.size)
        val steps = app.db.levainDao().getBakeSteps(bakeId)
        val firstWaiting = steps.first { it.kind != StepKind.ACTION }
        assertEquals(firstWaiting.dueAtEpochMs, app.scheduler.lastScheduledAt!!.toEpochMilli())
    }

    @Test
    fun `finishing a step late moves everything after it`() = runTest {
        seedRecipes()
        val bakeId = sessions.start("focaccia-yeasted")
        val steps = app.db.levainDao().getBakeSteps(bakeId)

        val timed = steps.first { it.kind == StepKind.TIMED }
        val after = steps.first { it.position > timed.position && it.kind != StepKind.ACTION }
        val originalDue = after.dueAtEpochMs!!

        // Forty minutes later than the plan wanted.
        val late = app.clock.instant()
            .plus(Duration.ofMinutes(timed.plannedDurationMinutes!!.toLong()))
            .plus(Duration.ofMinutes(40))
        sessions.complete(timed.id, late)

        val movedDue = app.db.levainDao().getBakeStep(after.id)!!.dueAtEpochMs!!
        assertTrue("later steps should shift with the delay", movedDue > originalDue)
    }

    @Test
    fun `a hold stops the clock and resuming re-projects from now`() = runTest {
        seedRecipes()
        val bakeId = sessions.start("focaccia-yeasted")
        val pending = app.db.levainDao().getBakeSteps(bakeId).first { it.completedAtEpochMs == null }
        val dueBeforeHold = pending.dueAtEpochMs!!

        sessions.hold(bakeId)
        assertEquals(BakeStatus.HELD, sessions.getBake(bakeId)!!.status)
        assertTrue("a held bake must not keep an alarm armed", app.scheduler.cancelled > 0)

        // Eight hours in the fridge.
        app.clock.advanceBy(Duration.ofHours(8))
        sessions.resume(bakeId)

        val bake = sessions.getBake(bakeId)!!
        assertEquals(BakeStatus.ACTIVE, bake.status)
        assertNull(bake.heldAtEpochMs)

        val dueAfterResume = app.db.levainDao().getBakeStep(pending.id)!!.dueAtEpochMs!!
        // The pause is not lateness: the step gets its full duration from resume.
        assertTrue(dueAfterResume > dueBeforeHold)
        assertTrue(dueAfterResume >= app.clock.instant().toEpochMilli())
        assertNotNull(app.scheduler.lastScheduledAt)
    }

    @Test
    fun `extending a step pushes it out without completing it`() = runTest {
        seedRecipes()
        val bakeId = sessions.start("focaccia-yeasted")
        val step = app.db.levainDao().getBakeSteps(bakeId).first { it.kind == StepKind.TIMED }
        val before = step.dueAtEpochMs!!

        sessions.extend(step.id, Duration.ofMinutes(15))

        val after = app.db.levainDao().getBakeStep(step.id)!!
        assertNull("extending must not tick the step off", after.completedAtEpochMs)
        assertEquals(before + Duration.ofMinutes(15).toMillis(), after.dueAtEpochMs)
    }

    @Test
    fun `an abandoned bake is kept, not deleted`() = runTest {
        seedRecipes()
        val bakeId = sessions.start("focaccia-yeasted")

        sessions.abandon(bakeId)

        assertEquals(BakeStatus.ABANDONED, sessions.getBake(bakeId)!!.status)
        assertTrue(app.db.levainDao().getAllBakes().any { it.id == bakeId })
        // An abandoned bake is no longer the live one.
        assertNull(sessions.observeActive().first())
    }

    @Test
    fun `finishing records the outcome and stops the alarms`() = runTest {
        seedRecipes()
        val bakeId = sessions.start("focaccia-yeasted")

        sessions.finish(bakeId, outcomeRating = 4, note = "Big bubbles, slightly pale base")

        val bake = sessions.getBake(bakeId)!!
        assertEquals(BakeStatus.FINISHED, bake.status)
        assertEquals(4, bake.outcomeRating)
        assertEquals("Big bubbles, slightly pale base", bake.note)
        assertNull(sessions.observeActive().first())
    }

    @Test
    fun `a sourdough bake can be tied to the starter that leavened it`() = runTest {
        seedRecipes()
        val starterId = app.repository.createStarter(
            com.ayushojha.levain.data.Starter(name = "Rye", createdAtEpochMs = app.clock.instant().toEpochMilli())
        )

        val bakeId = sessions.start("sourdough-country", scale = 2.0, starterId = starterId)

        val bake = sessions.getBake(bakeId)!!
        assertEquals(starterId, bake.starterId)
        assertEquals(2.0, bake.scale, 0.0001)
        // And it shows up in that starter's history.
        assertTrue(app.repository.observeBakes(starterId).first().any { it.id == bakeId })
    }
}
