package com.ayushojha.levain

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ayushojha.levain.data.BakeStatus
import com.ayushojha.levain.data.LevainDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A real v0.3 database file, opened by the shipping build.
 *
 * Room runs the migration itself here rather than the test calling
 * `migrate()` directly — that way Room's own schema validation runs against
 * the migrated database, so hand-written DDL that drifts from the entities
 * fails the test instead of failing on someone's phone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class Migration3to4Test {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun createV3Database(name: String) {
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `starter` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `state` TEXT NOT NULL,
                `activeIntervalHours` INTEGER NOT NULL,
                `dormantIntervalHours` INTEGER NOT NULL,
                `createdAtEpochMs` INTEGER NOT NULL,
                `lastNotifiedDueAtEpochMs` INTEGER,
                `programStartedAtEpochMs` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `feeding` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `starterId` INTEGER NOT NULL,
                `timestampEpochMs` INTEGER NOT NULL,
                `ratio` TEXT NOT NULL,
                `flourType` TEXT NOT NULL,
                `intervalHoursAtFeeding` INTEGER,
                FOREIGN KEY(`starterId`) REFERENCES `starter`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_feeding_starterId` ON `feeding` (`starterId`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `health_observation` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `starterId` INTEGER NOT NULL,
                `timestampEpochMs` INTEGER NOT NULL,
                `riseRating` TEXT NOT NULL,
                `timeToPeakMinutes` INTEGER,
                `smell` TEXT,
                `photoPath` TEXT,
                `note` TEXT,
                FOREIGN KEY(`starterId`) REFERENCES `starter`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_observation_starterId` ON `health_observation` (`starterId`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bake` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `starterId` INTEGER NOT NULL,
                `timestampEpochMs` INTEGER NOT NULL,
                `levainNotes` TEXT,
                `outcomeRating` INTEGER NOT NULL,
                `photoPath` TEXT,
                `note` TEXT,
                FOREIGN KEY(`starterId`) REFERENCES `starter`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bake_starterId` ON `bake` (`starterId`)")

        // A starter with two years of life in it, and a loaf it made.
        db.execSQL(
            "INSERT INTO starter (id, name, state, activeIntervalHours, dormantIntervalHours, createdAtEpochMs, lastNotifiedDueAtEpochMs, programStartedAtEpochMs) " +
                "VALUES (1, 'Rye', 'ACTIVE', 24, 168, 1000, NULL, NULL)"
        )
        db.execSQL(
            "INSERT INTO feeding (id, starterId, timestampEpochMs, ratio, flourType, intervalHoursAtFeeding) " +
                "VALUES (1, 1, 2000, '1:5:5', 'Rye', 24)"
        )
        db.execSQL(
            "INSERT INTO health_observation (id, starterId, timestampEpochMs, riseRating, timeToPeakMinutes, smell, photoPath, note) " +
                "VALUES (1, 1, 3000, 'PEAKED', 240, 'TANGY', 'shot.jpg', 'gorgeous')"
        )
        db.execSQL(
            "INSERT INTO bake (id, starterId, timestampEpochMs, levainNotes, outcomeRating, photoPath, note) " +
                "VALUES (1, 1, 4000, '1:2:2 build', 5, 'loaf.jpg', 'best yet')"
        )

        db.version = 3
        db.close()
    }

    private fun openMigrated(name: String): LevainDatabase =
        Room.databaseBuilder(context, LevainDatabase::class.java, name)
            .addMigrations(
                LevainDatabase.MIGRATION_1_2,
                LevainDatabase.MIGRATION_2_3,
                LevainDatabase.MIGRATION_3_4,
            )
            .setQueryExecutor(Runnable::run)
            .setTransactionExecutor(Runnable::run)
            .allowMainThreadQueries()
            .build()

    @Test
    fun `a v3 database opens on the new build with every row intact`() = runTest {
        val name = "migration-intact.db"
        createV3Database(name)

        val db = openMigrated(name)
        val dao = db.levainDao()

        val starter = dao.getStarters().single()
        assertEquals("Rye", starter.name)
        assertEquals(1000L, starter.createdAtEpochMs)

        val feeding = dao.getLastFeeding(1)!!
        assertEquals("1:5:5", feeding.ratio)
        assertEquals(24, feeding.intervalHoursAtFeeding)

        val observation = dao.getAllObservations().single()
        assertEquals("gorgeous", observation.note)
        assertEquals("shot.jpg", observation.photoPath)

        val bake = dao.getAllBakes().single()
        assertEquals(4000L, bake.timestampEpochMs)
        assertEquals(5, bake.outcomeRating)
        assertEquals("loaf.jpg", bake.photoPath)
        assertEquals(1L, bake.starterId)

        // Everything that existed before the bake-centric turn is a finished
        // bake at scale 1 with no steps — "logged without a guided run".
        assertEquals(BakeStatus.FINISHED, bake.status)
        assertEquals(1.0, bake.scale, 0.0001)
        assertNull(bake.recipeId)
        assertTrue(dao.getBakeSteps(bake.id).isEmpty())

        db.close()
        context.deleteDatabase(name)
    }

    @Test
    fun `the new tables are usable after migrating`() = runTest {
        val name = "migration-newtables.db"
        createV3Database(name)

        val db = openMigrated(name)
        val dao = db.levainDao()

        dao.upsertRecipe(
            com.ayushojha.levain.data.Recipe(
                id = "focaccia-yeasted",
                name = "Yeasted focaccia",
                summary = "Same day, no starter needed.",
                breadType = "focaccia",
                requiresStarter = false,
                referenceBatch = "one 9x13 pan",
                contentVersion = 1,
            )
        )
        dao.upsertStepTemplates(
            listOf(
                com.ayushojha.levain.data.RecipeStepTemplate(
                    recipeId = "focaccia-yeasted",
                    position = 0,
                    title = "Fold",
                    instruction = "Corner to centre, four times round.",
                    kind = com.ayushojha.levain.data.StepKind.TIMED,
                    durationMinutes = 30,
                    repeatCount = 4,
                    repeatEveryMinutes = 30,
                    phase = "bulk",
                )
            )
        )

        assertEquals("Yeasted focaccia", dao.getRecipe("focaccia-yeasted")!!.name)
        assertEquals(4, dao.getStepTemplates("focaccia-yeasted").single().repeatCount)

        db.close()
        context.deleteDatabase(name)
    }

    @Test
    fun `deleting a starter orphans its bakes instead of erasing them`() = runTest {
        val name = "migration-setnull.db"
        createV3Database(name)

        val db = openMigrated(name)
        val dao = db.levainDao()

        dao.deleteStarter(1)

        // The culture is gone; the loaf it made is still history.
        val bake = dao.getAllBakes().single()
        assertNull(bake.starterId)
        assertEquals(5, bake.outcomeRating)
        // Its feedings and observations did cascade — those belong to the starter.
        assertTrue(dao.getAllFeedings().isEmpty())
        assertNotNull(bake)

        db.close()
        context.deleteDatabase(name)
    }

    @Test
    fun `an active bake is found without a starter at all`() = runTest {
        val name = "migration-active.db"
        createV3Database(name)

        val db = openMigrated(name)
        val dao = db.levainDao()

        val id = dao.insertBake(
            com.ayushojha.levain.data.Bake(
                starterId = null,
                recipeId = "focaccia-yeasted",
                status = BakeStatus.ACTIVE,
                timestampEpochMs = 9000,
                startedAtEpochMs = 9000,
            )
        )
        dao.insertBakeSteps(
            listOf(
                com.ayushojha.levain.data.BakeStep(
                    bakeId = id,
                    position = 0,
                    title = "Mix",
                    instruction = "Combine everything.",
                    kind = com.ayushojha.levain.data.StepKind.ACTION,
                )
            )
        )

        val active = dao.observeActiveBake().first()
        assertEquals(id, active!!.id)
        assertNull(active.starterId)
        assertNull(active.outcomeRating)
        assertEquals(1, dao.getBakeSteps(id).size)

        db.close()
        context.deleteDatabase(name)
    }
}
