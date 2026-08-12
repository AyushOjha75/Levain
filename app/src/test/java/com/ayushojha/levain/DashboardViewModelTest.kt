package com.ayushojha.levain

import app.cash.turbine.test
import com.ayushojha.levain.data.Feeding
import com.ayushojha.levain.data.HealthObservation
import com.ayushojha.levain.data.LifecycleState
import com.ayushojha.levain.data.RiseRating
import com.ayushojha.levain.data.Starter
import com.ayushojha.levain.domain.DueStatus
import com.ayushojha.levain.ui.dashboard.DashboardViewModel
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DashboardViewModelTest {

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

    private fun viewModel() = DashboardViewModel(app.repository, app.clock)

    private suspend fun createStarter(
        name: String = "Rye",
        state: LifecycleState = LifecycleState.ACTIVE,
        activeIntervalHours: Int = 24,
        dormantIntervalHours: Int = 168,
    ): Long = app.repository.createStarter(
        Starter(
            name = name,
            state = state,
            activeIntervalHours = activeIntervalHours,
            dormantIntervalHours = dormantIntervalHours,
            createdAtEpochMs = app.clock.instant().toEpochMilli(),
        )
    )

    private suspend fun feed(starterId: Long, hoursAgo: Long) {
        app.repository.logFeeding(
            Feeding(
                starterId = starterId,
                timestampEpochMs = app.clock.instant().minus(Duration.ofHours(hoursAgo)).toEpochMilli(),
                ratio = "1:5:5",
                flourType = "Rye",
            )
        )
    }

    @Test
    fun `starter fed 25h ago with 24h active interval shows overdue`() = runTest(mainDispatcherRule.dispatcher) {
        val id = createStarter(activeIntervalHours = 24)
        feed(id, hoursAgo = 25)

        viewModel().uiState.test {
            val card = awaitMatching { it.cards.isNotEmpty() }.cards.single { it.starter.id == id }
            assertEquals(DueStatus.OVERDUE, card.dueness.status)
        }
    }

    @Test
    fun `starter fed 2h ago with 24h active interval is ok`() = runTest(mainDispatcherRule.dispatcher) {
        val id = createStarter(activeIntervalHours = 24)
        feed(id, hoursAgo = 2)

        viewModel().uiState.test {
            val card = awaitMatching { it.cards.isNotEmpty() }.cards.single { it.starter.id == id }
            assertEquals(DueStatus.OK, card.dueness.status)
        }
    }

    @Test
    fun `dormant starter uses its dormant interval`() = runTest(mainDispatcherRule.dispatcher) {
        val id = createStarter(state = LifecycleState.DORMANT, dormantIntervalHours = 168)
        feed(id, hoursAgo = 48) // 2 days: overdue for an active starter, fine for a fridge one

        viewModel().uiState.test {
            val card = awaitMatching { it.cards.isNotEmpty() }.cards.single { it.starter.id == id }
            assertEquals(DueStatus.OK, card.dueness.status)
        }
    }

    @Test
    fun `archived starter is never due even when long unfed`() = runTest(mainDispatcherRule.dispatcher) {
        val id = createStarter(state = LifecycleState.ARCHIVED)
        feed(id, hoursAgo = 24 * 30)

        viewModel().uiState.test {
            val card = awaitMatching { it.cards.isNotEmpty() }.cards.single { it.starter.id == id }
            assertEquals(DueStatus.NEVER_DUE, card.dueness.status)
            assertNull(card.dueness.dueAt)
        }
    }

    @Test
    fun `never-fed active starter comes due from its creation time`() = runTest(mainDispatcherRule.dispatcher) {
        val id = createStarter(activeIntervalHours = 24)
        app.clock.advanceBy(Duration.ofHours(26))

        viewModel().uiState.test {
            val card = awaitMatching { it.cards.isNotEmpty() }.cards.single { it.starter.id == id }
            assertEquals(DueStatus.OVERDUE, card.dueness.status)
        }
    }

    @Test
    fun `card carries last feeding and last observation`() = runTest(mainDispatcherRule.dispatcher) {
        val id = createStarter()
        feed(id, hoursAgo = 3)
        app.repository.logObservation(
            HealthObservation(
                starterId = id,
                timestampEpochMs = app.clock.instant().toEpochMilli(),
                riseRating = RiseRating.PEAKED,
            )
        )

        viewModel().uiState.test {
            val card = awaitMatching { it.cards.isNotEmpty() }.cards.single { it.starter.id == id }
            assertEquals("1:5:5", card.lastFeeding?.ratio)
            assertEquals(RiseRating.PEAKED, card.lastObservation?.riseRating)
        }
    }
}
