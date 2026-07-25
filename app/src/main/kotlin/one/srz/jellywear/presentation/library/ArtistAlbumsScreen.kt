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

/** Albums by one artist, found via the artistIds filter (not folder parentage). */
@Composable
fun ArtistAlbumsScreen(
    session: JellyfinSession,
    preferences: AppPreferences,
    artistId: String,
    onOpenAlbum: (String) -> Unit,
    onShufflePlay: () -> Unit,
    // Used only by the single-album auto-skip below -- see the matching
    // parameter on CategoryScreen for why this needs to be distinct from
    // onOpenAlbum (which is for actual taps).
    onSkipToAlbum: (String) -> Unit,
) {
    var albums by remember(artistId) { mutableStateOf<List<BaseItemDto>?>(null) }
    var error by remember(artistId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    LaunchedEffect(artistId) {
        val api = session.api ?: return@LaunchedEffect
        val id = artistId.toUUIDOrNull() ?: return@LaunchedEffect
        try {
            val result = api.itemsApi.getItems(
                GetItemsRequest(
                    userId = session.userId,
                    recursive = true,
                    artistIds = listOf(id),
                    includeItemTypes = listOf(BaseItemKind.MUSIC_ALBUM),
                    sortBy = listOf(ItemSortBy.SORT_NAME),
                ),
            ).content.items
            // An artist with only one album: skip straight into it instead
            // of showing a pick-list of one, same as CategoryScreen/ItemBrowserScreen.
            val onlyAlbum = result.singleOrNull()
            if (onlyAlbum != null) {
                onSkipToAlbum(onlyAlbum.id.toString())
            } else {
                albums = result
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
                Text(text = stringResource(R.string.category_music))
            }
        }
        when {
            error != null -> item {
                Text(text = error ?: stringResource(R.string.login_error_generic))
            }
            albums == null -> item {
                Text(text = stringResource(R.string.library_loading))
            }
            albums.orEmpty().isEmpty() -> item {
                Text(text = stringResource(R.string.library_empty))
            }
            else -> items(albums.orEmpty()) { album ->
                val id = album.id.toString()
                ShuffleableChip(
                    text = album.name ?: "?",
                    imageUrl = if (preferences.coverArtMode != CoverArtMode.OFF) session.imageUrl(album.id) else null,
                    onClick = { onOpenAlbum(id) },
                    onLongClick = {
                        scope.launch {
                            val queue = session.fetchShuffledQueue(
                                GetItemsRequest(
                                    userId = session.userId,
                                    parentId = album.id,
                                    recursive = true,
                                    mediaTypes = listOf(MediaType.AUDIO),
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
