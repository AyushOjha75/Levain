package com.ayushojha.levain.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LifecycleState { ACTIVE, DORMANT, ARCHIVED }

enum class RiseRating { PEAKED, RISING, SLUGGISH, FLAT }

enum class Smell { MILD, YEASTY, TANGY, SOUR, ACETONE, ALCOHOLIC, OFF }

@Entity(tableName = "starter")
data class Starter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val state: LifecycleState = LifecycleState.ACTIVE,
    /** Feeding interval while ACTIVE, in hours. */
    val activeIntervalHours: Int = 24,
    /** Feeding interval while DORMANT, in hours. */
    val dormantIntervalHours: Int = 168,
    val createdAtEpochMs: Long,
    /**
     * The dueAt instant this starter was last notified for. Guards "fire once
     * per due event": a new notification is emitted only when the computed
     * dueAt differs from this value.
     */
    val lastNotifiedDueAtEpochMs: Long? = null,
)

@Entity(
    tableName = "feeding",
    foreignKeys = [ForeignKey(
        entity = Starter::class,
        parentColumns = ["id"],
        childColumns = ["starterId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("starterId")],
)
data class Feeding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val starterId: Long,
    val timestampEpochMs: Long,
    /** starter:flour:water, e.g. "1:5:5". */
    val ratio: String,
    val flourType: String,
)

@Entity(
    tableName = "health_observation",
    foreignKeys = [ForeignKey(
        entity = Starter::class,
        parentColumns = ["id"],
        childColumns = ["starterId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("starterId")],
)
data class HealthObservation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val starterId: Long,
    val timestampEpochMs: Long,
    val riseRating: RiseRating,
    val timeToPeakMinutes: Int? = null,
    val smell: Smell? = null,
    /** Path relative to the app-private photos directory. */
    val photoPath: String? = null,
    val note: String? = null,
)

@Entity(
    tableName = "bake",
    foreignKeys = [ForeignKey(
        entity = Starter::class,
        parentColumns = ["id"],
        childColumns = ["starterId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("starterId")],
)
data class Bake(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val starterId: Long,
    val timestampEpochMs: Long,
    /** Levain (bake build) folded into the Bake as notes — not a separate entity. */
    val levainNotes: String? = null,
    /** 1..5. */
    val outcomeRating: Int,
    val photoPath: String? = null,
    val note: String? = null,
)
