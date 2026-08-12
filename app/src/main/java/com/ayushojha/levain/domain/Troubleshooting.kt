package com.ayushojha.levain.domain

/**
 * The troubleshooting wizard's bundled decision tree:
 * symptom → (optional follow-up) → diagnosis + plan. Static content, no backend.
 */
sealed interface TroubleshootingNode {
    data class Question(
        val text: String,
        val options: List<Pair<String, TroubleshootingNode>>,
    ) : TroubleshootingNode

    data class Diagnosis(
        val title: String,
        val explanation: String,
        val plan: List<String>,
    ) : TroubleshootingNode
}

object TroubleshootingTree {

    private val hungryStarter = TroubleshootingNode.Diagnosis(
        title = "Your starter is hungry",
        explanation = "An acetone / nail-polish smell means the culture has burned through its food and is producing acetic byproducts.",
        plan = listOf(
            "Feed it now at 1:5:5 (starter:flour:water)",
            "Shorten the feeding interval — try feeding 4–6 hours earlier than usual",
            "If it's warm where you keep it, consider a cooler spot or the fridge",
        ),
    )

    private val alcoholLayer = TroubleshootingNode.Diagnosis(
        title = "Hooch — it's starving, not spoiled",
        explanation = "That grey liquid (hooch) is alcohol from a starving culture. Harmless, and a clear 'feed me more often' signal.",
        plan = listOf(
            "Pour off the hooch (or stir it in for extra tang)",
            "Feed at a stronger ratio like 1:5:5",
            "Feed more frequently or refrigerate between feeds",
        ),
    )

    private val newStarter = TroubleshootingNode.Diagnosis(
        title = "Normal early-days lull",
        explanation = "Days 2–5 of a new starter often go quiet after early bubbling — the initial bacteria die off before the real sourdough culture establishes.",
        plan = listOf(
            "Keep the daily discard-and-feed rhythm",
            "Keep it warm (24–27°C) — cold slows everything",
            "Give it 3–4 more days before worrying",
        ),
    )

    private val sluggishMature = TroubleshootingNode.Diagnosis(
        title = "Run-down culture — give it a spa week",
        explanation = "A previously-strong starter that's gone sluggish usually needs warmth and stronger feeding, not replacement.",
        plan = listOf(
            "Feed twice daily at 1:2:2 for 3 days, somewhere warm",
            "Switch part of the flour to whole rye — extra nutrients",
            "Make sure your water isn't heavily chlorinated (let it sit an hour, or use filtered)",
        ),
    )

    private val mold = TroubleshootingNode.Diagnosis(
        title = "Mold — this one's serious",
        explanation = "Fuzzy spots (white, blue, green, or pink/orange streaks) mean mold. Unlike hooch, mold is not safe to stir in.",
        plan = listOf(
            "If mold is on the surface or jar: discard the starter — it's not worth the risk",
            "If you have a recent healthy backup or dried flakes, restart from those",
            "Next time: cleaner jar, fresher flour, and don't let the surface dry out",
        ),
    )

    val root = TroubleshootingNode.Question(
        text = "What's wrong with your starter?",
        options = listOf(
            "It smells like nail polish / acetone" to hungryStarter,
            "There's grey liquid on top" to alcoholLayer,
            "It isn't rising" to TroubleshootingNode.Question(
                text = "How old is this starter?",
                options = listOf(
                    "Less than 2 weeks — I'm creating it" to newStarter,
                    "Established — it used to rise fine" to sluggishMature,
                ),
            ),
            "I see fuzzy spots or strange colours" to mold,
        ),
    )
}
