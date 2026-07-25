package one.srz.jellywear.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.min
import one.srz.jellywear.presentation.theme.JellyfinBlue
import one.srz.jellywear.presentation.theme.JellyfinPurple

private val RING_STROKE_WIDTH = 5.dp
private val RING_INSET = 3.dp

/**
 * A ring hugging the outer edge of the round display, showing playback
 * progress starting at 12 o'clock and sweeping clockwise. Purely decorative
 * -- no touch handling -- since it's drawn above every screen (see
 * JellywearApp) and a touch-reactive version repeatedly ended up competing
 * with SwipeDismissableNavHost's swipe-to-dismiss for the same gestures,
 * breaking back navigation. Kept simple and safe over seekable.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = RING_STROKE_WIDTH.toPx()
        val inset = RING_INSET.toPx() + stroke / 2f
        val diameter = min(size.width, size.height) - inset * 2f
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = Color.White.copy(alpha = 0.16f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        if (progress > 0f) {
            drawArc(
                brush = Brush.sweepGradient(listOf(JellyfinBlue, JellyfinPurple, JellyfinBlue)),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
