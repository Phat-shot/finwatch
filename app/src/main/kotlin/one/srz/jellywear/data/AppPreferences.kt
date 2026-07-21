package one.srz.jellywear.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

enum class ThemeMode { DARK, LIGHT, SYSTEM }

enum class CoverArtMode { OFF, FOLDERS, FOLDERS_AND_PLAYBACK }

/**
 * User-configurable appearance/behavior settings, persisted in
 * SharedPreferences and exposed as Compose state so the whole app
 * recomposes immediately when something changes in SettingsScreen.
 */
class AppPreferences private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(
        ThemeMode.entries.getOrElse(prefs.getInt(KEY_THEME_MODE, ThemeMode.DARK.ordinal)) { ThemeMode.DARK },
    )
        private set

    var accentColorArgb by mutableStateOf(prefs.getInt(KEY_ACCENT_COLOR, DEFAULT_ACCENT))
        private set

    var fontColorArgb by mutableStateOf(prefs.getInt(KEY_FONT_COLOR, DEFAULT_FONT_COLOR))
        private set

    var coverArtMode by mutableStateOf(
        CoverArtMode.entries.getOrElse(prefs.getInt(KEY_COVER_ART_MODE, CoverArtMode.OFF.ordinal)) { CoverArtMode.OFF },
    )
        private set

    /** BCP-47 language tag (e.g. "de"), or null to follow the system language. */
    var languageTag by mutableStateOf(prefs.getString(KEY_LANGUAGE_TAG, null))
        private set

    // Named update* rather than set* -- a `var themeMode ... private set`
    // property already compiles to a JVM setThemeMode(...) accessor, so a
    // same-named function here is a platform signature clash even though
    // the generated one is private.
    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.edit { putInt(KEY_THEME_MODE, mode.ordinal) }
    }

    fun setAccentColor(argb: Int) {
        accentColorArgb = argb
        prefs.edit { putInt(KEY_ACCENT_COLOR, argb) }
    }

    fun setFontColor(argb: Int) {
        fontColorArgb = argb
        prefs.edit { putInt(KEY_FONT_COLOR, argb) }
    }

    fun updateCoverArtMode(mode: CoverArtMode) {
        coverArtMode = mode
        prefs.edit { putInt(KEY_COVER_ART_MODE, mode.ordinal) }
    }

    fun updateLanguageTag(tag: String?) {
        languageTag = tag
        prefs.edit { putString(KEY_LANGUAGE_TAG, tag) }
    }

    companion object {
        private const val PREFS_NAME = "app_preferences"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_FONT_COLOR = "font_color"
        private const val KEY_COVER_ART_MODE = "cover_art_mode"
        private const val KEY_LANGUAGE_TAG = "language_tag"

        // 10% less green than the original #CCFF00.
        const val DEFAULT_ACCENT = 0xFFCCE600.toInt()
        const val DEFAULT_FONT_COLOR = 0xFF6F7578.toInt()

        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences =
            instance ?: synchronized(this) {
                instance ?: AppPreferences(context).also { instance = it }
            }
    }
}
