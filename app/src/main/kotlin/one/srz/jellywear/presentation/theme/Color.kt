package one.srz.jellywear.presentation.theme

import androidx.compose.ui.graphics.Color
import one.srz.jellywear.R

val Background = Color(0xFF000000)
// Chip/row backgrounds: a near-black neutral, not the accent color, so text
// stays legible on top of it (the accent is reserved for icon glyphs).
val Surface = Color(0xFF161616)

val BackgroundLight = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFFEAEAEA)

val OnAccent = Color(0xFF000000)

/** Preset accent colors (icon tint) the user can pick from in Settings. */
val AccentColorPresets = listOf(
    0xFFCCE600.toInt() to R.string.color_neon_yellow_green,
    0xFF00E5FF.toInt() to R.string.color_cyan,
    0xFFFF00E5.toInt() to R.string.color_magenta,
    0xFFFF9500.toInt() to R.string.color_orange,
    0xFFFF3B30.toInt() to R.string.color_red,
    0xFF3399FF.toInt() to R.string.color_blue,
)

/** Preset text colors the user can pick from in Settings. */
val FontColorPresets = listOf(
    0xFF6F7578.toInt() to R.string.color_anthracite,
    0xFFB0B0B0.toInt() to R.string.color_light_gray,
    0xFFFFFFFF.toInt() to R.string.color_white,
    0xFF3A3A3A.toInt() to R.string.color_dark_gray,
)
