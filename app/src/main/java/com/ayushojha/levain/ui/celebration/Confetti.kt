package com.ayushojha.levain.ui.celebration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin
import kotlin.random.Random

private val ConfettiColors = com.ayushojha.levain.ui.theme.Palette.Confetti

private class Particle(seed: Random) {
    val x = seed.nextFloat()
    val drift = (seed.nextFloat() - 0.5f) * 0.3f
    val speed = 0.6f + seed.nextFloat() * 0.8f
    val size = 6f + seed.nextFloat() * 8f
    val color = ConfettiColors[seed.nextInt(ConfettiColors.size)]
    val spin = seed.nextFloat() * 720f - 360f
    val wobble = seed.nextFloat() * 6.28f
}

/**
 * A one-shot confetti burst for celebration moments (5-star bakes, milestones).
 * Runs for [durationMs] then calls [onFinished].
 */
@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    durationMs: Int = 1800,
    onFinished: () -> Unit = {},
) {
    val particles = remember { List(70) { Particle(Random(it)) } }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
        onFinished()
    }

    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        particles.forEach { p ->
            val y = (t * p.speed) % 1.2f
            if (y <= 1f) {
                val x = p.x + p.drift * t + 0.02f * sin(p.wobble + t * 12f)
                val alpha = (1f - t).coerceIn(0f, 1f) * 0.5f + 0.5f
                rotate(p.spin * t, pivot = Offset(x * size.width, y * size.height)) {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(x * size.width, y * size.height),
                        size = Size(p.size, p.size * 1.6f),
                    )
                }
            }
        }
    }
}
