package one.srz.jellywear.presentation

import androidx.compose.foundation.ScrollState
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

/**
 * Same contract for a plain Column made scrollable with
 * Modifier.verticalScroll -- used where a list is the wrong shape (the
 * login flow's centered status screens). Making such screens scrollable
 * at all is itself a quality requirement: at the largest system font a
 * fixed, centered Column overflows the round display and its text is
 * cut off at the top and bottom.
 */
@Composable
fun ScrollIndicatorScaffold(
    state: ScrollState,
    content: @Composable () -> Unit,
) {
    Scaffold(positionIndicator = { PositionIndicator(scrollState = state) }) {
        content()
    }
}
