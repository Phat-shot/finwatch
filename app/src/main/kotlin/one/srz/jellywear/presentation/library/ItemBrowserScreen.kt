package one.srz.jellywear.presentation.library

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.data.CoverArtMode
import one.srz.jellywear.data.JellyfinSession
import one.srz.jellywear.playback.PlaybackQueue
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

// Upper bound for the single-child folder skip below -- a pathological or
// self-referencing folder structure must not turn into an unbounded loop of
// network calls.
private const val MAX_SINGLE_FOLDER_SKIPS = 15

@Composable
fun ItemBrowserScreen(
    session: JellyfinSession,
    preferences: AppPreferences,
    parentId: String,
    onOpenFolder: (String) -> Unit,
    onPlayItem: (String) -> Unit,
    onShufflePlay: () -> Unit,
) {
    var children by remember(parentId) { mutableStateOf<List<BaseItemDto>?>(null) }
    var error by remember(parentId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    LaunchedEffect(parentId) {
        val api = session.api ?: return@LaunchedEffect
        try {
            // Skip straight through folders that only wrap a single child
            // folder (e.g. a season that's the only season of a series) --
            // there's nothing to choose between, so stop and show the first
            // level that actually has something to pick from.
            var currentParentId = parentId.toUUIDOrNull()
            var skips = 0
            while (true) {
                val items = api.itemsApi.getItems(
                    GetItemsRequest(
                        userId = session.userId,
                        parentId = currentParentId,
                        recursive = false,
                        sortBy = listOf(ItemSortBy.SORT_NAME),
                    ),
                ).content.items
                val onlyChild = items.singleOrNull()
                if (onlyChild != null && onlyChild.isFolder == true && skips < MAX_SINGLE_FOLDER_SKIPS) {
                    currentParentId = onlyChild.id
                    skips++
                    continue
                }
                children = items
                break
            }
        } catch (e: ApiClientException) {
            error = e.message
        }
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
    ) {
        item {
            ListHeader {
                Text(text = stringResource(R.string.library_title))
            }
        }
        when {
            error != null -> item {
                Text(text = error ?: stringResource(R.string.login_error_generic))
            }
            children == null -> item {
                Text(text = stringResource(R.string.library_loading))
            }
            children.orEmpty().isEmpty() -> item {
                Text(text = stringResource(R.string.library_empty))
            }
            else -> items(children.orEmpty()) { child ->
                val id = child.id.toString()
                ShuffleableChip(
                    text = child.name ?: "?",
                    imageUrl = if (preferences.coverArtMode != CoverArtMode.OFF) session.imageUrl(child.id) else null,
                    onClick = {
                        if (child.isFolder == true) onOpenFolder(id) else onPlayItem(id)
                    },
                    onLongClick = {
                        scope.launch {
                            val queue = session.fetchShuffledQueue(
                                GetItemsRequest(
                                    userId = session.userId,
                                    parentId = child.id,
                                    recursive = true,
                                    mediaTypes = listOf(MediaType.AUDIO, MediaType.VIDEO),
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
}
