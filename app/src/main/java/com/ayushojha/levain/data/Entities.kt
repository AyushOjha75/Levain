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
    /**
     * Non-null while this Starter is inside the create-a-starter 7-day
     * program; the current program day derives from this instant.
     */
    val programStartedAtEpochMs: Long? = null,
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
    /**
     * The feeding interval (hours) in force when this feeding was logged.
     * Streaks judge each historical gap against its own era, so moving a
     * starter between counter and fridge never rewrites its history.
     */
    val intervalHoursAtFeeding: Int? = null,
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

enum class BakeStatus { PLANNED, ACTIVE, HELD, FINISHED, ABANDONED }

enum class StepKind {
    /** A real duration with a real timer. */
    TIMED,

    /** The baker decides, guided by an estimate and an observable cue. */
    JUDGED,

    /** Done the moment you do it. Never given a fake duration. */
    ACTION,
}

/**
 * One run of a Recipe — live while it runs, and the history entry afterwards.
 * Status is the only thing that separates the two.
 *
 * `starterId` is nullable because yeasted breads have no Starter, and deleting
 * a Starter orphans its Bakes (SET NULL) rather than erasing the loaves it made.
 */
@Entity(
    tableName = "bake",
    foreignKeys = [ForeignKey(
        entity = Starter::class,
        parentColumns = ["id"],
        childColumns = ["starterId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("starterId")],
)
data class Bake(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val starterId: Long? = null,
    /** Which Recipe this came from, purely as provenance — Steps are snapshotted. */
    val recipeId: String? = null,
    val recipeContentVersion: Int? = null,
    val status: BakeStatus = BakeStatus.FINISHED,
    /** Multiple of the Recipe's reference batch. */
    val scale: Double = 1.0,
    val timestampEpochMs: Long,
    val startedAtEpochMs: Long? = null,
    /** Set while the Bake is HELD — a deliberate pause stops the Projection. */
    val heldAtEpochMs: Long? = null,
    /** Levain build notes, for bakes logged without a guided run. */
    val levainNotes: String? = null,
    /** 1..5, null until the bake is finished and rated. */
    val outcomeRating: Int? = null,
    val photoPath: String? = null,
    val note: String? = null,
)

/** The reusable plan for one bread. Bundled content, parsed from assets. */
@Entity(tableName = "recipe")
data class Recipe(
    /** Stable slug, e.g. "sourdough-country". */
    @PrimaryKey val id: String,
    val name: String,
    val summary: String,
    val breadType: String,
    val requiresStarter: Boolean,
    /** What the declared quantities make: "1 loaf", "one 9x13 pan". */
    val referenceBatch: String,
    val contentVersion: Int,
)

@Entity(
    tableName = "recipe_ingredient",
    foreignKeys = [ForeignKey(
        entity = Recipe::class,
        parentColumns = ["id"],
        childColumns = ["recipeId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("recipeId")],
)
data class RecipeIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: String,
    val position: Int,
    val name: String,
    val grams: Double,
    val bakersPercent: Double? = null,
    /** Which part of the bake consumes it: levain, dough, topping. */
    val phase: String,
)

/**
 * A Step as the Recipe declares it — the compact form, where repeats are still
 * a count. Expanded into [BakeStep]s when a Bake starts.
 */
@Entity(
    tableName = "recipe_step",
    foreignKeys = [ForeignKey(
        entity = Recipe::class,
        parentColumns = ["id"],
        childColumns = ["recipeId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("recipeId")],
)
data class RecipeStepTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: String,
    val position: Int,
    val title: String,
    val instruction: String,
    val kind: StepKind,
    /** TIMED: the timer. JUDGED: the estimate. ACTION: null. */
    val durationMinutes: Int? = null,
    /** JUDGED only — what the baker looks for. */
    val cue: String? = null,
    val repeatCount: Int = 1,
    val repeatEveryMinutes: Int? = null,
    val phase: String,
)

/**
 * A Step belonging to one Bake, snapshotted at start so revising bundled
 * content can never rewrite a bake in progress or in history.
 */
@Entity(
    tableName = "bake_step",
    foreignKeys = [ForeignKey(
        entity = Bake::class,
        parentColumns = ["id"],
        childColumns = ["bakeId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("bakeId")],
)
data class BakeStep(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bakeId: Long,
    val position: Int,
    val title: String,
    val instruction: String,
    val kind: StepKind,
    val cue: String? = null,
    val plannedDurationMinutes: Int? = null,
    /** The Projection: when this step comes due, recalculated as steps land. */
    val dueAtEpochMs: Long? = null,
    val completedAtEpochMs: Long? = null,
)
