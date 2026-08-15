package com.ayushojha.levain

import com.ayushojha.levain.data.LifecycleState
import com.ayushojha.levain.domain.DoughMath
import com.ayushojha.levain.domain.StarterProgram
import com.ayushojha.levain.domain.TroubleshootingNode
import com.ayushojha.levain.domain.TroubleshootingTree
import com.ayushojha.levain.ui.wizard.StarterWizardViewModel
import com.ayushojha.levain.ui.wizard.TroubleshootingViewModel
import com.ayushojha.levain.ui.wizard.WizardStep
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WizardAndGadgetsTest {

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

    @Test
    fun `onboarding path creates a starter with its first feeding`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = StarterWizardViewModel(app.repository, app.clock)
        vm.chooseHasStarter(true)
        vm.setName("Rye")
        vm.advanceFromName()
        vm.setHome(LifecycleState.DORMANT)
        vm.advanceFromHome()
        vm.setRatio("1:3:3")
        vm.setFlourType("Rye")
        vm.finishOnboarding(fedJustNow = true)
        advanceUntilIdle()

        assertEquals(WizardStep.DONE, vm.uiState.value.step)
        val starter = app.repository.observeStarters().first().single()
        assertEquals("Rye", starter.name)
        assertEquals(LifecycleState.DORMANT, starter.state)
        val feeding = app.repository.getLastFeeding(starter.id)
        assertNotNull(feeding)
        assertEquals("1:3:3", feeding!!.ratio)
    }

    @Test
    fun `program path creates a day-1 program starter and day advances with time`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = StarterWizardViewModel(app.repository, app.clock)
        vm.chooseHasStarter(false)
        assertEquals(WizardStep.PROGRAM_NAME, vm.uiState.value.step)
        vm.setName("Baby")
        vm.startProgram()
        advanceUntilIdle()

        val starter = app.repository.observeStarters().first().single()
        assertNotNull(starter.programStartedAtEpochMs)
        val startedAt = java.time.Instant.ofEpochMilli(starter.programStartedAtEpochMs!!)
        assertEquals(1, StarterProgram.currentDay(startedAt, app.clock.instant(), app.clock.zone))

        app.clock.advanceBy(Duration.ofDays(3))
        assertEquals(4, StarterProgram.currentDay(startedAt, app.clock.instant(), app.clock.zone))

        app.clock.advanceBy(Duration.ofDays(10))
        assertTrue(StarterProgram.isComplete(startedAt, app.clock.instant(), app.clock.zone))
    }

    @Test
    fun `program starter is not due during the no-feed first two days`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = StarterWizardViewModel(app.repository, app.clock)
        vm.chooseHasStarter(false)
        vm.setName("Baby")
        vm.startProgram()
        advanceUntilIdle()

        val starter = app.repository.observeStarters().first().single()

        // Day 2 says "no feeding today" — no reminder, no hungry face.
        app.clock.advanceBy(Duration.ofHours(30))
        val day2 = com.ayushojha.levain.domain.DueCalculator.dueness(starter, null, app.clock.instant())
        assertEquals(com.ayushojha.levain.domain.DueStatus.OK, day2.status)

        // By day 3 the daily rhythm starts.
        app.clock.advanceBy(Duration.ofHours(20))
        val day3 = com.ayushojha.levain.domain.DueCalculator.dueness(starter, null, app.clock.instant())
        assertTrue(day3.status != com.ayushojha.levain.domain.DueStatus.OK)
    }

    @Test
    fun `troubleshooting acetone path reaches the hungry-starter diagnosis`() {
        val vm = TroubleshootingViewModel()
        val root = vm.uiState.value.node as TroubleshootingNode.Question
        val acetone = root.options.first { it.first.contains("acetone") }.second
        vm.select(acetone)

        val diagnosis = vm.uiState.value.node
        assertTrue(diagnosis is TroubleshootingNode.Diagnosis)
        assertTrue((diagnosis as TroubleshootingNode.Diagnosis).title.contains("hungry"))

        vm.back()
        assertEquals(TroubleshootingTree.root, vm.uiState.value.node)
    }

    @Test
    fun `levain build math splits target into seed flour water`() {
        // 220g at 100% hydration, 20% seed: flour = 220 / 2.2 = 100
        val build = DoughMath.levainBuild(targetGrams = 220, hydrationPct = 100, inoculationPct = 20)
        assertEquals(100, build.flourGrams)
        assertEquals(100, build.waterGrams)
        assertEquals(20, build.seedGrams)
    }

    @Test
    fun `bakers percentages fold the levain halves into hydration`() {
        // 500f + 350w + 100 levain (50f/50w): hydration = 400/550 = 72.7 → 73%
        val p = DoughMath.bakersPercentages(flourGrams = 500, waterGrams = 350, saltGrams = 10.0, levainGrams = 100)
        assertEquals(73, p.hydrationPct)
        assertEquals(2.0, p.saltPct, 0.001)
        assertEquals(20, p.levainPct)
    }
}
