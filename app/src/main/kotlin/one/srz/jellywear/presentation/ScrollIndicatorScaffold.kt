package one.srz.jellywear.presentation

import androidx.compose.runtime.Composable
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold

/**
 * Wraps a scrollable screen in the scroll position indicator that Wear OS
 * expects along the right edge.
 *
 * This is not cosmetic. Google Play enforces it as a Wear app quality
 * requirement -- "the app does not display a scrollbar when the user
 * interacts with a scrollable view" is a policy violation that blocks
 * releases, which is exactly what happened to 1.21. Every screen with a
 * scrollable list has to go through here; a bare ScalingLazyColumn is a
 * policy violation waiting to be reported.
 */
@Composable
fun ScrollIndicatorScaffold(
    state: ScalingLazyListState,
    content: @Composable () -> Unit,
) {
    Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = state) }) {
        content()
    }
}
