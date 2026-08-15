package com.ayushojha.levain.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The only place a hex value lives in this app.
 *
 * **Hearth** is the shell: crust browns, crumb grounds, olive for healthy,
 * ember for act-now. **Instrument** is the Ember register that live bake
 * screens switch into — always dark, built to be read at arm's length with
 * floury hands. Illustrations draw from here too, so nothing invents its own brown.
 */
object Palette {

    // --- Hearth, day ---
    val Crumb = Color(0xFFF5EADB)
    val CrumbRaised = Color(0xFFFDF7EC)
    val CrumbShade = Color(0xFFE9DCC8)
    val Crust = Color(0xFF4A2C17)
    val CrustInk = Color(0xFF3A2413)
    val CrustContainer = Color(0xFFE4D2B8)
    val Muted = Color(0xFF8A7A66)
    val Outline = Color(0xFFD9C8AF)

    // --- Hearth, night ---
    val NightGround = Color(0xFF17120D)
    val NightSurface = Color(0xFF201A13)
    val NightRaised = Color(0xFF2E2419)
    val NightInk = Color(0xFFEFE3D2)
    val NightMuted = Color(0xFFB0A08A)
    val NightOutline = Color(0xFF4A3B2A)
    val CrustLight = Color(0xFFE0B584)

    // --- Living things ---
    val Olive = Color(0xFF6B8E4E)
    val OliveLight = Color(0xFF9CBB7E)
    val OliveContainer = Color(0xFFDCE8CE)
    val OliveDeep = Color(0xFF2F4220)
    val OliveNight = Color(0xFF3C5229)

    // --- Attention ---
    val Ember = Color(0xFFC8622E)
    val EmberLight = Color(0xFFE4844F)
    val EmberContainer = Color(0xFFF7DECC)
    val EmberDeep = Color(0xFF5A2408)
    val Rust = Color(0xFFA83E22)
    val RustLight = Color(0xFFE58A6D)

    // --- Ember register: the kitchen instrument, always dark ---
    val InstrumentGround = Color(0xFF121110)
    val InstrumentPanel = Color(0xFF1D1B19)
    val InstrumentInk = Color(0xFFE6E1DA)
    val InstrumentMuted = Color(0xFF7A736B)
    val InstrumentOutline = Color(0xFF2C2926)
    val Hot = Color(0xFFE8562A)
    val HotInk = Color(0xFF150802)
    val OnTrack = Color(0xFF7FB069)

    // --- Illustration: the jar Avatar (starter section only) ---
    val JarGlass = Color(0xFFEDE3D4)
    val JarGlassNight = Color(0xFF4A4238)
    val Dough = Color(0xFFF6EBDD)
    val DoughShade = Color(0xFFE3CDA8)
    val DoughRetired = Color(0xFFCFC8BE)
    val LidRetired = Color(0xFF8E8579)
    val FaceInk = Color(0xFF4A3624)
    val Cheek = Color(0x33D96A4A)

    /** Celebration confetti — the whole palette, thrown in the air. */
    val Confetti = listOf(Crust, Olive, CrustLight, Rust, CrustContainer)
}
