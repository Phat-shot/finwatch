package one.srz.jellywear.presentation.theme

import androidx.compose.ui.graphics.Color

// Neon yellow-green accent, used for icons and interactive highlights.
val NeonAccent = Color(0xFFCCFF00)
val NeonAccentDim = Color(0xFF9ACC00)

val Background = Color(0xFF000000)
// Chip/row backgrounds: a near-black neutral, not the neon accent, so text
// stays legible on top of it (neon is reserved for icon glyphs).
val Surface = Color(0xFF161616)

// Anthracite text -- deliberately dark/muted against the black background;
// nudged a bit lighter than true anthracite (~#2B2E30) so it stays legible
// at Wear OS text sizes.
val TextAnthracite = Color(0xFF4A4E50)
val OnAccent = Color(0xFF000000)
