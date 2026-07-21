package one.srz.jellywear.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/**
 * User-configurable appearance/behavior settings, persisted in
 * SharedPreferences and exposed as Compose state so the whole app
 * recomposes immediately when something changes in SettingsScreen.
 */
class AppPreferences private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isDarkMode by mutableStateOf(prefs.getBoolean(KEY_DARK_MODE, true))
        private set

    var accentColorArgb by mutableStateOf(prefs.getInt(KEY_ACCENT_COLOR, DEFAULT_ACCENT))
        private set

    var fontColorArgb by mutableStateOf(prefs.getInt(KEY_FONT_COLOR, DEFAULT_FONT_COLOR))
        private set

    var showCoverArt by mutableStateOf(prefs.getBoolean(KEY_COVER_ART, false))
        private set

    fun setDarkMode(value: Boolean) {
        isDarkMode = value
        prefs.edit { putBoolean(KEY_DARK_MODE, value) }
    }

    fun setAccentColor(argb: Int) {
        accentColorArgb = argb
        prefs.edit { putInt(KEY_ACCENT_COLOR, argb) }
    }

    fun setFontColor(argb: Int) {
        fontColorArgb = argb
        prefs.edit { putInt(KEY_FONT_COLOR, argb) }
    }

    fun setShowCoverArt(value: Boolean) {
        showCoverArt = value
        prefs.edit { putBoolean(KEY_COVER_ART, value) }
    }

    companion object {
        private const val PREFS_NAME = "app_preferences"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_FONT_COLOR = "font_color"
        private const val KEY_COVER_ART = "show_cover_art"

        const val DEFAULT_ACCENT = 0xFFCCFF00.toInt()
        const val DEFAULT_FONT_COLOR = 0xFF6F7578.toInt()

        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences =
            instance ?: synchronized(this) {
                instance ?: AppPreferences(context).also { instance = it }
            }
    }
}
