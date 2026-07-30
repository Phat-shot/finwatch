package one.srz.jellywear.presentation.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import one.srz.jellywear.R
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.data.CoverArtMode

fun CoverArtMode.labelRes(): Int = when (this) {
    CoverArtMode.OFF -> R.string.cover_art_mode_off
    CoverArtMode.FOLDERS -> R.string.cover_art_mode_folders
    CoverArtMode.FOLDERS_AND_PLAYBACK -> R.string.cover_art_mode_folders_and_playback
}

@Composable
fun CoverArtModeScreen(preferences: AppPreferences, onDone: () -> Unit) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
    ) {
        item {
            ListHeader {
                Text(text = stringResource(R.string.settings_cover_art))
            }
        }
        items(CoverArtMode.entries) { mode ->
            val selected = preferences.coverArtMode == mode
            Chip(
                onClick = {
                    preferences.updateCoverArtMode(mode)
                    onDone()
                },
                label = { Text(text = stringResource(mode.labelRes())) },
                icon = if (selected) {
                    { Icon(imageVector = Icons.Filled.Check, contentDescription = stringResource(R.string.selected)) }
                } else {
                    null
                },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
