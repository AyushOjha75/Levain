package com.ayushojha.levain.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Starter::class, Feeding::class, HealthObservation::class, Bake::class],
    version = 3,
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

        fun build(context: Context): LevainDatabase =
            Room.databaseBuilder(context, LevainDatabase::class.java, "levain.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
