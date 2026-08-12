package com.ayushojha.levain.domain

import com.ayushojha.levain.data.RiseRating
import com.ayushojha.levain.data.Smell

/**
 * Contextual micro-education: one short truth attached to the exact moment
 * the user records something. Bundled content — fact-checked by the baker.
 */
object Tips {

    val smellTips: Map<Smell, String> = mapOf(
        Smell.MILD to "A mild smell means a young or freshly-fed culture — flavour builds as it ripens.",
        Smell.YEASTY to "Yeasty like bread dough: the yeast side of the culture is dominant. Great for oven spring.",
        Smell.TANGY to "Pleasantly tangy is the sweet spot — a balanced yeast/bacteria culture at its prime.",
        Smell.SOUR to "Strongly sour means lots of lactic acid — ripe, and it'll flavour your bread accordingly.",
        Smell.ACETONE to "Nail polish smell = it's starving. It burned through its food; feed sooner or at a stronger ratio.",
        Smell.ALCOHOLIC to "Boozy smell (or grey liquid on top) is 'hooch' — harmless, but a clear sign it wants more frequent feeds.",
        Smell.OFF to "Genuinely off — cheesy, putrid? Check for fuzzy mold. When in doubt with visible mold, start fresh.",
    )

    val riseTips: Map<RiseRating, String> = mapOf(
        RiseRating.PEAKED to "Caught at peak! This is the moment it's strongest — ideal for building a levain.",
        RiseRating.RISING to "Still climbing — check back in an hour or two if you're planning to bake.",
        RiseRating.SLUGGISH to "Slow rise? Usually cold or underfed. Warmth (24–27°C) fixes more starters than anything else.",
        RiseRating.FLAT to "No rise at all: if it's new, keep going — if established, try warm spot + 1:2:2 feeds twice daily.",
    )

    /** Rotating dashboard facts — one per day. */
    val facts = listOf(
        "Sourdough is the oldest form of leavened bread — Egyptians baked it 4,500 years ago.",
        "A starter is a stable culture of wild yeast and lactobacilli — over 50 species have been found in starters worldwide.",
        "The sour in sourdough comes from lactic and acetic acid, not the yeast.",
        "Flour is alive: 1g holds up to 13,000 wild yeast cells before you add anything.",
        "Cold slows acid production more than yeast activity — fridge-proofed dough gets more complex, not more sour.",
        "The famous 1849 Gold Rush 'sourdoughs' slept with their starters to keep them warm.",
        "A well-kept starter is effectively immortal — some bakeries feed cultures older than a century.",
        "Whole rye flour ferments fastest: more enzymes, more minerals, more wild microbes.",
        "The float test: a spoonful of ripe starter floats in water because it's full of gas.",
        "Baker's percentages express everything relative to flour = 100%, so recipes scale perfectly.",
        "Higher hydration = more open crumb, but also less structure — 75% is a friendly middle ground.",
        "Salt slows fermentation by about 30% at 2% of flour weight — it's a brake, not just seasoning.",
        "Levain and starter aren't the same: the levain is the offshoot you build for one bake and use up.",
        "Steam in the first 15 minutes is what makes crust crackle — that's why Dutch ovens work so well.",
    )

    fun factOfTheDay(epochDay: Long): String = facts[(epochDay % facts.size).toInt()]
}
