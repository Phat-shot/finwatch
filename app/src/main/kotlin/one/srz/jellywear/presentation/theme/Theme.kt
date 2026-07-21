package one.srz.jellywear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val JellywearColors = Colors(
    primary = NeonAccent,
    primaryVariant = NeonAccentDim,
    secondary = NeonAccent,
    background = Background,
    surface = Surface,
    onPrimary = OnAccent,
    onSecondary = OnAccent,
    onBackground = TextAnthracite,
    onSurface = TextAnthracite,
)

@Composable
fun JellywearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = JellywearColors,
        content = content,
    )
}
