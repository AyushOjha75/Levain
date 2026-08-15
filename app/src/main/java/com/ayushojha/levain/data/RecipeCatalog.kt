package com.ayushojha.levain.data

import android.content.res.AssetManager
import androidx.room.withTransaction
import org.json.JSONObject

/**
 * Where recipe content comes from. Assets in the app, plain strings in tests —
 * so the parser and the seeding rules are testable without a device.
 */
interface RecipeSource {
    fun fileNames(): List<String>
    fun read(fileName: String): String
}

class AssetRecipeSource(private val assets: AssetManager) : RecipeSource {
    override fun fileNames(): List<String> =
        assets.list(DIR).orEmpty().filter { it.endsWith(".json") }.map { "$DIR/$it" }

    override fun read(fileName: String): String =
        assets.open(fileName).bufferedReader().use { it.readText() }

    private companion object { const val DIR = "recipes" }
}

data class ParsedRecipe(
    val recipe: Recipe,
    val ingredients: List<RecipeIngredient>,
    val steps: List<RecipeStepTemplate>,
)

/**
 * Loads bundled recipes into Room.
 *
 * Recipes are content, not user data: assets are the source of truth and Room
 * is the queryable copy. A recipe is re-seeded whenever its `contentVersion`
 * differs from what's stored, so correcting a fermentation time is a patch
 * release rather than a database migration — and no external "have I seeded
 * yet" flag has to be kept honest.
 */
class RecipeCatalog(
    private val db: LevainDatabase,
    private val source: RecipeSource,
) {

    private val dao: LevainDao get() = db.levainDao()

    /** Returns how many recipes were written. Zero means everything was current. */
    suspend fun seed(): Int {
        var written = 0
        source.fileNames().forEach { file ->
            val parsed = parse(source.read(file))
            val existing = dao.getRecipe(parsed.recipe.id)
            if (existing == null || existing.contentVersion != parsed.recipe.contentVersion) {
                db.withTransaction {
                    // Children are replaced wholesale — a revised recipe may have
                    // fewer steps than it used to, and leftovers would be invisible.
                    dao.deleteIngredientsFor(parsed.recipe.id)
                    dao.deleteStepTemplatesFor(parsed.recipe.id)
                    dao.upsertRecipe(parsed.recipe)
                    dao.upsertIngredients(parsed.ingredients)
                    dao.upsertStepTemplates(parsed.steps)
                }
                written++
            }
        }
        return written
    }

    companion object {

        fun parse(json: String): ParsedRecipe {
            val root = JSONObject(json)
            val id = root.getString("id")

            val recipe = Recipe(
                id = id,
                name = root.getString("name"),
                summary = root.getString("summary"),
                breadType = root.getString("breadType"),
                requiresStarter = root.getBoolean("requiresStarter"),
                referenceBatch = root.getString("referenceBatch"),
                contentVersion = root.getInt("contentVersion"),
            )

            val ingredientsJson = root.getJSONArray("ingredients")
            val ingredients = (0 until ingredientsJson.length()).map { i ->
                val o = ingredientsJson.getJSONObject(i)
                RecipeIngredient(
                    recipeId = id,
                    position = i,
                    name = o.getString("name"),
                    grams = o.getDouble("grams"),
                    bakersPercent = if (o.has("bakersPercent")) o.getDouble("bakersPercent") else null,
                    phase = o.getString("phase"),
                )
            }

            val stepsJson = root.getJSONArray("steps")
            val steps = (0 until stepsJson.length()).map { i ->
                val o = stepsJson.getJSONObject(i)
                val title = o.getString("title")
                val kind = StepKind.valueOf(o.getString("kind"))
                val duration = if (o.has("durationMinutes")) o.getInt("durationMinutes") else null
                val cue = o.optString("cue", "").ifBlank { null }

                // Content that breaks the Step contract must fail here, loudly,
                // rather than reaching a baker as a timer that never ends or a
                // judged step with nothing to look for.
                when (kind) {
                    StepKind.JUDGED -> require(cue != null) {
                        "$id / \"$title\": a JUDGED step needs an observable cue"
                    }
                    StepKind.TIMED -> require(duration != null) {
                        "$id / \"$title\": a TIMED step needs a duration"
                    }
                    StepKind.ACTION -> require(duration == null) {
                        "$id / \"$title\": an ACTION step is instantaneous and must not carry a duration"
                    }
                }

                RecipeStepTemplate(
                    recipeId = id,
                    position = i,
                    title = title,
                    instruction = o.getString("instruction"),
                    kind = kind,
                    durationMinutes = duration,
                    cue = cue,
                    repeatCount = o.optInt("repeatCount", 1),
                    repeatEveryMinutes = if (o.has("repeatEveryMinutes")) o.getInt("repeatEveryMinutes") else null,
                    phase = o.getString("phase"),
                )
            }

            require(steps.isNotEmpty()) { "$id: a recipe with no steps is not a recipe" }
            return ParsedRecipe(recipe, ingredients, steps)
        }
    }
}
