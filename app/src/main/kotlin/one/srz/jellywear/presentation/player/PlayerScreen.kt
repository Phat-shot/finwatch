package one.srz.jellywear.presentation.player

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import one.srz.jellywear.R
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.data.CoverArtMode
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
private const val CONTROLS_AUTO_HIDE_MS = 3000L
private const val AUDIO_AUTO_BACKGROUND_MS = 10_000L

@Composable
fun PlayerScreen(session: JellyfinSession, preferences: AppPreferences, itemId: String) {
    val context = LocalContext.current
    var queueItems by remember(itemId) { mutableStateOf<List<BaseItemDto>>(emptyList()) }
    var error by remember(itemId) { mutableStateOf<String?>(null) }
    var isPlaying by remember(itemId) { mutableStateOf(true) }
    var controller by remember(itemId) { mutableStateOf<MediaController?>(null) }
    var positionMs by remember(itemId) { mutableStateOf(0L) }
    var durationMs by remember(itemId) { mutableStateOf(0L) }
    var currentIndex by remember(itemId) { mutableStateOf(0) }
    var hasNext by remember(itemId) { mutableStateOf(false) }
    var hasPrevious by remember(itemId) { mutableStateOf(false) }
    // Whether *this* visit to the player has already handed its queue to
    // the controller. The underlying ExoPlayer lives in PlaybackService for
    // the whole process, so checking its mediaItemCount to decide "is this
    // fresh" doesn't work -- it's never 0 again once anything has ever
    // played, silently preventing every later item (audio or video) from
    // starting.
    var hasSetMediaItems by remember(itemId) { mutableStateOf(false) }
    var controlsVisible by remember(itemId) { mutableStateOf(true) }
    var interactionTick by remember(itemId) { mutableStateOf(0) }

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
        if (api != null && ctrl != null && queueItems.isNotEmpty() && !hasSetMediaItems) {
            val mediaItems = queueItems.map { queueItem ->
                MediaItem.Builder()
                    .setUri(buildStreamUrl(api, queueItem, preferences.transcodeEnabled))
                    // So the foreground-service notification shows the
                    // track/episode title (and Wear renders it as its
                    // native media-playback card) instead of a blank one.
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(queueItem.name)
                            .build(),
                    )
                    .build()
            }
            ctrl.setMediaItems(mediaItems)
            ctrl.prepare()
            ctrl.playWhenReady = true
            hasSetMediaItems = true
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
            hasNext = ctrl.hasNextMediaItem()
            hasPrevious = ctrl.hasPreviousMediaItem()
            delay(500)
        }
    }

    val currentItem = queueItems.getOrNull(currentIndex)
    val isVideo = currentItem?.mediaType == MediaType.VIDEO

    // Keep the screen on for video (there's nothing to look at otherwise);
    // let the normal timeout apply for audio. Also keep it on while the
    // item's type is still unknown (before the first fetch resolves) --
    // otherwise a slow network response lets the watch's short default
    // timeout turn the screen off mid-load, which can tear down the
    // video surface before ExoPlayer ever gets to start playback.
    val view = LocalView.current
    val keepScreenOn = currentItem == null || isVideo
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // Auto-hide video controls after a few seconds; tapping the video
    // toggles them back. Audio controls always stay visible.
    LaunchedEffect(controlsVisible, isVideo) {
        if (isVideo && controlsVisible) {
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    // For audio, return to the watch face after 10s of no interaction --
    // apps can't force the display off directly, but backgrounding the
    // activity lets the system's own idle/ambient timeout take over while
    // playback keeps going through the MediaSession. Any tap resets it.
    val activity = context as? ComponentActivity
    LaunchedEffect(isVideo, interactionTick) {
        if (!isVideo) {
            delay(AUDIO_AUTO_BACKGROUND_MS)
            activity?.moveTaskToBack(true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(itemId, isVideo) {
                detectTapGestures {
                    if (isVideo) controlsVisible = !controlsVisible
                    interactionTick++
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            error != null -> Text(
                text = error ?: stringResource(R.string.login_error_generic),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
            queueItems.isEmpty() -> CircularProgressIndicator()
            else -> {
                if (!isVideo &&
                    preferences.coverArtMode == CoverArtMode.FOLDERS_AND_PLAYBACK &&
                    currentItem != null
                ) {
                    AsyncImage(
                        model = session.imageUrl(currentItem.id),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                    )
                }
                if (isVideo) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                // Fill the round screen's width only (not
                                // height) -- ZOOM fills whichever dimension
                                // needs less scaling, which for wide video on
                                // a square/round screen means it fills height
                                // and crops the sides. FIXED_WIDTH fills the
                                // width and crops/letterboxes top and bottom
                                // instead, which is what we want here.
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                            }
                        },
                        update = { playerView -> playerView.player = controller },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (!isVideo || controlsVisible) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (!isVideo) {
                            Text(
                                text = currentItem?.name ?: "",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.title3,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { controller?.seekToPrevious() },
                                enabled = hasPrevious,
                                colors = ButtonDefaults.iconButtonColors(),
                            ) {
                                Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = null)
                            }
                            Button(
                                onClick = { controller?.let { if (it.isPlaying) it.pause() else it.play() } },
                                colors = ButtonDefaults.iconButtonColors(),
                                modifier = Modifier.padding(horizontal = 8.dp),
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                )
                            }
                            Button(
                                onClick = { controller?.seekToNext() },
                                enabled = hasNext,
                                colors = ButtonDefaults.iconButtonColors(),
                            ) {
                                Icon(imageVector = Icons.Filled.SkipNext, contentDescription = null)
                            }
                        }
                        if (durationMs > 0) {
                            val progress = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 28.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.25f)),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress)
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colors.primary),
                                )
                            }
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
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun buildStreamUrl(api: ApiClient, item: BaseItemDto, transcode: Boolean): String {
    val deviceId = api.deviceInfo.id
    val base = if (item.mediaType == MediaType.VIDEO) {
        if (transcode) {
            api.videosApi.getVideoStreamUrl(
                itemId = item.id,
                static = false,
                deviceId = deviceId,
                videoBitRate = AppPreferences.TRANSCODE_VIDEO_BITRATE,
                audioBitRate = AppPreferences.TRANSCODE_AUDIO_BITRATE,
            )
        } else {
            api.videosApi.getVideoStreamUrl(itemId = item.id, static = true, deviceId = deviceId)
        }
    } else {
        if (transcode) {
            api.audioApi.getAudioStreamUrl(
                itemId = item.id,
                static = false,
                deviceId = deviceId,
                audioBitRate = AppPreferences.TRANSCODE_AUDIO_BITRATE,
            )
        } else {
            api.audioApi.getAudioStreamUrl(itemId = item.id, static = true, deviceId = deviceId)
        }
    }
    val separator = if (base.contains("?")) "&" else "?"
    return "$base$separator${ApiClient.QUERY_ACCESS_TOKEN}=${api.accessToken}"
}
