package one.srz.jellywear.presentation.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.IconButton
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import one.srz.jellywear.R
import one.srz.jellywear.data.JellyfinSession
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.audioApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.serializer.toUUID

@Composable
fun PlayerScreen(session: JellyfinSession, itemId: String) {
    val context = LocalContext.current
    var item by remember(itemId) { mutableStateOf<BaseItemDto?>(null) }
    var error by remember(itemId) { mutableStateOf<String?>(null) }
    var isPlaying by remember(itemId) { mutableStateOf(true) }

    val player = remember(itemId) { ExoPlayer.Builder(context).build() }

    LaunchedEffect(itemId) {
        val api = session.api ?: return@LaunchedEffect
        try {
            item = api.userLibraryApi.getItem(itemId = itemId.toUUID(), userId = session.userId).content
        } catch (e: ApiClientException) {
            error = e.message
        }
    }

    LaunchedEffect(item) {
        val api = session.api
        val currentItem = item
        if (api != null && currentItem != null) {
            player.setMediaItem(MediaItem.fromUri(buildStreamUrl(api, currentItem)))
            player.prepare()
            player.playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            error != null -> Text(
                text = error ?: stringResource(R.string.login_error_generic),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
            item == null -> CircularProgressIndicator()
            else -> {
                val isVideo = item?.mediaType == MediaType.VIDEO
                if (isVideo) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = false
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!isVideo) {
                        Text(
                            text = item?.name ?: "",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.title3,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

private fun buildStreamUrl(api: ApiClient, item: BaseItemDto): String {
    val deviceId = api.deviceInfo.id
    val base = if (item.mediaType == MediaType.VIDEO) {
        api.videosApi.getVideoStreamUrl(itemId = item.id, static = true, deviceId = deviceId)
    } else {
        api.audioApi.getAudioStreamUrl(itemId = item.id, static = true, deviceId = deviceId)
    }
    val separator = if (base.contains("?")) "&" else "?"
    return "$base$separator${ApiClient.QUERY_ACCESS_TOKEN}=${api.accessToken}"
}
