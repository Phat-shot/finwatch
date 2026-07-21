package one.srz.jellywear.presentation.theme

import androidx.compose.ui.graphics.Color

val Background = Color(0xFF000000)
// Chip/row backgrounds: a near-black neutral, not the accent color, so text
// stays legible on top of it (the accent is reserved for icon glyphs).
val Surface = Color(0xFF161616)

val BackgroundLight = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFFEAEAEA)

val OnAccent = Color(0xFF000000)

/** Preset accent colors (icon tint) the user can pick from in Settings. */
val AccentColorPresets = listOf(
    0xFFCCE600.toInt() to "Neon yellow-green",
    0xFF00E5FF.toInt() to "Cyan",
    0xFFFF00E5.toInt() to "Magenta",
    0xFFFF9500.toInt() to "Orange",
    0xFFFF3B30.toInt() to "Red",
    0xFF3399FF.toInt() to "Blue",
)

/** Preset text colors the user can pick from in Settings. */
val FontColorPresets = listOf(
    0xFF6F7578.toInt() to "Anthracite",
    0xFFB0B0B0.toInt() to "Light gray",
    0xFFFFFFFF.toInt() to "White",
    0xFF3A3A3A.toInt() to "Dark gray",
)
