package one.srz.jellywear.playback

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.data.JellyfinSession
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
// @OptIn instead of @UnstableApi: the unstable surface (DefaultDataSource,
// DefaultHttpDataSource, DefaultMediaSourceFactory) is an implementation
// detail of this class. @UnstableApi would propagate to every reference to
// PlaybackService (MainActivity, PlayerScreen), each then failing lint's
// UnsafeOptInUsageError; @OptIn accepts the risk here without spreading it.
@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

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

        // Stream URLs are token-free (see PlayerScreen.buildStreamUrl) --
        // authenticate every HTTP request via the Authorization header
        // instead, so the token never shows up in URLs that ExoPlayer might
        // log (e.g. inside a PlaybackException on a network error).
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        JellyfinSession.getInstance(this).authorizationHeader()?.let { header ->
            httpDataSourceFactory.setDefaultRequestProperties(mapOf("Authorization" to header))
        }
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(DefaultDataSource.Factory(this, httpDataSourceFactory)),
            )
            .build()
        player.addListener(playerListener)

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).putExtra(EXTRA_OPEN_NOW_PLAYING, true),
            PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()

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
