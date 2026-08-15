package com.ayushojha.levain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ayushojha.levain.data.AssetRecipeSource
import com.ayushojha.levain.data.RecipeCatalog
import com.ayushojha.levain.data.RecipeSource
import com.ayushojha.levain.data.StepKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * These run against the **real bundled JSON**, not fixtures — the point is to
 * catch content mistakes, and content is exactly the thing a fixture would hide.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RecipeCatalogTest {

    private lateinit var app: TestApp

    @Before
    fun setUp() {
        app = TestApp()
    }

    @After
    fun tearDown() {
        app.close()
    }

    private fun bundled(): RecipeCatalog {
        val assets = ApplicationProvider.getApplicationContext<Context>().assets
        return RecipeCatalog(app.db, AssetRecipeSource(assets))
    }

    @Test
    fun `the three bundled recipes load on a fresh install`() = runTest {
        assertEquals(3, bundled().seed())

        val dao = app.db.levainDao()
        val loaf = dao.getRecipe("sourdough-country")!!
        val sourdoughFocaccia = dao.getRecipe("focaccia-sourdough")!!
        val yeastedFocaccia = dao.getRecipe("focaccia-yeasted")!!

        assertTrue(loaf.requiresStarter)
        assertTrue(sourdoughFocaccia.requiresStarter)
        // The whole point of this one: a bread with no Starter anywhere in it.
        assertFalse(yeastedFocaccia.requiresStarter)

        assertTrue(dao.getStepTemplates(loaf.id).size > 10)
        assertTrue(dao.getIngredients(loaf.id).any { it.name.contains("Ripe levain") })
    }

    @Test
    fun `every judged step carries a cue and every timed step a duration`() = runTest {
        bundled().seed()
        val dao = app.db.levainDao()

        listOf("sourdough-country", "focaccia-sourdough", "focaccia-yeasted").forEach { id ->
            dao.getStepTemplates(id).forEach { step ->
                when (step.kind) {
                    StepKind.JUDGED -> assertTrue(
                        "$id / ${step.title} is judged but has no cue",
                        !step.cue.isNullOrBlank(),
                    )
                    StepKind.TIMED -> assertTrue(
                        "$id / ${step.title} is timed but has no duration",
                        step.durationMinutes != null,
                    )
                    StepKind.ACTION -> assertTrue(
                        "$id / ${step.title} is an action but carries a duration",
                        step.durationMinutes == null,
                    )
                }
            }
        }
    }

    @Test
    fun `the yeasted route has no levain build at all`() = runTest {
        bundled().seed()
        val steps = app.db.levainDao().getStepTemplates("focaccia-yeasted")
        assertTrue(steps.none { it.phase == "levain" })
        assertTrue(app.db.levainDao().getStepTemplates("focaccia-sourdough").any { it.phase == "levain" })
    }

    @Test
    fun `both focaccias warn against baking from cold and against the hollow tap`() = runTest {
        bundled().seed()
        val dao = app.db.levainDao()

        listOf("focaccia-sourdough", "focaccia-yeasted").forEach { id ->
            val steps = dao.getStepTemplates(id)
            // Traps that fail silently: focaccia does not bake from cold...
            assertTrue(
                "$id never warns about baking from cold",
                steps.any { it.instruction.contains("room temperature", ignoreCase = true) },
            )
            // ...and the hollow-tap test is a loaf test, not a focaccia test.
            assertTrue(
                "$id never warns the hollow tap is invalid",
                steps.any { it.cue?.contains("hollow", ignoreCase = true) == true },
            )
        }
        // The loaf, by contrast, is supposed to use the tap.
        assertTrue(
            dao.getStepTemplates("sourdough-country")
                .any { it.cue?.contains("Hollow when tapped") == true },
        )
    }

    @Test
    fun `bumping a content version re-seeds without duplicating steps`() = runTest {
        val v1 = recipeJson(version = 1, stepTitle = "Fold the dough")
        val source = MutableSource(v1)
        val catalog = RecipeCatalog(app.db, source)

        assertEquals(1, catalog.seed())
        // Running again with nothing changed writes nothing.
        assertEquals(0, catalog.seed())

        source.content = recipeJson(version = 2, stepTitle = "Stretch and fold")
        assertEquals(1, catalog.seed())

        val steps = app.db.levainDao().getStepTemplates("test-bread")
        assertEquals(1, steps.size)
        assertEquals("Stretch and fold", steps.single().title)
        assertEquals(2, app.db.levainDao().getRecipe("test-bread")!!.contentVersion)
    }

    @Test
    fun `a judged step with no cue is rejected at parse time`() {
        val broken = """
            {"id":"broken","name":"Broken","summary":"s","breadType":"sourdough",
             "requiresStarter":true,"referenceBatch":"1","contentVersion":1,
             "ingredients":[],
             "steps":[{"title":"Bulk","instruction":"wait","kind":"JUDGED","phase":"bulk"}]}
        """.trimIndent()

        val error = runCatching { RecipeCatalog.parse(broken) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(error!!.message!!.contains("observable cue"))
    }

    private class MutableSource(var content: String) : RecipeSource {
        override fun fileNames() = listOf("recipes/test.json")
        override fun read(fileName: String) = content
    }

    private fun recipeJson(version: Int, stepTitle: String) = """
        {"id":"test-bread","name":"Test bread","summary":"s","breadType":"sourdough",
         "requiresStarter":true,"referenceBatch":"1 loaf","contentVersion":$version,
         "ingredients":[{"name":"Flour","grams":500,"bakersPercent":100,"phase":"dough"}],
         "steps":[{"title":"$stepTitle","instruction":"do it","kind":"TIMED",
                   "durationMinutes":30,"phase":"bulk"}]}
    """.trimIndent()
}
