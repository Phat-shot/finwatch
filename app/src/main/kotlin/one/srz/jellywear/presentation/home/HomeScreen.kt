package one.srz.jellywear.presentation.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import kotlinx.coroutines.launch
import one.srz.jellywear.R
import one.srz.jellywear.data.JellyfinSession
import one.srz.jellywear.playback.PlaybackQueue
import one.srz.jellywear.presentation.library.Category
import one.srz.jellywear.presentation.library.ShuffleableChip
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.request.GetItemsRequest

@Composable
fun HomeScreen(
    session: JellyfinSession,
    onOpenCategory: (Category) -> Unit,
    onShufflePlay: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
    ) {
        item {
            ListHeader {
                Text(text = stringResource(R.string.app_name))
            }
        }
        items(Category.entries) { category ->
            ShuffleableChip(
                text = stringResource(category.titleRes),
                onClick = { onOpenCategory(category) },
                onLongClick = {
                    scope.launch {
                        val playableKind = when (category) {
                            Category.MUSIC -> BaseItemKind.AUDIO
                            Category.SERIES -> BaseItemKind.EPISODE
                            Category.AUDIO -> BaseItemKind.AUDIO_BOOK
                            Category.MOVIES -> BaseItemKind.MOVIE
                        }
                        val queue = session.fetchShuffledQueue(
                            GetItemsRequest(
                                userId = session.userId,
                                recursive = true,
                                includeItemTypes = listOf(playableKind),
                            ),
                        )
                        if (queue != null) {
                            PlaybackQueue.items = queue
                            onShufflePlay()
                        }
                    }
                },
            )
        }
    }
}
