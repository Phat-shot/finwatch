package one.srz.jellywear.presentation.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import one.srz.jellywear.BuildConfig
import one.srz.jellywear.R
import one.srz.jellywear.data.JellyfinSession
import one.srz.jellywear.presentation.ScrollIndicatorScaffold

@Composable
fun SettingsScreen(
    session: JellyfinSession,
    onOpenAppearance: () -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenLibraries: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val listState = rememberScalingLazyListState()

    ScrollIndicatorScaffold(state = listState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
        ) {
            item {
                ListHeader {
                    Text(text = stringResource(R.string.settings_title))
                }
            }
            item {
                Chip(
                    onClick = onOpenAppearance,
                    label = { Text(text = stringResource(R.string.settings_appearance)) },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Chip(
                    onClick = onOpenPlayback,
                    label = { Text(text = stringResource(R.string.settings_playback)) },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Chip(
                    onClick = onOpenLibraries,
                    label = { Text(text = stringResource(R.string.settings_libraries)) },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Chip(
                    onClick = {
                        session.logout()
                        onLoggedOut()
                    },
                    label = { Text(text = stringResource(R.string.settings_logout)) },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    text = stringResource(R.string.settings_server, session.serverUrl ?: "-"),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    text = stringResource(R.string.settings_account, session.username ?: "-"),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
