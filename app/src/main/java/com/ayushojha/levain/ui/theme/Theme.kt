package com.ayushojha.levain.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

// Serif display over sans body: the "artisanal bakery" typographic pairing,
// using system families so no fonts ship in the APK.
private val LevainTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.4.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp),
)

// Soft, bready corners everywhere.
private val LevainShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun LevainTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = LevainTypography,
        shapes = LevainShapes,
        content = content,
    )
}
