package one.srz.jellywear.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.data.ThemeMode

@Composable
fun JellywearTheme(preferences: AppPreferences, content: @Composable () -> Unit) {
    val accent = Color(preferences.accentColorArgb)
    val fontColor = Color(preferences.fontColorArgb)

    val isDark = when (preferences.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colors = if (isDark) {
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
