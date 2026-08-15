package com.ayushojha.levain.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Starter::class,
        Feeding::class,
        HealthObservation::class,
        Bake::class,
        Recipe::class,
        RecipeIngredient::class,
        RecipeStepTemplate::class,
        BakeStep::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class LevainDatabase : RoomDatabase() {

    abstract fun levainDao(): LevainDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE starter ADD COLUMN programStartedAtEpochMs INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE feeding ADD COLUMN intervalHoursAtFeeding INTEGER")
            }
        }

        /**
         * The bake-centric turn. Adds Recipes, their ingredients and step
         * templates, and snapshotted Bake steps; rebuilds `bake` so it can be
         * in progress, carry a scale, and survive its Starter being deleted.
         *
         * SQLite can't alter a foreign key, so `bake` is rebuilt rather than
         * altered. Every existing bake becomes a FINISHED bake at scale 1 with
         * no steps — exactly what "logged without a guided run" means.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recipe` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `breadType` TEXT NOT NULL,
                        `requiresStarter` INTEGER NOT NULL,
                        `referenceBatch` TEXT NOT NULL,
                        `contentVersion` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recipe_ingredient` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `recipeId` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `grams` REAL NOT NULL,
                        `bakersPercent` REAL,
                        `phase` TEXT NOT NULL,
                        FOREIGN KEY(`recipeId`) REFERENCES `recipe`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredient_recipeId` ON `recipe_ingredient` (`recipeId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recipe_step` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `recipeId` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `instruction` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `durationMinutes` INTEGER,
                        `cue` TEXT,
                        `repeatCount` INTEGER NOT NULL,
                        `repeatEveryMinutes` INTEGER,
                        `phase` TEXT NOT NULL,
                        FOREIGN KEY(`recipeId`) REFERENCES `recipe`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_step_recipeId` ON `recipe_step` (`recipeId`)")

                // Rebuild `bake`: nullable starter, SET NULL on delete, plus
                // status, scale, recipe provenance and run timestamps.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bake_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `starterId` INTEGER,
                        `recipeId` TEXT,
                        `recipeContentVersion` INTEGER,
                        `status` TEXT NOT NULL,
                        `scale` REAL NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        `startedAtEpochMs` INTEGER,
                        `heldAtEpochMs` INTEGER,
                        `levainNotes` TEXT,
                        `outcomeRating` INTEGER,
                        `photoPath` TEXT,
                        `note` TEXT,
                        FOREIGN KEY(`starterId`) REFERENCES `starter`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `bake_new` (
                        `id`, `starterId`, `status`, `scale`, `timestampEpochMs`,
                        `levainNotes`, `outcomeRating`, `photoPath`, `note`
                    )
                    SELECT `id`, `starterId`, 'FINISHED', 1.0, `timestampEpochMs`,
                           `levainNotes`, `outcomeRating`, `photoPath`, `note`
                    FROM `bake`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `bake`")
                db.execSQL("ALTER TABLE `bake_new` RENAME TO `bake`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bake_starterId` ON `bake` (`starterId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bake_step` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bakeId` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `instruction` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `cue` TEXT,
                        `plannedDurationMinutes` INTEGER,
                        `dueAtEpochMs` INTEGER,
                        `completedAtEpochMs` INTEGER,
                        FOREIGN KEY(`bakeId`) REFERENCES `bake`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bake_step_bakeId` ON `bake_step` (`bakeId`)")
            }
        }

        fun build(context: Context): LevainDatabase =
            Room.databaseBuilder(context, LevainDatabase::class.java, "levain.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
