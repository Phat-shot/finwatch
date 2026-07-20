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
import org.jellyfin.sdk.api.client.extensions.userViewApi
import org.jellyfin.sdk.model.api.BaseItemDto

@Composable
fun LibraryScreen(session: JellyfinSession) {
    var libraries by remember { mutableStateOf<List<BaseItemDto>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val api = session.api ?: return@LaunchedEffect
        try {
            libraries = api.userViewApi.getUserViews().content.items
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
            libraries == null -> item {
                Text(text = stringResource(R.string.library_loading))
            }
            libraries.orEmpty().isEmpty() -> item {
                Text(text = stringResource(R.string.library_empty))
            }
            else -> items(libraries.orEmpty()) { library ->
                // TODO: browsing into a library (artists/albums/songs) and
                // audio playback land in a follow-up increment.
                Chip(
                    onClick = { },
                    label = { Text(text = library.name ?: "?") },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
