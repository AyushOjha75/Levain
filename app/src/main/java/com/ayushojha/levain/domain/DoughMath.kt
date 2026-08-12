package com.ayushojha.levain.domain

import kotlin.math.roundToInt

data class LevainBuild(
    val seedGrams: Int,
    val flourGrams: Int,
    val waterGrams: Int,
)

data class BakersPercentages(
    val hydrationPct: Int,
    val saltPct: Double,
    val levainPct: Int,
)

/** Bakers' arithmetic. Pure functions — the calculators' single source of truth. */
object DoughMath {

    /**
     * How to build [targetGrams] of levain at [hydrationPct] (water/flour)
     * using [inoculationPct] seed starter (seed/flour).
     */
    fun levainBuild(targetGrams: Int, hydrationPct: Int, inoculationPct: Int): LevainBuild {
        val h = hydrationPct / 100.0
        val s = inoculationPct / 100.0
        val flour = targetGrams / (1 + h + s)
        return LevainBuild(
            seedGrams = (flour * s).roundToInt(),
            flourGrams = flour.roundToInt(),
            waterGrams = (flour * h).roundToInt(),
        )
    }

    /**
     * Baker's percentages for a dough. The levain is assumed 100% hydration,
     * so its flour and water halves fold into total hydration.
     */
    fun bakersPercentages(flourGrams: Int, waterGrams: Int, saltGrams: Double, levainGrams: Int): BakersPercentages {
        val levainFlour = levainGrams / 2.0
        val levainWater = levainGrams / 2.0
        val totalFlour = flourGrams + levainFlour
        val totalWater = waterGrams + levainWater
        return BakersPercentages(
            hydrationPct = if (totalFlour > 0) (totalWater / totalFlour * 100).roundToInt() else 0,
            saltPct = if (flourGrams > 0) ((saltGrams / flourGrams * 1000).roundToInt() / 10.0) else 0.0,
            levainPct = if (flourGrams > 0) (levainGrams.toDouble() / flourGrams * 100).roundToInt() else 0,
        )
    }
}
