package one.srz.jellywear.presentation.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import one.srz.jellywear.R
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.data.ThemeMode

fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.DARK -> R.string.theme_mode_dark
    ThemeMode.LIGHT -> R.string.theme_mode_light
    ThemeMode.SYSTEM -> R.string.theme_mode_system
}

@Composable
fun ThemeModeScreen(preferences: AppPreferences, onDone: () -> Unit) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
    ) {
        item {
            ListHeader {
                Text(text = stringResource(R.string.settings_theme_mode))
            }
        }
        items(ThemeMode.entries) { mode ->
            Chip(
                onClick = {
                    preferences.updateThemeMode(mode)
                    onDone()
                },
                label = { Text(text = stringResource(mode.labelRes())) },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
