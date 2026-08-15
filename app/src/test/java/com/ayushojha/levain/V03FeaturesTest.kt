package com.ayushojha.levain

import app.cash.turbine.test
import com.ayushojha.levain.data.Bake
import com.ayushojha.levain.data.BackupManager
import com.ayushojha.levain.data.Feeding
import com.ayushojha.levain.data.HealthObservation
import com.ayushojha.levain.data.LifecycleState
import com.ayushojha.levain.data.RiseRating
import com.ayushojha.levain.data.Starter
import com.ayushojha.levain.domain.InsightsCalculator
import com.ayushojha.levain.domain.RiseTrend
import com.ayushojha.levain.ui.feeding.FeedingViewModel
import com.ayushojha.levain.ui.settings.SettingsViewModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
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
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class V03FeaturesTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var app: TestApp
    private lateinit var photosDir: File

    @Before
    fun setUp() {
        app = TestApp()
        photosDir = tempFolder.newFolder("photos")
    }

    @After
    fun tearDown() {
        app.close()
    }

    private fun backupManager() = BackupManager(app.db, photosDir)

    private suspend fun seedStarter(name: String = "Rye"): Long = app.repository.createStarter(
        Starter(name = name, createdAtEpochMs = app.clock.instant().toEpochMilli())
    )

    @Test
    fun `backup round-trips data and photos through the settings seam`() = runTest(mainDispatcherRule.dispatcher) {
        val id = seedStarter()
        app.repository.logFeeding(
            Feeding(starterId = id, timestampEpochMs = app.clock.instant().toEpochMilli(), ratio = "1:5:5", flourType = "Rye")
        )
        app.repository.logObservation(
            HealthObservation(
                starterId = id,
                timestampEpochMs = app.clock.instant().toEpochMilli(),
                riseRating = RiseRating.PEAKED,
                photoPath = "shot.jpg",
                note = "gorgeous",
            )
        )
        app.repository.logBake(
            Bake(starterId = id, timestampEpochMs = app.clock.instant().toEpochMilli(), outcomeRating = 5)
        )
        File(photosDir, "shot.jpg").writeBytes(byteArrayOf(1, 2, 3, 4))

        val vm = SettingsViewModel(backupManager(), app.repository, mainDispatcherRule.dispatcher)
        val out = ByteArrayOutputStream()
        vm.export { out }
        advanceUntilIdle()
        assertTrue(vm.uiState.value.lastResult!!.contains("✓"))

        // Wreck everything, then restore.
        app.repository.deleteStarter(id)
        File(photosDir, "shot.jpg").delete()
        assertTrue(app.repository.observeStarters().first().isEmpty())

        vm.import { ByteArrayInputStream(out.toByteArray()) }
        advanceUntilIdle()
        assertTrue(vm.uiState.value.lastResult!!.contains("✓"))
        // A restored phone must get its reminders back.
        assertTrue(app.scheduler.scheduled.isNotEmpty())

        val restored = app.repository.observeStarters().first().single()
        assertEquals("Rye", restored.name)
        assertEquals("1:5:5", app.repository.getLastFeeding(restored.id)!!.ratio)
        assertEquals("gorgeous", app.repository.observeObservations(restored.id).first().single().note)
        assertEquals(5, app.repository.observeBakes(restored.id).first().single().outcomeRating)
        assertEquals(4, File(photosDir, "shot.jpg").readBytes().size)
    }

    @Test
    fun `streaks judge each gap against its own era so dormancy pauses`() = runTest(mainDispatcherRule.dispatcher) {
        val id = seedStarter()

        suspend fun feedNow() {
            app.repository.logFeeding(
                Feeding(starterId = id, timestampEpochMs = app.clock.instant().toEpochMilli(), ratio = "1:5:5", flourType = "White")
            )
        }

        // Two on-time counter feedings (24h era).
        feedNow()
        app.clock.advanceBy(Duration.ofHours(24)); feedNow()

        // Into the fridge: weekly era, fed on its weekly schedule.
        val starter = app.repository.getStarter(id)!!
        app.repository.updateStarter(starter.copy(state = LifecycleState.DORMANT))
        app.clock.advanceBy(Duration.ofDays(7)); feedNow()
        app.clock.advanceBy(Duration.ofDays(7)); feedNow()

        // Back to the counter; feed on time again.
        app.repository.updateStarter(app.repository.getStarter(id)!!.copy(state = LifecycleState.ACTIVE))
        app.clock.advanceBy(Duration.ofHours(24)); feedNow()

        val vm = com.ayushojha.levain.ui.dashboard.DashboardViewModel(app.repository, app.clock)
        vm.uiState.test {
            val card = awaitMatching { it.cards.isNotEmpty() }.cards.single()
            // All five feedings were on time within their own eras: streak unbroken.
            assertEquals(5, card.vitals.feedingStreak)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fridge-exit feeding after a dormant-era gap keeps the streak`() = runTest(mainDispatcherRule.dispatcher) {
        val id = seedStarter()
        suspend fun feedNow() {
            app.repository.logFeeding(
                Feeding(starterId = id, timestampEpochMs = app.clock.instant().toEpochMilli(), ratio = "1:5:5", flourType = "White")
            )
        }
        // Dormant era: fed weekly.
        app.repository.updateStarter(app.repository.getStarter(id)!!.copy(state = LifecycleState.DORMANT))
        feedNow()
        app.clock.advanceBy(Duration.ofDays(7)); feedNow()

        // Taken out of the fridge, flipped to ACTIVE, fed after the last dormant gap.
        app.repository.updateStarter(app.repository.getStarter(id)!!.copy(state = LifecycleState.ACTIVE))
        app.clock.advanceBy(Duration.ofDays(6)); feedNow() // gap started dormant — on time for that era

        val vm = com.ayushojha.levain.ui.dashboard.DashboardViewModel(app.repository, app.clock)
        vm.uiState.test {
            val card = awaitMatching { it.cards.isNotEmpty() }.cards.single()
            assertEquals(3, card.vitals.feedingStreak)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an archive written by v0_3 still imports`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = SettingsViewModel(backupManager(), app.repository, mainDispatcherRule.dispatcher)

        // Format 1: no bake status, no scale, starterId always present, rating required.
        val out = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(out).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("data.json"))
            zip.write(
                """{"version":1,
                   "starters":[{"id":1,"name":"Rye","state":"ACTIVE","activeIntervalHours":24,
                                "dormantIntervalHours":168,"createdAtEpochMs":1000}],
                   "feedings":[{"id":1,"starterId":1,"timestampEpochMs":2000,"ratio":"1:5:5","flourType":"Rye"}],
                   "observations":[],
                   "bakes":[{"id":1,"starterId":1,"timestampEpochMs":4000,"outcomeRating":5}]}""".toByteArray()
            )
            zip.closeEntry()
        }

        vm.import { ByteArrayInputStream(out.toByteArray()) }
        advanceUntilIdle()
        assertTrue(vm.uiState.value.lastResult!!.contains("✓"))

        val starter = app.repository.observeStarters().first().single()
        assertEquals("Rye", starter.name)
        val bake = app.repository.observeBakes(starter.id).first().single()
        // An old archive's bakes land as finished bakes at scale 1 — same rule
        // the schema migration applies to old rows.
        assertEquals(com.ayushojha.levain.data.BakeStatus.FINISHED, bake.status)
        assertEquals(1.0, bake.scale, 0.0001)
        assertEquals(5, bake.outcomeRating)
    }

    @Test
    fun `corrupt import rolls back and leaves current data intact`() = runTest(mainDispatcherRule.dispatcher) {
        val id = seedStarter("Survivor")
        val vm = SettingsViewModel(backupManager(), app.repository, mainDispatcherRule.dispatcher)

        // A structurally-valid zip whose data.json has a poisoned enum value.
        val out = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(out).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("data.json"))
            zip.write(
                """{"version":1,"starters":[{"id":9,"name":"Bad","state":"NOT_A_STATE",
                   "activeIntervalHours":24,"dormantIntervalHours":168,"createdAtEpochMs":0}],
                   "feedings":[],"observations":[],"bakes":[]}""".toByteArray()
            )
            zip.closeEntry()
        }

        vm.import { ByteArrayInputStream(out.toByteArray()) }
        advanceUntilIdle()

        assertTrue(vm.uiState.value.lastResult!!.contains("failed", ignoreCase = true))
        // The wipe rolled back: the original starter survives.
        assertEquals("Survivor", app.repository.observeStarters().first().single().name)
    }

    @Test
    fun `editing a feeding preserves identity and updates fields`() = runTest(mainDispatcherRule.dispatcher) {
        val id = seedStarter()
        app.repository.logFeeding(
            Feeding(starterId = id, timestampEpochMs = app.clock.instant().toEpochMilli(), ratio = "1:5:5", flourType = "White")
        )
        val original = app.repository.getLastFeeding(id)!!

        val vm = FeedingViewModel(app.repository, app.clock, id, feedingId = original.id)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.editing)
        assertEquals("1:5:5", vm.uiState.value.ratio)

        vm.setRatio("1:2:2")
        vm.save()
        advanceUntilIdle()

        val updated = app.repository.getFeeding(original.id)!!
        assertEquals("1:2:2", updated.ratio)
        assertEquals(original.timestampEpochMs, updated.timestampEpochMs)
        assertEquals(1, app.repository.observeFeedings(id).first().size)
    }

    @Test
    fun `when-chips back-fill the fed timestamp`() = runTest(mainDispatcherRule.dispatcher) {
        val id = seedStarter()
        val vm = FeedingViewModel(app.repository, app.clock, id)
        advanceUntilIdle()

        vm.setHoursAgo(3)
        vm.save()
        advanceUntilIdle()

        val expected = app.clock.instant().minus(Duration.ofHours(3)).toEpochMilli()
        assertEquals(expected, app.repository.getLastFeeding(id)!!.timestampEpochMs)
    }

    @Test
    fun `insights compute rhythm on-time rate trend and bake stats`() {
        val base = 1_000_000_000_000L
        val h = 3_600_000L
        val feedings = listOf(
            Feeding(1, 1, base, "1:5:5", "W", 24),
            Feeding(2, 1, base + 24 * h, "1:5:5", "W", 24),
            Feeding(3, 1, base + 48 * h, "1:5:5", "W", 24),
            Feeding(4, 1, base + 100 * h, "1:5:5", "W", 24), // 52h gap — late
        )
        val observations = listOf(
            HealthObservation(1, 1, base, RiseRating.FLAT),
            HealthObservation(2, 1, base + 1, RiseRating.SLUGGISH),
            HealthObservation(3, 1, base + 2, RiseRating.RISING),
            HealthObservation(4, 1, base + 3, RiseRating.PEAKED),
        )
        val bakes = listOf(
            Bake(id = 1, starterId = 1, timestampEpochMs = base, outcomeRating = 4),
            Bake(id = 2, starterId = 1, timestampEpochMs = base, outcomeRating = 5),
        )

        val insights = InsightsCalculator.insights(feedings, observations, bakes)
        assertEquals(33, insights.avgGapHours) // (24+24+52)/3 = 33.3
        assertEquals(66, insights.onTimePercent) // 2 of 3 gaps on time
        assertEquals(RiseTrend.IMPROVING, insights.riseTrend)
        assertEquals(2, insights.bakeCount)
        assertEquals(4.5, insights.avgBakeRating!!, 0.001)
    }
}
