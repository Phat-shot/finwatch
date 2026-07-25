package one.srz.jellywear.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.data.JellyfinSession
import one.srz.jellywear.playback.PlaybackQueue
import one.srz.jellywear.presentation.library.Category
import one.srz.jellywear.presentation.theme.JellyfinBlue
import one.srz.jellywear.presentation.theme.JellyfinPurple
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.request.GetItemsRequest

private const val ICON_TILE_SIZE_DP = 54
private const val ICON_SIZE_DP = 26

private fun Category.icon(): ImageVector = when (this) {
    Category.MUSIC -> Icons.Filled.MusicNote
    Category.AUDIO -> Icons.Filled.Headphones
    Category.SERIES -> Icons.Filled.Tv
    Category.MOVIES -> Icons.Filled.Movie
    Category.FAVORITES -> Icons.Filled.Favorite
    Category.PLAYLISTS -> Icons.Filled.QueueMusic
}

/**
 * Compact icon-grid launcher: the visible category tiles (configurable in
 * Settings > Libraries) plus Settings, two per row, no text labels -- meant
 * to be scannable/tappable at a glance rather than a long scrolling list.
 */
@Composable
fun HomeScreen(
    session: JellyfinSession,
    preferences: AppPreferences,
    onOpenCategory: (Category) -> Unit,
    onShufflePlay: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    val visibleCategories = Category.entries.filter { preferences.isCategoryVisible(it) }

    fun shufflePlay(category: Category) {
        scope.launch {
            val queue = when (category) {
                Category.AUDIO -> session.fetchAudiobooks().shuffled().takeIf { it.isNotEmpty() }
                Category.FAVORITES -> session.fetchFavoriteMusic().shuffled().takeIf { it.isNotEmpty() }
                Category.PLAYLISTS -> session.fetchPlaylistTracks().shuffled().takeIf { it.isNotEmpty() }
                Category.MUSIC -> session.fetchShuffledQueue(
                    GetItemsRequest(
                        userId = session.userId,
                        recursive = true,
                        includeItemTypes = listOf(BaseItemKind.AUDIO),
                    ),
                )
                Category.SERIES -> session.fetchShuffledQueue(
                    GetItemsRequest(
                        userId = session.userId,
                        recursive = true,
                        includeItemTypes = listOf(BaseItemKind.EPISODE),
                    ),
                )
                Category.MOVIES -> session.fetchShuffledQueue(
                    GetItemsRequest(
                        userId = session.userId,
                        recursive = true,
                        includeItemTypes = listOf(BaseItemKind.MOVIE),
                    ),
                )
            }
            if (queue != null) {
                PlaybackQueue.items = queue
                onShufflePlay()
            }
        }
    }

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
        items(visibleCategories.chunked(2)) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { category ->
                    CompactIconTile(
                        icon = category.icon(),
                        contentDescription = stringResource(category.titleRes),
                        onClick = { onOpenCategory(category) },
                        onLongClick = { shufflePlay(category) },
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CompactIconTile(
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings_tile),
                    onClick = onOpenSettings,
                    onLongClick = { },
                )
            }
        }
    }
}

// A thin Jellyfin blue-to-purple gradient ring around every tile -- independent
// of the user's chosen accent color, so the launcher keeps a consistent
// brand identity even when the accent is customized elsewhere.
private val TileBorderBrush = Brush.linearGradient(listOf(JellyfinBlue, JellyfinPurple))

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactIconTile(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(ICON_TILE_SIZE_DP.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colors.surface)
            .border(BorderStroke(1.5.dp, TileBorderBrush), CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(1.5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(ICON_SIZE_DP.dp),
        )
    }
}
