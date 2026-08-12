package com.ayushojha.levain

import app.cash.turbine.test
import com.ayushojha.levain.data.Bake
import com.ayushojha.levain.data.Feeding
import com.ayushojha.levain.data.RiseRating
import com.ayushojha.levain.data.Smell
import com.ayushojha.levain.data.Starter
import com.ayushojha.levain.ui.bake.BakeViewModel
import com.ayushojha.levain.ui.observation.ObservationViewModel
import com.ayushojha.levain.ui.timeline.TimelineEvent
import com.ayushojha.levain.ui.timeline.TimelineViewModel
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
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
class ObservationBakeTimelineTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: TestApp
    private var starterId: Long = 0

    @Before
    fun setUp() {
        app = TestApp()
    }

    @After
    fun tearDown() {
        app.close()
    }

    private suspend fun createStarter(): Long = app.repository.createStarter(
        Starter(name = "Rye", createdAtEpochMs = app.clock.instant().toEpochMilli())
    )

    @Test
    fun `observation saves structured fields and free text`() = runTest(mainDispatcherRule.dispatcher) {
        starterId = createStarter()
        val vm = ObservationViewModel(app.repository, app.clock, starterId)

        vm.setRiseRating(RiseRating.SLUGGISH)
        vm.setTimeToPeakMinutes(300)
        vm.setSmell(Smell.ACETONE)
        vm.setNote("Smells like nail polish — needs more frequent feeds")
        vm.save()
        advanceUntilIdle()

        val saved = app.repository.observeObservations(starterId).first().single()
        assertEquals(RiseRating.SLUGGISH, saved.riseRating)
        assertEquals(300, saved.timeToPeakMinutes)
        assertEquals(Smell.ACETONE, saved.smell)
        assertEquals("Smells like nail polish — needs more frequent feeds", saved.note)
        assertEquals(app.clock.instant().toEpochMilli(), saved.timestampEpochMs)
    }

    @Test
    fun `bake links outcome back to its source starter`() = runTest(mainDispatcherRule.dispatcher) {
        starterId = createStarter()
        val vm = BakeViewModel(app.repository, app.clock, starterId)

        vm.setLevainNotes("1:2:2 build, used at peak")
        vm.setOutcomeRating(2)
        vm.setNote("Flat loaf")
        vm.save()
        advanceUntilIdle()

        val saved = app.repository.observeBakes(starterId).first().single()
        assertEquals(starterId, saved.starterId)
        assertEquals(2, saved.outcomeRating)
        assertEquals("1:2:2 build, used at peak", saved.levainNotes)
    }

    @Test
    fun `timeline interleaves feedings observations and bakes newest first`() = runTest(mainDispatcherRule.dispatcher) {
        starterId = createStarter()
        val t0 = app.clock.instant()

        app.repository.logFeeding(
            Feeding(starterId = starterId, timestampEpochMs = t0.toEpochMilli(), ratio = "1:5:5", flourType = "Rye")
        )
        app.repository.logObservation(
            com.ayushojha.levain.data.HealthObservation(
                starterId = starterId,
                timestampEpochMs = t0.plus(Duration.ofHours(4)).toEpochMilli(),
                riseRating = RiseRating.PEAKED,
            )
        )
        app.repository.logBake(
            Bake(
                starterId = starterId,
                timestampEpochMs = t0.plus(Duration.ofHours(8)).toEpochMilli(),
                outcomeRating = 5,
            )
        )

        val vm = TimelineViewModel(app.repository, starterId)
        vm.uiState.test {
            val events = awaitMatching { it.events.size == 3 }.events
            assertTrue(events[0] is TimelineEvent.BakeEvent)
            assertTrue(events[1] is TimelineEvent.ObservationEvent)
            assertTrue(events[2] is TimelineEvent.FeedingEvent)
        }
    }

    @Test
    fun `deleting an event removes it from the timeline`() = runTest(mainDispatcherRule.dispatcher) {
        starterId = createStarter()
        val feedingId = app.repository.logFeeding(
            Feeding(
                starterId = starterId,
                timestampEpochMs = app.clock.instant().toEpochMilli(),
                ratio = "1:5:5",
                flourType = "Rye",
            )
        )

        val vm = TimelineViewModel(app.repository, starterId)
        vm.uiState.test {
            val event = awaitMatching { it.events.size == 1 }.events.single() as TimelineEvent.FeedingEvent
            assertEquals(feedingId, event.feeding.id)
            vm.delete(event)
            awaitMatching { it.events.isEmpty() }
        }
    }
}
