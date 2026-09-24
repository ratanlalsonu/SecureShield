package com.example.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color

enum class AppThemePalette(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val primaryColor: Color,
    val accentColor: Color
) {
    CYBER_BLUE(
        id = "cyber_blue",
        displayName = "Cyber Shield",
        subtitle = "Classic cybersecurity deep navy with cyan accents",
        primaryColor = Color(0xFF0D47A1),
        accentColor = Color(0xFF0288D1)
    ),
    EMERALD_SENTINEL(
        id = "emerald_sentinel",
        displayName = "Emerald Guardian",
        subtitle = "High-tech matrix green & mint defense accents",
        primaryColor = Color(0xFF00695C),
        accentColor = Color(0xFF00897B)
    ),
    MIDNIGHT_STEALTH(
        id = "midnight_stealth",
        displayName = "Midnight Stealth",
        subtitle = "Deep obsidian slate with electric indigo glow",
        primaryColor = Color(0xFF4338CA),
        accentColor = Color(0xFF6366F1)
    ),
    CRIMSON_AEGIS(
        id = "crimson_aegis",
        displayName = "Crimson Aegis",
        subtitle = "Vigilant tactical ruby with amber alert highlights",
        primaryColor = Color(0xFFC62828),
        accentColor = Color(0xFFE53935)
    ),
    NORDIC_FROST(
        id = "nordic_frost",
        displayName = "Nordic Frost",
        subtitle = "Clean arctic sapphire & slate executive aesthetic",
        primaryColor = Color(0xFF1E3A8A),
        accentColor = Color(0xFF38BDF8)
    ),
    DYNAMIC_MATERIAL(
        id = "dynamic_material",
        displayName = "Dynamic Material You",
        subtitle = "Adaptive colors derived from device wallpaper (Android 12+)",
        primaryColor = Color(0xFF2563EB),
        accentColor = Color(0xFF38BDF8)
    );

    companion object {
        fun fromId(id: String?): AppThemePalette {
            return values().firstOrNull { it.id == id } ?: CYBER_BLUE
        }
    }
}

enum class ThemeMode(
    val id: String,
    val displayName: String,
    val subtitle: String
) {
    SYSTEM("system", "System Default", "Follows device dark/light schedule"),
    LIGHT("light", "Light Mode", "Crisp day mode with high readability"),
    DARK("dark", "Dark Mode", "Sleek eye-comfort mode for low-light environments");

    companion object {
        fun fromId(id: String?): ThemeMode {
            return values().firstOrNull { it.id == id } ?: SYSTEM
        }
    }
}

data class ThemeConfig(
    val palette: AppThemePalette = AppThemePalette.CYBER_BLUE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledPureBlack: Boolean = false
)

class ThemePreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("secureshield_theme_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PALETTE = "key_theme_palette"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_AMOLED = "key_amoled_pure_black"
    }

    fun getThemeConfig(): ThemeConfig {
        val paletteId = prefs.getString(KEY_PALETTE, AppThemePalette.CYBER_BLUE.id)
        val modeId = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.id)
        val amoled = prefs.getBoolean(KEY_AMOLED, false)

        return ThemeConfig(
            palette = AppThemePalette.fromId(paletteId),
            themeMode = ThemeMode.fromId(modeId),
            amoledPureBlack = amoled
        )
    }

    fun savePalette(palette: AppThemePalette) {
        prefs.edit().putString(KEY_PALETTE, palette.id).apply()
    }

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.id).apply()
    }

    fun saveAmoledPureBlack(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AMOLED, enabled).apply()
    }
}
