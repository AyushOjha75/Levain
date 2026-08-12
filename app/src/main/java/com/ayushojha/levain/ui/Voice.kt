package com.ayushojha.levain.ui

import com.ayushojha.levain.domain.Mood

/** The app's voice: one warm line per mood, spoken on the starter's card. */
fun moodLine(name: String, mood: Mood): String = when (mood) {
    Mood.HUNGRY -> "$name is hungry!"
    Mood.SLEEPY -> "$name is feeling sluggish…"
    Mood.BEAMING -> "$name is thriving!"
    Mood.RESTING -> "$name is napping in the fridge"
    Mood.RETIRED -> "$name lives on in memory"
    Mood.CONTENT -> "$name is doing just fine"
}
