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
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.data.JellyfinSession

@Composable
fun SettingsScreen(
    session: JellyfinSession,
    preferences: AppPreferences,
    onOpenThemeModePicker: () -> Unit,
    onOpenCoverArtModePicker: () -> Unit,
    onOpenAccentColorPicker: () -> Unit,
    onOpenFontColorPicker: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val listState = rememberScalingLazyListState()

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
        item {
            Chip(
                onClick = onOpenThemeModePicker,
                label = { Text(text = stringResource(R.string.settings_theme_mode)) },
                secondaryLabel = { Text(text = stringResource(preferences.themeMode.labelRes())) },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Chip(
                onClick = onOpenCoverArtModePicker,
                label = { Text(text = stringResource(R.string.settings_cover_art)) },
                secondaryLabel = { Text(text = stringResource(preferences.coverArtMode.labelRes())) },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Chip(
                onClick = onOpenAccentColorPicker,
                label = { Text(text = stringResource(R.string.settings_accent_color)) },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Chip(
                onClick = onOpenFontColorPicker,
                label = { Text(text = stringResource(R.string.settings_font_color)) },
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
    }
}
