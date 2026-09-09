package one.srz.jellywear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import one.srz.jellywear.data.AppPreferences

@Composable
fun JellywearTheme(preferences: AppPreferences, content: @Composable () -> Unit) {
    val accent = Color(preferences.accentColorArgb)
    val fontColor = Color(preferences.fontColorArgb)

    // Always dark: Wear OS app quality requirement WO-V13 demands a black
    // background for every app, so there is no light theme to switch to.
    // Accent and font color stay user-configurable on top of it.
    val colors = Colors(
        primary = accent,
        primaryVariant = accent,
        secondary = accent,
        background = Background,
        surface = Surface,
        onPrimary = OnAccent,
        onSecondary = OnAccent,
        onBackground = fontColor,
        onSurface = fontColor,
    )

    MaterialTheme(
        colors = colors,
        content = content,
    )
}
