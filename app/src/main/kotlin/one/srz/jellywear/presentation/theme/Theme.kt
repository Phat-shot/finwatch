package one.srz.jellywear.presentation.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.data.ThemeMode

@Composable
fun JellywearTheme(preferences: AppPreferences, content: @Composable () -> Unit) {
    val accent = Color(preferences.accentColorArgb)
    val fontColor = Color(preferences.fontColorArgb)

    // Most Wear OS devices have no user-facing light/dark toggle and report
    // UI_MODE_NIGHT_UNDEFINED, which isSystemInDarkTheme() treats as "not
    // dark" -- SYSTEM mode would always resolve to the light theme. Instead,
    // only go light when the system explicitly says so; undefined (the
    // common case) and explicit night mode both stay dark.
    val nightMode = LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK
    val isDark = when (preferences.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> nightMode != Configuration.UI_MODE_NIGHT_NO
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
