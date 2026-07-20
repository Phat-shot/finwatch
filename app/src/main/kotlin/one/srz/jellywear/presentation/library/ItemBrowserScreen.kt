package one.srz.jellywear.presentation.library

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import one.srz.jellywear.R
import one.srz.jellywear.data.JellyfinSession
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

@Composable
fun ItemBrowserScreen(
    session: JellyfinSession,
    parentId: String,
    onOpenFolder: (String) -> Unit,
    onPlayItem: (String) -> Unit,
) {
    var children by remember(parentId) { mutableStateOf<List<BaseItemDto>?>(null) }
    var error by remember(parentId) { mutableStateOf<String?>(null) }

    LaunchedEffect(parentId) {
        val api = session.api ?: return@LaunchedEffect
        try {
            children = api.itemsApi.getItems(
                GetItemsRequest(
                    userId = session.userId,
                    parentId = parentId.toUUIDOrNull(),
                    recursive = false,
                    sortBy = listOf(ItemSortBy.SORT_NAME),
                ),
            ).content.items
        } catch (e: ApiClientException) {
            error = e.message
        }
    }

    ScalingLazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            ListHeader {
                Text(text = stringResource(R.string.library_title))
            }
        }
        when {
            error != null -> item {
                Text(text = error ?: stringResource(R.string.login_error_generic))
            }
            children == null -> item {
                Text(text = stringResource(R.string.library_loading))
            }
            children.orEmpty().isEmpty() -> item {
                Text(text = stringResource(R.string.library_empty))
            }
            else -> items(children.orEmpty()) { child ->
                Chip(
                    onClick = {
                        val id = child.id.toString()
                        if (child.isFolder == true) onOpenFolder(id) else onPlayItem(id)
                    },
                    label = { Text(text = child.name ?: "?") },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
