package one.srz.jellywear.presentation.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import one.srz.jellywear.R
import one.srz.jellywear.data.AppPreferences
import one.srz.jellywear.presentation.library.Category

@Composable
fun LibrarySettingsScreen(preferences: AppPreferences) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
    ) {
        item {
            ListHeader {
                Text(text = stringResource(R.string.settings_libraries))
            }
        }
        items(Category.entries) { category ->
            val visible = preferences.isCategoryVisible(category)
            ToggleChip(
                label = { Text(text = stringResource(category.titleRes)) },
                checked = visible,
                toggleControl = { Switch(checked = visible) },
                onCheckedChange = { preferences.updateCategoryVisibility(category, it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
