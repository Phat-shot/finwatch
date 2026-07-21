package one.srz.jellywear.presentation.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.launch
import one.srz.jellywear.R
import one.srz.jellywear.data.JellyfinSession
import one.srz.jellywear.playback.PlaybackQueue
import one.srz.jellywear.presentation.library.Category
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.request.GetItemsRequest

private fun Category.icon(): ImageVector = when (this) {
    Category.MUSIC -> Icons.Filled.MusicNote
    Category.AUDIO -> Icons.Filled.Headphones
    Category.SERIES -> Icons.Filled.Tv
    Category.MOVIES -> Icons.Filled.Movie
}

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
            CategoryIconTile(
                icon = category.icon(),
                label = stringResource(category.titleRes),
                onClick = { onOpenCategory(category) },
                onLongClick = {
                    scope.launch {
                        val queue = if (category == Category.AUDIO) {
                            session.fetchAudiobooks().shuffled().takeIf { it.isNotEmpty() }
                        } else {
                            val playableKind = when (category) {
                                Category.MUSIC -> BaseItemKind.AUDIO
                                Category.SERIES -> BaseItemKind.EPISODE
                                Category.MOVIES -> BaseItemKind.MOVIE
                                Category.AUDIO -> error("handled above")
                            }
                            session.fetchShuffledQueue(
                                GetItemsRequest(
                                    userId = session.userId,
                                    recursive = true,
                                    includeItemTypes = listOf(playableKind),
                                ),
                            )
                        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryIconTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colors.surface, RoundedCornerShape(50))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.button,
            color = MaterialTheme.colors.onSurface,
        )
    }
}
