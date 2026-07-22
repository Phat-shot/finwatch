package one.srz.jellywear.data

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.content.getSystemService
import one.srz.jellywear.presentation.library.Category

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

    /** Server-side transcode to a watch-appropriate bitrate instead of direct play. */
    var transcodeEnabled by mutableStateOf(prefs.getBoolean(KEY_TRANSCODE, false))
        private set

    /** Route audio to the watch's built-in speaker instead of a connected Bluetooth device. */
    var speakerOutputEnabled by mutableStateOf(prefs.getBoolean(KEY_SPEAKER_OUTPUT, false))
        private set

    /** Which category tiles show up on the compact launcher (Settings > Libraries). All shown by default. */
    var visibleCategories by mutableStateOf(loadVisibleCategories(prefs))
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

    fun updateTranscodeEnabled(value: Boolean) {
        transcodeEnabled = value
        prefs.edit { putBoolean(KEY_TRANSCODE, value) }
    }

    fun updateSpeakerOutputEnabled(value: Boolean) {
        speakerOutputEnabled = value
        prefs.edit { putBoolean(KEY_SPEAKER_OUTPUT, value) }
    }

    fun isCategoryVisible(category: Category): Boolean = category in visibleCategories

    fun updateCategoryVisibility(category: Category, visible: Boolean) {
        visibleCategories = if (visible) visibleCategories + category else visibleCategories - category
        prefs.edit { putStringSet(KEY_VISIBLE_CATEGORIES, visibleCategories.map { it.route }.toSet()) }
    }

    companion object {
        // Exposed so PlaybackService can listen for changes to this file
        // directly (SharedPreferences.OnSharedPreferenceChangeListener)
        // without depending on this class's Compose state, which a
        // background Service can't observe.
        const val PREFS_NAME = "app_preferences"
        const val KEY_SPEAKER_OUTPUT = "speaker_output_enabled"

        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_FONT_COLOR = "font_color"
        private const val KEY_COVER_ART_MODE = "cover_art_mode"
        private const val KEY_LANGUAGE_TAG = "language_tag"
        private const val KEY_TRANSCODE = "transcode_enabled"
        private const val KEY_VISIBLE_CATEGORIES = "visible_categories"

        // Sized for a watch screen and typically Bluetooth-tethered
        // bandwidth: well past the point of visible improvement at this
        // display size, comfortably below what most phone-tethered
        // connections struggle with.
        const val TRANSCODE_VIDEO_BITRATE = 1_500_000
        const val TRANSCODE_AUDIO_BITRATE = 128_000

        // 10% less green than the original #CCFF00.
        const val DEFAULT_ACCENT = 0xFFCCE600.toInt()
        const val DEFAULT_FONT_COLOR = 0xFF6F7578.toInt()

        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences =
            instance ?: synchronized(this) {
                instance ?: AppPreferences(context).also { instance = it }
            }

        /** The watch's built-in speaker output device, or null if it doesn't have one. */
        fun findBuiltInSpeaker(context: Context): AudioDeviceInfo? {
            val audioManager = context.getSystemService<AudioManager>() ?: return null
            return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        }

        fun hasBuiltInSpeaker(context: Context): Boolean = findBuiltInSpeaker(context) != null

        // SharedPreferences.getStringSet's returned set must not be held onto/mutated
        // directly (the framework may reuse or mutate it internally) -- mapping it into
        // a fresh Set<Category> up front avoids that aliasing bug and drops any
        // now-unknown routes.
        private fun loadVisibleCategories(prefs: SharedPreferences): Set<Category> {
            val storedRoutes = prefs.getStringSet(KEY_VISIBLE_CATEGORIES, null)
                ?: return Category.entries.toSet()
            return storedRoutes.mapNotNull(Category::fromRoute).toSet()
        }
    }
}
