package one.srz.jellywear.playback

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.presentation.MainActivity

/**
 * Hosts the ExoPlayer instance and its MediaSession so playback keeps
 * running (with system media controls) after the app leaves the
 * foreground -- e.g. the watch screen turns off while listening.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    // Background playback is the point of this service for normal
    // listening (screen off/ambient while music keeps going). But if the
    // device actually locks -- the user has a lock configured and it just
    // engaged, not just an idle timeout -- assume they're done and stop,
    // freeing the wake lock/foreground service/decoder instead of
    // draining the battery unattended.
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_SCREEN_OFF) return
            val keyguardManager = receiverContext.getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
            if (keyguardManager?.isKeyguardLocked == true) {
                mediaSession.player.stop()
                stopSelf()
            }
        }
    }

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

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build()

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()

        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

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

    override fun onDestroy() {
        getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(preferencesListener)
        unregisterReceiver(screenOffReceiver)
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}
