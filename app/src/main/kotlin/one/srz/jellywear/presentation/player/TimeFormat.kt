package one.srz.jellywear.presentation.player

/**
 * Formats a playback position/duration as "m:ss" for the player's
 * position readout. Extracted from PlayerScreen so it is unit-testable.
 *
 * Deliberately preserves the current behavior: minutes keep counting past
 * 59 instead of rolling over into an hours segment (a 2h movie reads
 * "120:00") -- changing that is tracked separately in issue #26.
 */
internal fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
