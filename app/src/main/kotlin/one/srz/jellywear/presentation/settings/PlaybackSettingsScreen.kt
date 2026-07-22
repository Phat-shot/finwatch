package one.srz.jellywear.presentation.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import one.srz.jellywear.R
import one.srz.jellywear.data.AppPreferences

@Composable
fun PlaybackSettingsScreen(preferences: AppPreferences) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    val hasBuiltInSpeaker = remember { AppPreferences.hasBuiltInSpeaker(context) }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
    ) {
        item {
            ListHeader {
                Text(text = stringResource(R.string.settings_playback))
            }
        }
        item {
            ToggleChip(
                label = { Text(text = stringResource(R.string.settings_transcode)) },
                checked = preferences.transcodeEnabled,
                toggleControl = { Switch(checked = preferences.transcodeEnabled) },
                onCheckedChange = { preferences.updateTranscodeEnabled(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (hasBuiltInSpeaker) {
            item {
                ToggleChip(
                    label = { Text(text = stringResource(R.string.settings_speaker_output)) },
                    checked = preferences.speakerOutputEnabled,
                    toggleControl = { Switch(checked = preferences.speakerOutputEnabled) },
                    onCheckedChange = { preferences.updateSpeakerOutputEnabled(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
