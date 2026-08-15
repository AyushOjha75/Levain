package com.ayushojha.levain.ui.avatar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ayushojha.levain.domain.Mood
import com.ayushojha.levain.ui.theme.Palette

// The jar's palette comes from the design system like everything else — the
// Avatar is the Starter section's motif, not a private colour world.
private val JarGlass = Palette.JarGlass
private val JarGlassDark = Palette.JarGlassNight
private val DoughLight = Palette.Dough
private val DoughShade = Palette.DoughShade
private val DoughRetired = Palette.DoughRetired
private val LidBrown = Palette.Crust
private val LidRetired = Palette.LidRetired
private val FaceInk = Palette.FaceInk
private val CheekPink = Palette.Cheek

/**
 * The Starter's Avatar: a parametric jar character, drawn in code so moods,
 * states, and animation cost nothing to vary. The mood is derived truth —
 * this composable just wears it.
 */
@Composable
fun StarterAvatar(
    mood: Mood,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "avatar")

    // Dough breathes; retired dough doesn't.
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (mood == Mood.RETIRED) 0f else 1f,
        animationSpec = infiniteRepeatable(
            tween(if (mood == Mood.SLEEPY || mood == Mood.RESTING) 3200 else 1800),
            RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    // Bubbles drift up the jar on a loop; lively moods bubble more.
    val bubblePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "bubbles",
    )

    // Awake moods blink; sleepy/resting/retired eyes stay closed anyway.
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3800
                1f at 0
                1f at 3500
                0.08f at 3600
                1f at 3700
            }
        ),
        label = "blink",
    )

    Canvas(modifier = modifier) {
        val jar = Rect(
            left = size.width * 0.14f,
            top = size.height * 0.18f,
            right = size.width * 0.86f,
            bottom = size.height * 0.96f,
        )

        // Jar glass
        drawRoundRect(
            color = if (darkTheme) JarGlassDark else JarGlass,
            topLeft = jar.topLeft,
            size = jar.size,
            cornerRadius = CornerRadius(size.width * 0.12f),
        )

        // Dough level: breathes up and down a little
        val doughTopBase = jar.top + jar.height * 0.35f
        val doughTop = doughTopBase - jar.height * 0.04f * breathe
        val dough = Rect(jar.left, doughTop, jar.right, jar.bottom)
        drawRoundRect(
            color = if (mood == Mood.RETIRED) DoughRetired else DoughLight,
            topLeft = dough.topLeft,
            size = dough.size,
            cornerRadius = CornerRadius(size.width * 0.12f),
        )
        // Shading band at the dough surface
        drawRoundRect(
            color = if (mood == Mood.RETIRED) DoughRetired else DoughShade,
            topLeft = Offset(dough.left, dough.top),
            size = Size(dough.width, jar.height * 0.05f),
            cornerRadius = CornerRadius(size.width * 0.1f),
        )

        // Lid
        drawRoundRect(
            color = if (mood == Mood.RETIRED) LidRetired else LidBrown,
            topLeft = Offset(jar.left - size.width * 0.04f, size.height * 0.08f),
            size = Size(jar.width + size.width * 0.08f, size.height * 0.12f),
            cornerRadius = CornerRadius(size.width * 0.06f),
        )

        // Bubbles (skip when retired — nothing's alive in there)
        if (mood != Mood.RETIRED) {
            val bubbleCount = when (mood) {
                Mood.BEAMING -> 5
                Mood.CONTENT, Mood.HUNGRY -> 3
                else -> 1
            }
            repeat(bubbleCount) { i ->
                val lane = 0.25f + 0.5f * (i.toFloat() / maxOf(1, bubbleCount - 1))
                val phase = (bubblePhase + i * 0.37f) % 1f
                val y = dough.bottom - (dough.height * 0.85f) * phase
                if (y > dough.top + jar.height * 0.08f) {
                    drawCircle(
                        color = DoughShade,
                        radius = size.width * (0.02f + 0.012f * (i % 3)),
                        center = Offset(jar.left + jar.width * lane, y),
                    )
                }
            }
        }

        // Face sits on the dough
        val faceCenterY = dough.top + jar.height * 0.18f
        val eyeDx = jar.width * 0.16f
        val eyeY = faceCenterY
        val eyeR = size.width * 0.035f
        val leftEye = Offset(jar.center.x - eyeDx, eyeY)
        val rightEye = Offset(jar.center.x + eyeDx, eyeY)

        when (mood) {
            Mood.SLEEPY, Mood.RESTING, Mood.RETIRED -> {
                drawClosedEye(leftEye, eyeR)
                drawClosedEye(rightEye, eyeR)
            }
            else -> {
                // blink squashes the eyes vertically
                drawOpenEye(leftEye, eyeR, blink)
                drawOpenEye(rightEye, eyeR, blink)
            }
        }

        // Cheeks for the happy moods
        if (mood == Mood.BEAMING || mood == Mood.CONTENT) {
            drawCircle(CheekPink, eyeR * 1.1f, Offset(leftEye.x - eyeR * 2.2f, eyeY + eyeR * 1.6f))
            drawCircle(CheekPink, eyeR * 1.1f, Offset(rightEye.x + eyeR * 2.2f, eyeY + eyeR * 1.6f))
        }

        // Mouth
        val mouthY = faceCenterY + jar.height * 0.1f
        val mouthW = jar.width * 0.22f
        when (mood) {
            Mood.BEAMING -> drawSmile(jar.center.x, mouthY, mouthW, open = true)
            Mood.CONTENT -> drawSmile(jar.center.x, mouthY, mouthW, open = false)
            Mood.HUNGRY -> drawCircle(
                color = FaceInk,
                radius = mouthW * 0.32f,
                center = Offset(jar.center.x, mouthY),
            )
            Mood.SLEEPY, Mood.RESTING -> drawLine(
                color = FaceInk,
                start = Offset(jar.center.x - mouthW * 0.3f, mouthY),
                end = Offset(jar.center.x + mouthW * 0.3f, mouthY),
                strokeWidth = size.width * 0.018f,
            )
            Mood.RETIRED -> drawSmile(jar.center.x, mouthY, mouthW * 0.8f, open = false)
        }
    }
}

private fun DrawScope.drawOpenEye(center: Offset, radius: Float, blink: Float) {
    drawOval(
        color = FaceInk,
        topLeft = Offset(center.x - radius, center.y - radius * blink),
        size = Size(radius * 2f, radius * 2f * blink),
    )
}

private fun DrawScope.drawClosedEye(center: Offset, radius: Float) {
    val path = Path().apply {
        moveTo(center.x - radius, center.y)
        quadraticTo(center.x, center.y + radius, center.x + radius, center.y)
    }
    drawPath(path, FaceInk, style = Stroke(width = radius * 0.5f))
}

private fun DrawScope.drawSmile(cx: Float, cy: Float, width: Float, open: Boolean) {
    val path = Path().apply {
        moveTo(cx - width / 2f, cy)
        quadraticTo(cx, cy + width * 0.55f, cx + width / 2f, cy)
        if (open) close()
    }
    if (open) {
        drawPath(path, FaceInk)
    } else {
        drawPath(path, FaceInk, style = Stroke(width = width * 0.12f))
    }
}
