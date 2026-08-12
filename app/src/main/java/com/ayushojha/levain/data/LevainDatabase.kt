package com.ayushojha.levain.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Starter::class, Feeding::class, HealthObservation::class, Bake::class],
    version = 1,
    exportSchema = false,
)
abstract class LevainDatabase : RoomDatabase() {

    abstract fun levainDao(): LevainDao

    companion object {
        fun build(context: Context): LevainDatabase =
            Room.databaseBuilder(context, LevainDatabase::class.java, "levain.db").build()
    }
}
