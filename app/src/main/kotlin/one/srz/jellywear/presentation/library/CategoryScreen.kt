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
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

/**
 * Flat, server-wide list of everything of [category]'s item kind -- e.g.
 * Movies aggregates every movie library, not just one. Music routes to
 * [ArtistAlbumsScreen] instead of the generic folder browser, since
 * Jellyfin artists aren't a real folder parent of their albums.
 */
@Composable
fun CategoryScreen(
    session: JellyfinSession,
    preferences: AppPreferences,
    category: Category,
    onOpenArtist: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onPlayItem: (String) -> Unit,
    onShufflePlay: () -> Unit,
) {
    var elements by remember(category) { mutableStateOf<List<BaseItemDto>?>(null) }
    var error by remember(category) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    LaunchedEffect(category) {
        val api = session.api ?: return@LaunchedEffect
        try {
            val result = when (category) {
                Category.AUDIO -> session.fetchAudiobooks()
                Category.FAVORITES -> session.fetchFavoriteMusic()
                else -> api.itemsApi.getItems(
                    GetItemsRequest(
                        userId = session.userId,
                        recursive = true,
                        includeItemTypes = listOf(category.itemKind),
                        sortBy = listOf(ItemSortBy.SORT_NAME),
                    ),
                ).content.items
            }
            // If there's only one thing to pick from, skip straight past this
            // list instead of making the user tap through a list of one --
            // same idea as ItemBrowserScreen's folder-drill skip. Music
            // routes to the artist screen regardless of isFolder (artists
            // aren't folder items); everything else only skips when the
            // single result is itself a folder to drill into, so a lone
            // playable item (e.g. the only movie in the library) still shows
            // up for an explicit tap instead of surprise-navigating.
            val onlyResult = result.singleOrNull()
            when {
                category == Category.MUSIC && onlyResult != null -> onOpenArtist(onlyResult.id.toString())
                onlyResult != null && onlyResult.isFolder == true -> onOpenFolder(onlyResult.id.toString())
                else -> elements = result
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
                Text(text = stringResource(category.titleRes))
            }
        }
        when {
            error != null -> item {
                Text(text = error ?: stringResource(R.string.login_error_generic))
            }
            elements == null -> item {
                Text(text = stringResource(R.string.library_loading))
            }
            elements.orEmpty().isEmpty() -> item {
                Text(text = stringResource(R.string.library_empty))
            }
            else -> items(elements.orEmpty()) { element ->
                val id = element.id.toString()
                ShuffleableChip(
                    text = element.name ?: "?",
                    imageUrl = if (preferences.coverArtMode != CoverArtMode.OFF) session.imageUrl(element.id) else null,
                    onClick = {
                        when {
                            category == Category.MUSIC -> onOpenArtist(id)
                            element.isFolder == true -> onOpenFolder(id)
                            else -> onPlayItem(id)
                        }
                    },
                    onLongClick = {
                        scope.launch {
                            val request = if (category == Category.MUSIC) {
                                GetItemsRequest(
                                    userId = session.userId,
                                    artistIds = listOf(element.id),
                                    recursive = true,
                                    includeItemTypes = listOf(BaseItemKind.AUDIO),
                                )
                            } else {
                                GetItemsRequest(
                                    userId = session.userId,
                                    parentId = id.toUUIDOrNull(),
                                    recursive = true,
                                    mediaTypes = listOf(MediaType.AUDIO, MediaType.VIDEO),
                                )
                            }
                            val queue = session.fetchShuffledQueue(request)
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
