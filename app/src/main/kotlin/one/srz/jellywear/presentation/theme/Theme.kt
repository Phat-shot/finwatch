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

    val colors = if (preferences.isDarkMode) {
        Colors(
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
    } else {
        Colors(
            primary = accent,
            primaryVariant = accent,
            secondary = accent,
            background = BackgroundLight,
            surface = SurfaceLight,
            onPrimary = OnAccent,
            onSecondary = OnAccent,
            onBackground = fontColor,
            onSurface = fontColor,
        )
    }

    MaterialTheme(
        colors = colors,
        content = content,
    )
}
