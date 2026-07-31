package one.srz.jellywear.presentation.player

import android.content.ComponentName
import android.content.Intent
import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
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
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.delay
import one.srz.jellywear.R
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.data.CoverArtMode
import one.srz.jellywear.data.JellyfinSession
import one.srz.jellywear.playback.NowPlaying
import one.srz.jellywear.playback.PlaybackQueue
import one.srz.jellywear.playback.PlaybackService
import one.srz.jellywear.presentation.MainActivity
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.exception.SecureConnectionException
import org.jellyfin.sdk.api.client.exception.TimeoutException
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

// PlayerView and AspectRatioFrameLayout (media3-ui) are @UnstableApi; the
// opt-in stays function-local instead of propagating to callers.
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(session: JellyfinSession, preferences: AppPreferences, itemId: String) {
    val context = LocalContext.current
    var queueItems by remember(itemId) { mutableStateOf<List<BaseItemDto>>(emptyList()) }
    // String resource id, not a raw exception message -- the watch shows a
    // short translated line, never technical text (see errorStringRes).
    var errorRes by remember(itemId) { mutableStateOf<Int?>(null) }
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
    // Where to restart playback when the queue is handed to the controller
    // again. stopIfVideo() (ON_STOP) tears the whole service down, so without
    // this a backgrounded video always restarted at 0:00 on return.
    // rememberSaveable so the position even survives the activity being
    // recreated (or the process dying) while backgrounded. C.TIME_UNSET means
    // "default position", i.e. an untouched fresh start.
    var startMediaItemIndex by rememberSaveable(itemId) { mutableStateOf(0) }
    var startPositionMs by rememberSaveable(itemId) { mutableStateOf(C.TIME_UNSET) }
    var controlsVisible by remember(itemId) { mutableStateOf(true) }
    var interactionTick by remember(itemId) { mutableStateOf(0) }
    // Resume-route fallback (see the PLAYER_RESUME_ID effect below): true
    // once the UI is fed from the connected controller's own metadata
    // because the process-local PlaybackQueue came up empty.
    var resumeFromController by remember(itemId) { mutableStateOf(false) }
    var controllerMetadata by remember(itemId) { mutableStateOf<MediaMetadata?>(null) }

    val currentItem = queueItems.getOrNull(currentIndex)
    // In controller-fallback resume mode there is no BaseItemDto to ask, so
    // the video/audio decision comes from the mediaType each MediaItem's
    // metadata was stamped with when the queue was handed to the controller.
    val isVideo = if (currentItem != null) {
        currentItem.mediaType == MediaType.VIDEO
    } else {
        controllerMetadata?.mediaType == MediaMetadata.MEDIA_TYPE_MOVIE
    }

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
                // Remember where the video was interrupted so the reload on
                // the next ON_START resumes there instead of at 0:00.
                controller?.let { ctrl ->
                    startMediaItemIndex = ctrl.currentMediaItemIndex
                    startPositionMs = ctrl.currentPosition
                }
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
                // Only claim an active session when there is actually one to
                // show; the empty case is resolved by the fallback effect
                // below once the controller has connected.
                if (queueItems.isNotEmpty()) NowPlaying.isActive = true
            }
            else -> {
                val api = session.api ?: return@LaunchedEffect
                // Today itemId is always an internally produced UUID, but a
                // malformed one (e.g. a future deeplink) used to crash with
                // an uncaught IllegalArgumentException -- degrade to the
                // generic error state instead.
                val itemUuid = try {
                    itemId.toUUID()
                } catch (e: IllegalArgumentException) {
                    errorRes = R.string.error_generic
                    return@LaunchedEffect
                }
                try {
                    val item = api.userLibraryApi.getItem(itemId = itemUuid, userId = session.userId).content
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
                    errorRes = errorStringRes(e)
                }
            }
        }
    }

    // Resume entry whose process-local queue is empty: the process died (or
    // was otherwise reset) while the media notification / watch-face chip
    // stayed visible, so PlaybackQueue has nothing although the entry point
    // promised something playing. Previously this showed a spinner forever.
    // Fall back to whatever the connected controller still carries; if that
    // is empty too (the controller connection just started a fresh, empty
    // service), leave for Home.
    if (itemId == PLAYER_RESUME_ID) {
        LaunchedEffect(controller, queueItems) {
            val ctrl = controller ?: return@LaunchedEffect
            if (queueItems.isNotEmpty() || resumeFromController) return@LaunchedEffect
            if (ctrl.mediaItemCount > 0) {
                resumeFromController = true
                controllerMetadata = ctrl.mediaMetadata
                NowPlaying.isActive = true
            } else {
                // PlayerScreen has no NavController to pop, so go Home by
                // relaunching MainActivity without EXTRA_OPEN_NOW_PLAYING:
                // CLEAR_TASK rebuilds the task on the normal Home start
                // destination (this screen is usually the task's only entry
                // here anyway, being the notification-tap start destination).
                context.startActivity(
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                )
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
                            // Lets a later controller-fallback resume (queue
                            // lost to process death) still tell video from
                            // audio without the BaseItemDto at hand.
                            .setMediaType(
                                if (queueItem.mediaType == MediaType.VIDEO) {
                                    MediaMetadata.MEDIA_TYPE_MOVIE
                                } else {
                                    MediaMetadata.MEDIA_TYPE_MUSIC
                                },
                            )
                            .build(),
                    )
                    .build()
            }
            // coerceIn guards against a stale saved index (e.g. the queue
            // shrank to a single refetched item after process death).
            ctrl.setMediaItems(
                mediaItems,
                startMediaItemIndex.coerceIn(0, mediaItems.lastIndex),
                startPositionMs,
            )
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

                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    // Keeps the controller-fallback resume UI (title, video
                    // detection) current across item transitions; harmless
                    // otherwise, it's only read when the queue is empty.
                    controllerMetadata = mediaMetadata
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    // STATE_ENDED too: when the queue simply finishes, the
                    // global ring would otherwise sit at 100% over every
                    // screen (and keep JellywearApp's ring controller bound
                    // and polling) until the process dies.
                    if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                        NowPlaying.isActive = false
                    }
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
    val keepScreenOn = (currentItem == null && !resumeFromController) || isVideo
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
    val activity = LocalActivity.current
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
            errorRes != null -> Text(
                text = stringResource(errorRes ?: R.string.error_generic),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
            queueItems.isEmpty() && !resumeFromController -> CircularProgressIndicator()
            else -> {
                if (!isVideo &&
                    preferences.coverArtMode == CoverArtMode.FOLDERS_AND_PLAYBACK &&
                    currentItem != null
                ) {
                    AsyncImage(
                        model = session.imageUrl(currentItem.id),
                        contentDescription = currentItem.name,
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
                                text = currentItem?.name
                                    ?: controllerMetadata?.title?.toString()
                                    ?: "",
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
                                Icon(
                                    imageVector = Icons.Filled.SkipPrevious,
                                    contentDescription = stringResource(R.string.player_previous),
                                )
                            }
                            Button(
                                onClick = { controller?.let { if (it.isPlaying) it.pause() else it.play() } },
                                colors = ButtonDefaults.iconButtonColors(),
                                modifier = Modifier.padding(horizontal = 8.dp),
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = stringResource(
                                        if (isPlaying) R.string.player_pause else R.string.player_play,
                                    ),
                                )
                            }
                            Button(
                                onClick = { controller?.seekToNext() },
                                enabled = hasNext,
                                colors = ButtonDefaults.iconButtonColors(),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = stringResource(R.string.player_next),
                                )
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

// Maps SDK failures onto the shared error strings so the watch shows a short
// translated line instead of a raw (technical, English-only) exception
// message.
private fun errorStringRes(e: ApiClientException): Int = when (e) {
    // Host unreachable / network offline; TLS failures grouped in here too,
    // as on the watch they almost always mean the connection path is broken.
    is TimeoutException, is SecureConnectionException -> R.string.error_network
    is InvalidStatusException -> when (e.status) {
        401, 403 -> R.string.error_auth
        else -> R.string.error_server
    }
    else -> if (e.cause is IOException) R.string.error_network else R.string.error_generic
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
    // Deliberately no access token in the URL: PlaybackService authenticates
    // its HTTP requests with the Authorization header instead (see its
    // ExoPlayer data-source factory), keeping the token out of error logs
    // and anything else that captures URLs.
    return base
}
