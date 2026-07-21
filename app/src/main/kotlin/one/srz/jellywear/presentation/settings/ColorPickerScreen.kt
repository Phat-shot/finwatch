package one.srz.jellywear.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import one.srz.jellywear.R
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.presentation.theme.AccentColorPresets
import one.srz.jellywear.presentation.theme.FontColorPresets

enum class ColorPickerTarget(val route: String) {
    ACCENT("accent"),
    FONT("font"),
    ;

    companion object {
        fun fromRoute(route: String): ColorPickerTarget? = entries.find { it.route == route }
    }
}

@Composable
fun ColorPickerScreen(
    target: ColorPickerTarget,
    preferences: AppPreferences,
    onDone: () -> Unit,
) {
    val presets = if (target == ColorPickerTarget.ACCENT) AccentColorPresets else FontColorPresets
    val titleRes = if (target == ColorPickerTarget.ACCENT) {
        R.string.settings_accent_color
    } else {
        R.string.settings_font_color
    }
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
    ) {
        item {
            ListHeader {
                Text(text = stringResource(titleRes))
            }
        }
        items(presets) { (argb, name) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable {
                        if (target == ColorPickerTarget.ACCENT) {
                            preferences.setAccentColor(argb)
                        } else {
                            preferences.setFontColor(argb)
                        }
                        onDone()
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(argb), CircleShape),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = name)
            }
        }
    }
}
