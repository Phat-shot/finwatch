package one.srz.jellywear.playback

import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * Holds a shuffled play queue built by a long-press ("shuffle this") action,
 * read by PlayerScreen when navigated to via the "player/queue" route.
 * Process-scoped, not persisted -- it only needs to survive the navigation
 * from the long-press to the player screen.
 */
object PlaybackQueue {
    var items: List<BaseItemDto> = emptyList()
}
