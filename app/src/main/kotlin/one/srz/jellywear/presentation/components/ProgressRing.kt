package one.srz.jellywear.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt
import one.srz.jellywear.presentation.theme.JellyfinBlue
import one.srz.jellywear.presentation.theme.JellyfinPurple

private val RING_STROKE_WIDTH = 5.dp
private val RING_INSET = 3.dp

// How far in from the true edge counts as "on the ring" for touch purposes.
// Kept generous since precisely tapping a thin 5dp stroke on a small round
// watch face isn't realistic -- everything outside this band is left
// unconsumed so normal screen content (lists, chips, buttons) underneath
// still gets it.
private val RING_HIT_BAND = 26.dp

// SwipeDismissableNavHost's swipe-to-dismiss (Wear's "back") is a
// left-edge-driven gesture and, since the ring is drawn above the nav host,
// competes with it for the same touch region. A thin exclusion strip wasn't
// enough to reliably stay out of its way, so this excludes the entire left
// half of the screen from ring interaction instead -- back-swipe always
// gets it uncontested, and the ring stays fully seekable via its top,
// right, and bottom.

/**
 * A ring hugging the outer edge of the round display, showing playback
 * progress starting at 12 o'clock and sweeping clockwise. When [onSeek] is
 * non-null, tapping or dragging within the outer band (right/top/bottom
 * only, see above) seeks; pass null to render a purely decorative ring with
 * no touch handling at all, e.g. on every screen except the player, so it
 * can never compete with that screen's own gestures (scrolling, back-swipe).
 */
@Composable
fun ProgressRing(
    progress: Float,
    onSeek: ((Float) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val seekModifier = if (onSeek != null) {
        Modifier.pointerInput(onSeek) {
            val hitBandPx = RING_HIT_BAND.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val widthPx = size.width.toFloat()
                val heightPx = size.height.toFloat()
                val center = Offset(widthPx / 2f, heightPx / 2f)
                val radius = min(widthPx, heightPx) / 2f
                val distanceFromEdge = radius - distanceBetween(down.position, center)
                val inRingBand = distanceFromEdge in 0f..hitBandPx
                val inBackGestureZone = down.position.x <= widthPx / 2f
                if (!inRingBand || inBackGestureZone) {
                    return@awaitEachGesture
                }

                down.consume()
                onSeek(fractionFor(down.position, center))
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    change.consume()
                    onSeek(fractionFor(change.position, center))
                }
            }
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier.then(seekModifier),
    ) {
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

private fun distanceBetween(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

/** Progress fraction (0..1) for a touch position, measured clockwise from 12 o'clock. */
private fun fractionFor(position: Offset, center: Offset): Float {
    val angle = atan2((position.y - center.y).toDouble(), (position.x - center.x).toDouble())
    var fromTwelve = angle + PI / 2
    if (fromTwelve < 0) fromTwelve += 2 * PI
    return (fromTwelve / (2 * PI)).toFloat().coerceIn(0f, 1f)
}
