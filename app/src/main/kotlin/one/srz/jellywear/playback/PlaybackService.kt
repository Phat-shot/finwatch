package one.srz.jellywear.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import one.srz.jellywear.presentation.MainActivity

/**
 * Hosts the ExoPlayer instance and its MediaSession so playback keeps
 * running (with system media controls) after the app leaves the
 * foreground -- e.g. the watch screen turns off while listening.
 */
class PlaybackService : MediaSessionService() {
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this).build()

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onDestroy() {
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}
