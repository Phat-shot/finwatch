package one.srz.jellywear.playback

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.google.common.collect.ImmutableList
import one.srz.jellywear.R
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.presentation.MainActivity

// Bounds how many times in a row onPlayerError may auto-recover the same
// item before giving up -- otherwise a genuinely unplayable stream (e.g. a
// codec the watch can't decode) would retry forever instead of surfacing as
// stopped playback.
private const val MAX_CONSECUTIVE_AUTO_RETRIES = 3

// Set on the session's activity PendingIntent so tapping the notification or
// the Wear OS watch-face playback icon opens straight into the now-playing
// screen (PLAYER_RESUME_ID) instead of just launching the app to its normal
// home screen.
const val EXTRA_OPEN_NOW_PLAYING = "one.srz.jellywear.EXTRA_OPEN_NOW_PLAYING"

/**
 * Hosts the ExoPlayer instance and its MediaSession so playback keeps
 * running (with system media controls) after the app leaves the
 * foreground -- e.g. the watch screen turns off while listening.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var sessionActivity: PendingIntent

    // Applies the Settings > speaker-output toggle live, including while
    // something is already playing -- this Service can't observe
    // AppPreferences' Compose state directly, but it's backed by the same
    // SharedPreferences file the UI writes to, so a plain change listener
    // works across the two.
    private val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == AppPreferences.KEY_SPEAKER_OUTPUT) {
            applyPreferredAudioDevice(prefs.getBoolean(key, false))
        }
    }

    // Recovers from transient playback failures (e.g. a network blip over
    // Bluetooth tethering) that would otherwise just halt playback until the
    // user manually hits play again, which read as the video repeatedly
    // "stopping".
    private var consecutiveAutoRetries = 0
    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            if (consecutiveAutoRetries >= MAX_CONSECUTIVE_AUTO_RETRIES) return
            consecutiveAutoRetries++
            val index = player.currentMediaItemIndex
            val position = player.currentPosition
            player.prepare()
            player.seekTo(index, position)
            player.play()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) consecutiveAutoRetries = 0
        }
    }

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build()
        player.addListener(playerListener)

        sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).putExtra(EXTRA_OPEN_NOW_PLAYING, true),
            PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()

        // Wear OS app-quality requirement: media playback from a foreground
        // service must be surfaced as an Ongoing Activity (watch-face chip +
        // Recents entry) for as long as the notification exists.
        setMediaNotificationProvider(OngoingActivityNotificationProvider())

        val preferences = AppPreferences.getInstance(this)
        applyPreferredAudioDevice(preferences.speakerOutputEnabled)
        getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(preferencesListener)
    }

    private fun applyPreferredAudioDevice(useSpeaker: Boolean) {
        val speaker = if (useSpeaker) AppPreferences.findBuiltInSpeaker(this) else null
        player.setPreferredAudioDevice(speaker)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    /**
     * Media3's stock notification pipeline, with a Wear OS Ongoing Activity
     * attached on top (issue #26 / Play's Wear quality requirements).
     *
     * Delegates the actual media notification to
     * [DefaultMediaNotificationProvider] so channel creation, transport
     * actions and artwork keep working exactly as before, then marks the
     * result with [OngoingActivity] so the watch face shows a playback chip
     * (status text = current item's title, tap = back into the now-playing
     * screen via [EXTRA_OPEN_NOW_PLAYING]). The chip's lifetime follows the
     * media notification's, which is exactly what the guideline asks for:
     * it appears when playback starts, may stay through a pause (the
     * notification stays up, just dismissible), and is gone once playback
     * stops and Media3 cancels the notification.
     */
    private inner class OngoingActivityNotificationProvider : MediaNotification.Provider {
        private val delegate = DefaultMediaNotificationProvider(this@PlaybackService)

        override fun createNotification(
            mediaSession: MediaSession,
            customLayout: ImmutableList<CommandButton>,
            actionFactory: MediaNotification.ActionFactory,
            onNotificationChangedCallback: MediaNotification.Provider.Callback,
        ): MediaNotification {
            val notification = delegate.createNotification(
                mediaSession,
                customLayout,
                actionFactory,
            ) { updated ->
                // Async updates (e.g. artwork finished loading) bypass the
                // synchronous return path, so they need the Ongoing Activity
                // re-applied too or the chip's data would be dropped.
                onNotificationChangedCallback.onNotificationChanged(withOngoingActivity(mediaSession, updated))
            }
            return withOngoingActivity(mediaSession, notification)
        }

        override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean =
            delegate.handleCustomCommand(session, action, extras)

        private fun withOngoingActivity(
            mediaSession: MediaSession,
            mediaNotification: MediaNotification,
        ): MediaNotification {
            // OngoingActivity can only be applied to a NotificationCompat.Builder,
            // but the default provider hands back an already-built Notification --
            // recover a Builder from it (androidx.core keeps texts, actions,
            // icons, extras). The one thing recovery can't restore is Media3's
            // MediaStyle (a compat style androidx.core doesn't know), so re-apply
            // that explicitly to keep the media look and session link intact.
            val builder = NotificationCompat.Builder(this@PlaybackService, mediaNotification.notification)
                .setStyle(MediaStyleNotificationHelper.MediaStyle(mediaSession))
            val title = mediaSession.player.mediaMetadata.title?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: getString(R.string.app_name)
            OngoingActivity.Builder(applicationContext, mediaNotification.notificationId, builder)
                .setStaticIcon(R.drawable.ic_ongoing_playback)
                .setTouchIntent(sessionActivity)
                .setStatus(Status.forPart(Status.TextPart(title)))
                .build()
                .apply(applicationContext)
            return MediaNotification(mediaNotification.notificationId, builder.build())
        }
    }

    // Default MediaSessionService behavior keeps the service (and its
    // notification) alive across a task removal as long as something is
    // still playing. The user wants the opposite: swiping the app away is an
    // explicit "I'm done", so playback and the notification should always go
    // away with it, audio included -- only backgrounding without closing the
    // app (handled client-side in PlayerScreen) is allowed to keep audio
    // going.
    override fun onTaskRemoved(rootIntent: Intent?) {
        pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(preferencesListener)
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}
