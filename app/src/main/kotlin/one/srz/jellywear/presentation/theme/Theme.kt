package one.srz.jellywear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val JellywearColors = Colors(
    primary = Purple,
    primaryVariant = PurpleVariant,
    secondary = Highlight,
    background = Background,
    surface = Surface,
    onPrimary = OnPrimary,
    onSecondary = OnPrimary,
    onBackground = OnPrimary,
    onSurface = OnPrimary,
)

@Composable
fun JellywearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = JellywearColors,
        content = content,
    )
}
