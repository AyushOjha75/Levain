package com.ayushojha.levain

import app.cash.turbine.test
import com.ayushojha.levain.data.Feeding
import com.ayushojha.levain.data.Starter
import com.ayushojha.levain.ui.feeding.FeedingViewModel
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FeedingViewModelTest {

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
    fun `form is pre-filled from the previous feeding`() = runTest(mainDispatcherRule.dispatcher) {
        starterId = createStarter()
        app.repository.logFeeding(
            Feeding(
                starterId = starterId,
                timestampEpochMs = app.clock.instant().minus(Duration.ofHours(20)).toEpochMilli(),
                ratio = "1:5:5",
                flourType = "Whole wheat",
            )
        )

        val vm = FeedingViewModel(app.repository, app.clock, starterId)
        vm.uiState.test {
            val state = awaitMatching { it.ratio.isNotEmpty() }
            assertEquals("1:5:5", state.ratio)
            assertEquals("Whole wheat", state.flourType)
        }
    }

    @Test
    fun `saving logs a feeding with the current time by default`() = runTest(mainDispatcherRule.dispatcher) {
        starterId = createStarter()
        val vm = FeedingViewModel(app.repository, app.clock, starterId)
        advanceUntilIdle()

        vm.setRatio("1:2:2")
        vm.setFlourType("Rye")
        vm.save()
        advanceUntilIdle()

        val logged = app.repository.getLastFeeding(starterId)!!
        assertEquals("1:2:2", logged.ratio)
        assertEquals("Rye", logged.flourType)
        assertEquals(app.clock.instant().toEpochMilli(), logged.timestampEpochMs)
    }

    @Test
    fun `timestamp can be edited to back-fill a late-logged feeding`() = runTest(mainDispatcherRule.dispatcher) {
        starterId = createStarter()
        val vm = FeedingViewModel(app.repository, app.clock, starterId)
        advanceUntilIdle()

        val threeHoursAgo = app.clock.instant().minus(Duration.ofHours(3)).toEpochMilli()
        vm.setTimestamp(threeHoursAgo)
        vm.save()
        advanceUntilIdle()

        assertEquals(threeHoursAgo, app.repository.getLastFeeding(starterId)!!.timestampEpochMs)
    }
}
