package one.srz.jellywear.presentation.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import one.srz.jellywear.R
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.presentation.theme.JellyfinBlue
import one.srz.jellywear.presentation.theme.LightGray

@Composable
fun AppearanceSettingsScreen(
    preferences: AppPreferences,
    onOpenThemeModePicker: () -> Unit,
    onOpenCoverArtModePicker: () -> Unit,
    onOpenAccentColorPicker: () -> Unit,
    onOpenFontColorPicker: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
    ) {
        item {
            ListHeader {
                Text(text = stringResource(R.string.settings_appearance))
            }
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
                onClick = onOpenLanguagePicker,
                label = { Text(text = stringResource(R.string.settings_language)) },
                secondaryLabel = { Text(text = languageDisplayName(preferences.languageTag)) },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            // One-tap preset, not a persisted toggle: applies Jellyfin's own
            // blue accent + light-gray text on top of the already-black
            // background, then leaves the two pickers above free to fine-tune
            // further. Opt-in on purpose -- nothing about a fresh install
            // (beta or prod) switches to this look on its own.
            Chip(
                onClick = {
                    preferences.setAccentColor(JellyfinBlue.toArgb())
                    preferences.setFontColor(LightGray.toArgb())
                },
                label = { Text(text = stringResource(R.string.settings_jellyfin_theme)) },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
