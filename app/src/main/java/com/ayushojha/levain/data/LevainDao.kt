package com.ayushojha.levain.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LevainDao {

    // --- Starters ---

    @Insert
    suspend fun insertStarter(starter: Starter): Long

    @Update
    suspend fun updateStarter(starter: Starter)

    @Query("SELECT * FROM starter ORDER BY name")
    fun observeStarters(): Flow<List<Starter>>

    @Query("SELECT * FROM starter WHERE id = :id")
    fun observeStarter(id: Long): Flow<Starter?>

    @Query("SELECT * FROM starter")
    suspend fun getStarters(): List<Starter>

    @Query("SELECT * FROM starter WHERE id = :id")
    suspend fun getStarter(id: Long): Starter?

    @Query("DELETE FROM starter WHERE id = :id")
    suspend fun deleteStarter(id: Long)

    // --- Feedings ---

    @Insert
    suspend fun insertFeeding(feeding: Feeding): Long

    @Update
    suspend fun updateFeeding(feeding: Feeding)

    @Query("SELECT * FROM feeding WHERE id = :id")
    suspend fun getFeeding(id: Long): Feeding?

    @Delete
    suspend fun deleteFeeding(feeding: Feeding)

    @Query("SELECT * FROM feeding WHERE starterId = :starterId ORDER BY timestampEpochMs DESC")
    fun observeFeedings(starterId: Long): Flow<List<Feeding>>

    @Query("SELECT * FROM feeding WHERE starterId = :starterId ORDER BY timestampEpochMs DESC LIMIT 1")
    suspend fun getLastFeeding(starterId: Long): Feeding?

    @Query("SELECT * FROM feeding")
    fun observeAllFeedings(): Flow<List<Feeding>>

    @Query(
        "SELECT * FROM feeding WHERE id IN " +
            "(SELECT id FROM feeding AS f WHERE f.timestampEpochMs = " +
            "(SELECT MAX(timestampEpochMs) FROM feeding WHERE starterId = f.starterId))"
    )
    fun observeLastFeedings(): Flow<List<Feeding>>

    // --- Health observations ---

    @Insert
    suspend fun insertObservation(observation: HealthObservation): Long

    @Delete
    suspend fun deleteObservation(observation: HealthObservation)

    @Query("SELECT * FROM health_observation WHERE starterId = :starterId ORDER BY timestampEpochMs DESC")
    fun observeObservations(starterId: Long): Flow<List<HealthObservation>>

    @Query(
        "SELECT * FROM health_observation WHERE id IN " +
            "(SELECT id FROM health_observation AS o WHERE o.timestampEpochMs = " +
            "(SELECT MAX(timestampEpochMs) FROM health_observation WHERE starterId = o.starterId))"
    )
    fun observeLastObservations(): Flow<List<HealthObservation>>

    // --- Bakes ---

    @Insert
    suspend fun insertBake(bake: Bake): Long

    @Delete
    suspend fun deleteBake(bake: Bake)

    @Query("SELECT * FROM bake WHERE starterId = :starterId ORDER BY timestampEpochMs DESC")
    fun observeBakes(starterId: Long): Flow<List<Bake>>

    // --- Recipes (bundled content, re-seeded from assets, never user data) ---

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsertRecipe(recipe: Recipe)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsertIngredients(ingredients: List<RecipeIngredient>)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsertStepTemplates(steps: List<RecipeStepTemplate>)

    @Query("SELECT * FROM recipe ORDER BY name")
    fun observeRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipe WHERE id = :id")
    suspend fun getRecipe(id: String): Recipe?

    @Query("SELECT * FROM recipe_ingredient WHERE recipeId = :recipeId ORDER BY position")
    suspend fun getIngredients(recipeId: String): List<RecipeIngredient>

    @Query("SELECT * FROM recipe_step WHERE recipeId = :recipeId ORDER BY position")
    suspend fun getStepTemplates(recipeId: String): List<RecipeStepTemplate>

    // --- Bake steps (snapshotted onto a Bake at start) ---

    @Insert
    suspend fun insertBakeSteps(steps: List<BakeStep>)

    @Update
    suspend fun updateBakeStep(step: BakeStep)

    @Query("SELECT * FROM bake_step WHERE bakeId = :bakeId ORDER BY position")
    fun observeBakeSteps(bakeId: Long): Flow<List<BakeStep>>

    @Query("SELECT * FROM bake_step WHERE bakeId = :bakeId ORDER BY position")
    suspend fun getBakeSteps(bakeId: Long): List<BakeStep>

    @Update
    suspend fun updateBake(bake: Bake)

    /** At most one Bake is live at a time; the app opens straight into it. */
    @Query("SELECT * FROM bake WHERE status IN ('ACTIVE','HELD') ORDER BY startedAtEpochMs DESC LIMIT 1")
    fun observeActiveBake(): Flow<Bake?>

    // --- Backup (full-table access + wipe for import) ---

    @Query("SELECT * FROM feeding")
    suspend fun getAllFeedings(): List<Feeding>

    @Query("SELECT * FROM health_observation")
    suspend fun getAllObservations(): List<HealthObservation>

    @Query("SELECT * FROM bake")
    suspend fun getAllBakes(): List<Bake>

    @Query("SELECT * FROM bake_step")
    suspend fun getAllBakeSteps(): List<BakeStep>

    @Query("DELETE FROM starter")
    suspend fun clearStarters()

    /**
     * Bakes no longer cascade from Starters (deleting a Starter orphans them),
     * so a full wipe has to clear them explicitly. Their steps cascade.
     */
    @Query("DELETE FROM bake")
    suspend fun clearBakes()
}
