package one.srz.jellywear.presentation.library

import one.srz.jellywear.R
import org.jellyfin.sdk.model.api.BaseItemKind

enum class Category(val route: String, val titleRes: Int, val itemKind: BaseItemKind) {
    MUSIC("music", R.string.category_music, BaseItemKind.MUSIC_ARTIST),
    AUDIO("audio", R.string.category_audio, BaseItemKind.AUDIO_BOOK),
    SERIES("series", R.string.category_series, BaseItemKind.SERIES),
    MOVIES("movies", R.string.category_movies, BaseItemKind.MOVIE),
    ;

    companion object {
        fun fromRoute(route: String): Category? = entries.find { it.route == route }
    }
}
