package com.ayushojha.levain.data

import androidx.room.withTransaction
import com.ayushojha.levain.domain.BakePlanner
import com.ayushojha.levain.reminders.DueScheduler
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Runs a Bake: starting one from a Recipe, and every way a real bake drifts
 * from its plan.
 *
 * The framing decided for the app is *a checklist the baker drives, with the
 * app keeping the schedule honest* — so every method here adjusts the
 * Projection, and none of them decides anything on the baker's behalf.
 *
 * There is deliberately **no foreground service**: Room is the source of truth
 * and exactly one alarm is armed at a time — the next prompt — re-armed after
 * every mutation. That is what survives process death, reboot and an OEM
 * battery manager, none of which a long-running service would.
 */
class BakeSessions(
    private val db: LevainDatabase,
    private val scheduler: DueScheduler,
    private val clock: Clock,
) {

    private val dao: LevainDao get() = db.levainDao()

    fun observeActive(): Flow<Bake?> = dao.observeActiveBake()
    fun observeSteps(bakeId: Long): Flow<List<BakeStep>> = dao.observeBakeSteps(bakeId)
    fun observeRecipes(): Flow<List<Recipe>> = dao.observeRecipes()

    suspend fun getBake(id: Long): Bake? = dao.getBake(id)

    /** Snapshot the recipe onto a new Bake and start the clock. */
    suspend fun start(recipeId: String, scale: Double = 1.0, starterId: Long? = null): Long {
        val recipe = requireNotNull(dao.getRecipe(recipeId)) { "No such recipe: $recipeId" }
        val templates = dao.getStepTemplates(recipeId)
        val now = clock.instant()

        val bakeId = db.withTransaction {
            val id = dao.insertBake(
                Bake(
                    starterId = starterId,
                    recipeId = recipe.id,
                    recipeContentVersion = recipe.contentVersion,
                    status = BakeStatus.ACTIVE,
                    scale = scale,
                    timestampEpochMs = now.toEpochMilli(),
                    startedAtEpochMs = now.toEpochMilli(),
                )
            )
            dao.insertBakeSteps(BakePlanner.project(BakePlanner.expand(templates, id), now))
            id
        }
        armNextPrompt(bakeId)
        return bakeId
    }

    /** Tick a step off — early, on time or late; the rest of the plan moves. */
    suspend fun complete(stepId: Long, at: Instant = clock.instant()) {
        val step = dao.getBakeStep(stepId) ?: return
        dao.updateBakeStep(step.copy(completedAtEpochMs = at.toEpochMilli()))
        reproject(step.bakeId)
    }

    /** Undo a tick — a mis-tap mid-bake shouldn't cost you the run. */
    suspend fun uncomplete(stepId: Long) {
        val step = dao.getBakeStep(stepId) ?: return
        dao.updateBakeStep(step.copy(completedAtEpochMs = null))
        reproject(step.bakeId)
    }

    /** "It needs another fifteen minutes" — extends a step without completing it. */
    suspend fun extend(stepId: Long, by: Duration) {
        val step = dao.getBakeStep(stepId) ?: return
        val planned = (step.plannedDurationMinutes ?: 0) + by.toMinutes().toInt()
        dao.updateBakeStep(step.copy(plannedDurationMinutes = planned))
        reproject(step.bakeId)
    }

    /**
     * A Hold is a deliberate pause — into the fridge, or simply stopping. It is
     * not a step running long, so the Projection stops rather than counting the
     * pause as lateness, and the alarm is cancelled.
     */
    suspend fun hold(bakeId: Long) {
        val bake = dao.getBake(bakeId) ?: return
        dao.updateBake(bake.copy(status = BakeStatus.HELD, heldAtEpochMs = clock.instant().toEpochMilli()))
        scheduler.cancel()
    }

    /** Resuming re-projects the remaining steps from now, not from the pause. */
    suspend fun resume(bakeId: Long) {
        val bake = dao.getBake(bakeId) ?: return
        val now = clock.instant()
        dao.updateBake(bake.copy(status = BakeStatus.ACTIVE, heldAtEpochMs = null))
        BakePlanner.shiftIncompleteTo(dao.getBakeSteps(bakeId), now).forEach { dao.updateBakeStep(it) }
        armNextPrompt(bakeId)
    }

    /** Insert something the recipe never mentioned. Snapshots make this safe. */
    suspend fun insertStep(bakeId: Long, after: Int, title: String, instruction: String, kind: StepKind, durationMinutes: Int?) {
        val steps = dao.getBakeSteps(bakeId).toMutableList()
        val shifted = steps.map { if (it.position > after) it.copy(position = it.position + 1) else it }
        shifted.filter { it.position > after }.forEach { dao.updateBakeStep(it) }
        dao.insertBakeSteps(
            listOf(
                BakeStep(
                    bakeId = bakeId,
                    position = after + 1,
                    title = title,
                    instruction = instruction,
                    kind = kind,
                    plannedDurationMinutes = durationMinutes,
                )
            )
        )
        reproject(bakeId)
    }

    /** Finish the bake and rate it. */
    suspend fun finish(bakeId: Long, outcomeRating: Int, note: String? = null, photoPath: String? = null) {
        val bake = dao.getBake(bakeId) ?: return
        dao.updateBake(
            bake.copy(
                status = BakeStatus.FINISHED,
                outcomeRating = outcomeRating,
                note = note ?: bake.note,
                photoPath = photoPath ?: bake.photoPath,
                timestampEpochMs = clock.instant().toEpochMilli(),
            )
        )
        scheduler.cancel()
    }

    /** A failed bake is data — abandoning keeps it, it doesn't delete it. */
    suspend fun abandon(bakeId: Long) {
        val bake = dao.getBake(bakeId) ?: return
        dao.updateBake(bake.copy(status = BakeStatus.ABANDONED))
        scheduler.cancel()
    }

    private suspend fun reproject(bakeId: Long) {
        val bake = dao.getBake(bakeId) ?: return
        if (bake.status != BakeStatus.ACTIVE) return
        val startedAt = bake.startedAtEpochMs?.let(Instant::ofEpochMilli) ?: clock.instant()
        BakePlanner.project(dao.getBakeSteps(bakeId), startedAt).forEach { dao.updateBakeStep(it) }
        armNextPrompt(bakeId)
    }

    private suspend fun armNextPrompt(bakeId: Long) {
        val next = BakePlanner.nextPromptAt(dao.getBakeSteps(bakeId))
        if (next == null) scheduler.cancel() else scheduler.scheduleExact(maxOf(next, clock.instant()))
    }
}
