package one.srz.jellywear.presentation.player

import android.content.ComponentName
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
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import one.srz.jellywear.R
import one.srz.jellywear.data.JellyfinSession
import one.srz.jellywear.playback.PlaybackQueue
import one.srz.jellywear.playback.PlaybackService
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.audioApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.serializer.toUUID

const val PLAYER_QUEUE_ID = "queue"

@Composable
fun PlayerScreen(session: JellyfinSession, itemId: String) {
    val context = LocalContext.current
    var queueItems by remember(itemId) { mutableStateOf<List<BaseItemDto>>(emptyList()) }
    var error by remember(itemId) { mutableStateOf<String?>(null) }
    var isPlaying by remember(itemId) { mutableStateOf(true) }
    var controller by remember(itemId) { mutableStateOf<MediaController?>(null) }
    var positionMs by remember(itemId) { mutableStateOf(0L) }
    var durationMs by remember(itemId) { mutableStateOf(0L) }
    var currentIndex by remember(itemId) { mutableStateOf(0) }

    // Connects to (and starts, if needed) PlaybackService so playback keeps
    // running in the background via its MediaSession.
    DisposableEffect(itemId) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            { controller = controllerFuture.get() },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            MediaController.releaseFuture(controllerFuture)
            controller = null
        }
    }

    LaunchedEffect(itemId) {
        if (itemId == PLAYER_QUEUE_ID) {
            queueItems = PlaybackQueue.items
        } else {
            val api = session.api ?: return@LaunchedEffect
            try {
                val item = api.userLibraryApi.getItem(itemId = itemId.toUUID(), userId = session.userId).content
                queueItems = listOf(item)
            } catch (e: ApiClientException) {
                error = e.message
            }
        }
    }

    LaunchedEffect(queueItems, controller) {
        val api = session.api
        val ctrl = controller
        if (api != null && ctrl != null && queueItems.isNotEmpty() && ctrl.mediaItemCount == 0) {
            val mediaItems = queueItems.map { MediaItem.fromUri(buildStreamUrl(api, it)) }
            ctrl.setMediaItems(mediaItems)
            ctrl.prepare()
            ctrl.playWhenReady = true
        }
    }

    DisposableEffect(controller) {
        val ctrl = controller
        if (ctrl == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            }
            ctrl.addListener(listener)
            onDispose { ctrl.removeListener(listener) }
        }
    }

    LaunchedEffect(controller) {
        val ctrl = controller ?: return@LaunchedEffect
        while (true) {
            positionMs = ctrl.currentPosition.coerceAtLeast(0L)
            durationMs = ctrl.duration.coerceAtLeast(0L)
            currentIndex = ctrl.currentMediaItemIndex
            delay(500)
        }
    }

    val currentItem = queueItems.getOrNull(currentIndex)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            error != null -> Text(
                text = error ?: stringResource(R.string.login_error_generic),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
            queueItems.isEmpty() -> CircularProgressIndicator()
            else -> {
                val isVideo = currentItem?.mediaType == MediaType.VIDEO
                if (isVideo) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                // Fill the round screen's width -- corners get
                                // cropped, but the watch bezel hides those anyway.
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            }
                        },
                        update = { view -> view.player = controller },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!isVideo) {
                        Text(
                            text = currentItem?.name ?: "",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.title3,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Button(
                        onClick = { controller?.let { if (it.isPlaying) it.pause() else it.play() } },
                        colors = ButtonDefaults.iconButtonColors(),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                        )
                    }
                    if (durationMs > 0) {
                        Text(
                            text = "${formatMillis(positionMs)} / ${formatMillis(durationMs)}",
                            style = MaterialTheme.typography.caption2,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
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
