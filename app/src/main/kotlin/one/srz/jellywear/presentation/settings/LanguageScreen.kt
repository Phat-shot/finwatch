package one.srz.jellywear.presentation.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

data class LanguageOption(val tag: String?, val nativeName: String)

// Language names shown in their own language, not translated -- the
// conventional pattern so users can find their language regardless of
// the app's current locale.
val SupportedLanguages = listOf(
    LanguageOption(null, ""),
    LanguageOption("en", "English"),
    LanguageOption("de", "Deutsch"),
    LanguageOption("fr", "Français"),
    LanguageOption("es", "Español"),
    LanguageOption("ar", "العربية"),
)

@Composable
fun languageDisplayName(tag: String?): String {
    if (tag == null) return stringResource(R.string.theme_mode_system)
    return SupportedLanguages.firstOrNull { it.tag == tag }?.nativeName ?: tag
}

@Composable
fun LanguageScreen(preferences: AppPreferences) {
    val listState = rememberScalingLazyListState()
    val activity = LocalContext.current as? ComponentActivity

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
    ) {
        item {
            ListHeader {
                Text(text = stringResource(R.string.settings_language))
            }
        }
        items(SupportedLanguages) { option ->
            val selected = preferences.languageTag == option.tag
            Chip(
                onClick = {
                    preferences.updateLanguageTag(option.tag)
                    // Locale is applied in attachBaseContext, which only runs
                    // once at Activity creation -- recreate to pick it up now.
                    activity?.recreate()
                },
                label = { Text(text = languageDisplayName(option.tag)) },
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
