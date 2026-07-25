package one.srz.jellywear.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Process-wide "is something playing" state, read by the app-level progress
 * ring (drawn above every screen, not just PlayerScreen) so it knows when to
 * show itself and where to seek to. [isActive] is the gate: it only flips to
 * true once PlayerScreen has actually started a session, so merely browsing
 * the library never auto-binds PlaybackService (and its foreground
 * notification) the way an always-on connection attempt would.
 */
object NowPlaying {
    var isActive by mutableStateOf(false)
    var isPlaying by mutableStateOf(false)
    var positionMs by mutableStateOf(0L)
    var durationMs by mutableStateOf(0L)
}
