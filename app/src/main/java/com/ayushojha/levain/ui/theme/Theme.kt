package com.ayushojha.levain.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm, bready palette: crust browns, crumb creams, a sour-green accent.
private val Crust = Color(0xFF8B5E34)
private val CrustDeep = Color(0xFF5C3A1E)
private val Crumb = Color(0xFFFDF6EC)
private val CrumbDark = Color(0xFF1F1A14)
private val SurfaceDark = Color(0xFF2A231B)
private val Sour = Color(0xFF6B8E4E)
private val SourDim = Color(0xFF9CBB7E)
private val Rust = Color(0xFFB3492B)

private val LightColors = lightColorScheme(
    primary = Crust,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E3CE),
    onPrimaryContainer = CrustDeep,
    secondary = Sour,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2EDD5),
    onSecondaryContainer = Color(0xFF2F4220),
    error = Rust,
    background = Crumb,
    surface = Crumb,
    surfaceVariant = Color(0xFFF0E5D6),
    onSurfaceVariant = Color(0xFF6E6255),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD9A876),
    onPrimary = Color(0xFF3F2913),
    primaryContainer = CrustDeep,
    onPrimaryContainer = Color(0xFFF3E3CE),
    secondary = SourDim,
    onSecondary = Color(0xFF243418),
    secondaryContainer = Color(0xFF3C5229),
    onSecondaryContainer = Color(0xFFE2EDD5),
    error = Color(0xFFE58A6D),
    background = CrumbDark,
    surface = SurfaceDark,
    surfaceVariant = Color(0xFF3A322A),
    onSurfaceVariant = Color(0xFFBEB0A0),
)

@Composable
fun LevainTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
