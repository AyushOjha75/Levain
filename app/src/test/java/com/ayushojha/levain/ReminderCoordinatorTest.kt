package com.ayushojha.levain

import com.ayushojha.levain.data.Feeding
import com.ayushojha.levain.data.LifecycleState
import com.ayushojha.levain.data.Starter
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReminderCoordinatorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: TestApp

    @Before
    fun setUp() {
        app = TestApp()
    }

    @After
    fun tearDown() {
        app.close()
    }

    private suspend fun createStarter(
        name: String,
        state: LifecycleState = LifecycleState.ACTIVE,
        activeIntervalHours: Int = 24,
    ): Long = app.repository.createStarter(
        Starter(
            name = name,
            state = state,
            activeIntervalHours = activeIntervalHours,
            createdAtEpochMs = app.clock.instant().toEpochMilli(),
        )
    )

    private suspend fun feedNow(starterId: Long) {
        app.repository.logFeeding(
            Feeding(
                starterId = starterId,
                timestampEpochMs = app.clock.instant().toEpochMilli(),
                ratio = "1:5:5",
                flourType = "White",
            )
        )
    }

    @Test
    fun `feeding a starter schedules the next alarm one interval later`() = runTest(mainDispatcherRule.dispatcher) {
        val id = createStarter("Rye", activeIntervalHours = 24)
        feedNow(id)

        val expected = app.clock.instant().plus(Duration.ofHours(24))
        assertEquals(expected, app.scheduler.lastScheduledAt)
    }

    @Test
    fun `alarm at due time notifies that starter once`() = runTest(mainDispatcherRule.dispatcher) {
        val id = createStarter("Rye", activeIntervalHours = 24)
        feedNow(id)

        app.clock.advanceBy(Duration.ofHours(24))
        app.repository.onAlarmFired()

        assertEquals(listOf(listOf("Rye")), app.presenter.notifications)
    }

    @Test
    fun `starters coming due together are coalesced into one notification`() = runTest(mainDispatcherRule.dispatcher) {
        val rye = createStarter("Rye", activeIntervalHours = 24)
        val white = createStarter("White", activeIntervalHours = 24)
        feedNow(rye)
        app.clock.advanceBy(Duration.ofMinutes(5)) // white comes due 5 min after rye
        feedNow(white)

        app.clock.advanceBy(Duration.ofHours(24).minus(Duration.ofMinutes(5)))
        app.repository.onAlarmFired()

        assertEquals(1, app.presenter.notifications.size)
        assertEquals(setOf("Rye", "White"), app.presenter.notifications.single().toSet())
    }

    @Test
    fun `a due event never notifies twice`() = runTest(mainDispatcherRule.dispatcher) {
        val id = createStarter("Rye", activeIntervalHours = 24)
        feedNow(id)

        app.clock.advanceBy(Duration.ofHours(24))
        app.repository.onAlarmFired()
        app.clock.advanceBy(Duration.ofHours(1)) // still unfed an hour later
        app.repository.onAlarmFired()

        assertEquals(1, app.presenter.notifications.size)
    }

    @Test
    fun `feeding again arms a fresh due event`() = runTest(mainDispatcherRule.dispatcher) {
        val id = createStarter("Rye", activeIntervalHours = 24)
        feedNow(id)
        app.clock.advanceBy(Duration.ofHours(24))
        app.repository.onAlarmFired()

        feedNow(id) // fed after the reminder — cycle restarts
        app.clock.advanceBy(Duration.ofHours(24))
        app.repository.onAlarmFired()

        assertEquals(2, app.presenter.notifications.size)
    }

    @Test
    fun `archived starters never schedule alarms`() = runTest(mainDispatcherRule.dispatcher) {
        val id = createStarter("Old friend", state = LifecycleState.ARCHIVED)
        feedNow(id)

        // The last reschedule found nothing to wake up for.
        assertTrue(app.scheduler.cancelled > 0)
        app.clock.advanceBy(Duration.ofHours(500))
        app.repository.onAlarmFired()
        assertTrue(app.presenter.notifications.isEmpty())
    }

    @Test
    fun `overdue-at-boot schedules an immediate firing rather than skipping it`() = runTest(mainDispatcherRule.dispatcher) {
        val id = createStarter("Rye", activeIntervalHours = 24)
        feedNow(id)

        // Device was off past the due time; reschedule clamps to now, not the past.
        app.clock.advanceBy(Duration.ofHours(30))
        app.coordinator.reschedule(app.clock.instant())

        assertEquals(app.clock.instant(), app.scheduler.lastScheduledAt)
    }
}
