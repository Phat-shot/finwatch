package one.srz.jellywear.presentation.player

import android.content.ComponentName
import android.content.Intent
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CancellationException
import kotlinx.coroutines.delay
import one.srz.jellywear.R
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.data.CoverArtMode
import one.srz.jellywear.data.JellyfinSession
import one.srz.jellywear.playback.NowPlaying
import one.srz.jellywear.playback.PlaybackQueue
import one.srz.jellywear.playback.PlaybackService
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.audioApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.serializer.toUUID

const val PLAYER_QUEUE_ID = "queue"

// Opened from the notification / Wear OS watch-face playback icon: reattach
// to whatever PlaybackService already has loaded instead of loading a queue,
// since something must already be playing for that entry point to exist.
const val PLAYER_RESUME_ID = "resume"

private const val CONTROLS_AUTO_HIDE_MS = 3000L
private const val AUDIO_AUTO_BACKGROUND_MS = 10_000L

// Audio codecs the watch can decode directly. Anything else (AC3/EAC3, DTS,
// TrueHD, ...) direct-plays picture-only -- the codec support Jellyfin
// negotiates via a full device profile, which this app doesn't build, so we
// approximate it here from the item's own stream info instead.
private val COMPATIBLE_AUDIO_CODECS = setOf("aac", "mp3", "flac", "opus", "vorbis")

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

    val currentItem = queueItems.getOrNull(currentIndex)
    val isVideo = currentItem?.mediaType == MediaType.VIDEO

    // The global ring (drawn in JellywearApp) mirrors these so it can fade
    // in/out together with the video controls instead of always sitting on
    // top of the video.
    SideEffect {
        NowPlaying.isVideo = isVideo
        NowPlaying.controlsVisible = controlsVisible
    }

    // Connects to (and starts, if needed) PlaybackService so playback keeps
    // running in the background via its MediaSession. Reconnects on every
    // ON_START (app returning to the foreground), not just once, since video
    // gets its whole service killed on ON_STOP below and needs a fresh
    // connection -- audio's service instead just keeps running unattended.
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentIsVideo = rememberUpdatedState(isVideo)
    DisposableEffect(itemId, lifecycleOwner) {
        var controllerFuture: ListenableFuture<MediaController>? = null

        fun connect() {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture = future
            future.addListener(
                {
                    // Reconnecting on every ON_START (see below) means this
                    // future gets cancelled by releaseController() far more
                    // often than the original one-shot connection did -- a
                    // listener still fires on cancellation, where get() throws.
                    controller = try {
                        future.get()
                    } catch (e: CancellationException) {
                        null
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }

        fun releaseController() {
            controllerFuture?.let { MediaController.releaseFuture(it) }
            controllerFuture = null
            controller = null
        }

        // Video has nothing to show once it isn't the visible screen --
        // unlike audio (meant to keep going in the background/notification),
        // kill playback and the whole service outright instead of leaving it
        // running unseen. hasSetMediaItems resets too, so reconnecting later
        // (a fresh, empty service) reloads the queue instead of staying blank.
        fun stopIfVideo() {
            if (currentIsVideo.value) {
                controller?.stop()
                context.stopService(Intent(context, PlaybackService::class.java))
                hasSetMediaItems = false
                // Video has nothing left to show anyone once its service is
                // gone -- hide the global ring instead of leaving it stuck at
                // a stale position.
                NowPlaying.isActive = false
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> connect()
                Lifecycle.Event.ON_STOP -> {
                    stopIfVideo()
                    releaseController()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopIfVideo()
            releaseController()
        }
    }

    LaunchedEffect(itemId) {
        when (itemId) {
            PLAYER_QUEUE_ID -> queueItems = PlaybackQueue.items
            // Just reattaching to what's already loaded in PlaybackService --
            // it's kept around for display (title, video/audio detection)
            // but never handed to setMediaItems below.
            PLAYER_RESUME_ID -> {
                queueItems = PlaybackQueue.items
                hasSetMediaItems = true
                NowPlaying.isActive = true
            }
            else -> {
                val api = session.api ?: return@LaunchedEffect
                try {
                    val item = api.userLibraryApi.getItem(itemId = itemId.toUUID(), userId = session.userId).content
                    queueItems = listOf(item)
                    // PLAYER_RESUME_ID (notification/watch-face tap, or
                    // reopening the app while this is playing) reattaches by
                    // reading PlaybackQueue.items -- without this, only the
                    // shuffle-play paths that already write to it would ever
                    // have anything to resume into, leaving a plain single-item
                    // play (by far the common case) stuck on the loading spinner
                    // forever once backgrounded and reopened.
                    PlaybackQueue.items = listOf(item)
                } catch (e: ApiClientException) {
                    error = e.message
                }
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
            NowPlaying.isActive = true
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
                    NowPlaying.isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_IDLE) NowPlaying.isActive = false
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
            NowPlaying.positionMs = positionMs
            NowPlaying.durationMs = durationMs
            delay(500)
        }
    }

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
                            // The global ring (JellywearApp/ProgressRing) is
                            // purely decorative now and easy to miss over a
                            // busy video frame, so video gets its own bar
                            // back here -- audio keeps just the time readout,
                            // its ring is normally the only thing on screen.
                            if (isVideo) {
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
                            }
                            Text(
                                text = "${formatMillis(positionMs)} / ${formatMillis(durationMs)}",
                                style = MaterialTheme.typography.caption2,
                                modifier = Modifier.padding(top = 8.dp),
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
                audioCodec = "aac",
            )
        } else {
            val streams = item.mediaStreams
            val audioCodec = streams?.firstOrNull { it.type == MediaStreamType.AUDIO }?.codec?.lowercase()
            if (streams != null && audioCodec != null && audioCodec !in COMPATIBLE_AUDIO_CODECS) {
                // Direct play would be picture-only: remux the container and
                // transcode just the audio track to AAC, leaving the (already
                // watch-compatible) video stream copied as-is.
                val videoCodec = streams.firstOrNull { it.type == MediaStreamType.VIDEO }?.codec
                api.videosApi.getVideoStreamUrl(
                    itemId = item.id,
                    static = false,
                    deviceId = deviceId,
                    videoCodec = videoCodec,
                    audioCodec = "aac",
                )
            } else {
                api.videosApi.getVideoStreamUrl(itemId = item.id, static = true, deviceId = deviceId)
            }
        }
    } else {
        // Transcoding only ever applies to video -- audio-only items always direct play.
        api.audioApi.getAudioStreamUrl(itemId = item.id, static = true, deviceId = deviceId)
    }
    val separator = if (base.contains("?")) "&" else "?"
    return "$base$separator${ApiClient.QUERY_ACCESS_TOKEN}=${api.accessToken}"
}
