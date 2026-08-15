package com.ayushojha.levain.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The app has two moods and therefore two registers.
 *
 * [Register.Hearth] is the shell — slow, warm, affectionate: starter care,
 * recipes, history. [Register.Bake] is the instrument a live bake switches
 * into — dark, high-contrast, numerals first. The switch is meaningful, not
 * decorative: one flat palette serving both is why the app used to read flat.
 */
enum class Register { Hearth, Bake }

private val HearthDay = lightColorScheme(
    primary = Palette.Crust,
    onPrimary = Palette.Crumb,
    primaryContainer = Palette.CrustContainer,
    onPrimaryContainer = Palette.CrustInk,
    secondary = Palette.Olive,
    onSecondary = Palette.Crumb,
    secondaryContainer = Palette.OliveContainer,
    onSecondaryContainer = Palette.OliveDeep,
    tertiary = Palette.Ember,
    onTertiary = Palette.Crumb,
    tertiaryContainer = Palette.EmberContainer,
    onTertiaryContainer = Palette.EmberDeep,
    error = Palette.Rust,
    onError = Palette.Crumb,
    background = Palette.Crumb,
    onBackground = Palette.CrustInk,
    surface = Palette.CrumbRaised,
    onSurface = Palette.CrustInk,
    surfaceVariant = Palette.CrumbShade,
    onSurfaceVariant = Palette.Muted,
    outline = Palette.Outline,
    outlineVariant = Palette.CrumbShade,
)

private val HearthNight = darkColorScheme(
    primary = Palette.CrustLight,
    onPrimary = Palette.CrustInk,
    primaryContainer = Palette.Crust,
    onPrimaryContainer = Palette.Crumb,
    secondary = Palette.OliveLight,
    onSecondary = Palette.OliveDeep,
    secondaryContainer = Palette.OliveNight,
    onSecondaryContainer = Palette.OliveContainer,
    tertiary = Palette.EmberLight,
    onTertiary = Palette.EmberDeep,
    tertiaryContainer = Palette.EmberDeep,
    onTertiaryContainer = Palette.EmberContainer,
    error = Palette.RustLight,
    onError = Palette.CrustInk,
    background = Palette.NightGround,
    onBackground = Palette.NightInk,
    surface = Palette.NightSurface,
    onSurface = Palette.NightInk,
    surfaceVariant = Palette.NightRaised,
    onSurfaceVariant = Palette.NightMuted,
    outline = Palette.NightOutline,
    outlineVariant = Palette.NightRaised,
)

/** Ember: one scheme, dark in both system themes — a kitchen is a kitchen. */
private val Instrument = darkColorScheme(
    primary = Palette.Hot,
    onPrimary = Palette.HotInk,
    primaryContainer = Palette.InstrumentPanel,
    onPrimaryContainer = Palette.Hot,
    secondary = Palette.OnTrack,
    onSecondary = Palette.HotInk,
    secondaryContainer = Palette.InstrumentPanel,
    onSecondaryContainer = Palette.OnTrack,
    tertiary = Palette.Hot,
    onTertiary = Palette.HotInk,
    error = Palette.Hot,
    onError = Palette.HotInk,
    background = Palette.InstrumentGround,
    onBackground = Palette.InstrumentInk,
    surface = Palette.InstrumentPanel,
    onSurface = Palette.InstrumentInk,
    surfaceVariant = Palette.InstrumentPanel,
    onSurfaceVariant = Palette.InstrumentMuted,
    outline = Palette.InstrumentOutline,
    outlineVariant = Palette.InstrumentOutline,
)

// Serif for the things that have names, humanist sans for everything you read
// quickly. No bundled fonts: a webfont that fails to load is worse than a
// system stack that never does.
private val Serif = FontFamily.Serif
private val Sans = FontFamily.SansSerif

private val LevainTypography = Typography(
    displaySmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 27.sp, lineHeight = 33.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 12.5.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, letterSpacing = 1.2.sp),
)

/** Type roles Material doesn't have a slot for. */
object LevainType {
    /** Ember's countdowns: monospace and tabular so digits never jitter. */
    val numeral = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1).sp,
        textAlign = TextAlign.Center,
    )
    val numeralSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    )
    /** Tracked uppercase, for section headers and state labels. */
    val eyebrow = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.6.sp,
    )
}

/** Soft, bready corners — nothing in a bakery has a sharp edge. */
private val LevainShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** One spacing rhythm, so no screen invents its own. */
object Spacing {
    val hair = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val section = 40.dp
}

@Composable
fun LevainTheme(
    register: Register = Register.Hearth,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = when {
        register == Register.Bake -> Instrument
        darkTheme -> HearthNight
        else -> HearthDay
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = LevainTypography,
        shapes = LevainShapes,
        content = content,
    )
}

/** Wrap a live bake screen in this to switch into the instrument register. */
@Composable
fun BakeRegister(content: @Composable () -> Unit) =
    LevainTheme(register = Register.Bake, content = content)
