package com.soniq.player.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soniq.player.core.common.enumValueOrDefault
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.BackgroundStyle
import com.soniq.player.core.model.appearance.FontFamilyOption
import com.soniq.player.core.model.appearance.PlayerLayout
import com.soniq.player.core.model.appearance.TextScale
import com.soniq.player.core.model.appearance.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists [AppearanceConfig] in Preferences DataStore.
 *
 * Enum values are stored by name and parsed defensively so that renamed or removed constants
 * never crash the app; unreadable storage falls back to defaults.
 */
@Singleton
class AppearancePreferencesDataSource @Inject constructor(
    @param:AppearanceDataStore private val dataStore: DataStore<Preferences>,
) {

    val appearance: Flow<AppearanceConfig> = dataStore.data
        .catch { error ->
            // A read failure must not take down the UI; render defaults instead.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences.toAppearanceConfig() }
        .distinctUntilChanged()

    /** Atomically transforms the stored appearance. */
    suspend fun update(transform: (AppearanceConfig) -> AppearanceConfig) {
        dataStore.edit { preferences ->
            val updated = transform(preferences.toAppearanceConfig())
            preferences.write(updated)
        }
    }

    /** Replaces the stored appearance with [config]. */
    suspend fun replace(config: AppearanceConfig) {
        dataStore.edit { preferences -> preferences.write(config) }
    }

    internal object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT_ARGB = intPreferencesKey("accent_argb")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val TEXT_SCALE = stringPreferencesKey("text_scale")
        val BACKGROUND_STYLE = stringPreferencesKey("background_style")
        val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        val BACKGROUND_SCRIM_ALPHA = floatPreferencesKey("background_scrim_alpha")
        val BACKGROUND_BLUR_DP = intPreferencesKey("background_blur_dp")
        val PLAYER_LAYOUT = stringPreferencesKey("player_layout")
    }

    private companion object {
        val DEFAULTS = AppearanceConfig()

        fun Preferences.toAppearanceConfig(): AppearanceConfig = AppearanceConfig(
            themeMode = enumValueOrDefault<ThemeMode>(this[Keys.THEME_MODE], DEFAULTS.themeMode),
            useAmoledBlack = this[Keys.AMOLED_BLACK] ?: DEFAULTS.useAmoledBlack,
            useDynamicColor = this[Keys.DYNAMIC_COLOR] ?: DEFAULTS.useDynamicColor,
            accentColorArgb = this[Keys.ACCENT_ARGB] ?: DEFAULTS.accentColorArgb,
            fontFamily = enumValueOrDefault<FontFamilyOption>(this[Keys.FONT_FAMILY], DEFAULTS.fontFamily),
            textScale = enumValueOrDefault<TextScale>(this[Keys.TEXT_SCALE], DEFAULTS.textScale),
            backgroundStyle = enumValueOrDefault<BackgroundStyle>(
                this[Keys.BACKGROUND_STYLE],
                DEFAULTS.backgroundStyle,
            ),
            backgroundImageUri = this[Keys.BACKGROUND_IMAGE_URI]?.takeIf { it.isNotBlank() },
            backgroundScrimAlpha = (this[Keys.BACKGROUND_SCRIM_ALPHA] ?: DEFAULTS.backgroundScrimAlpha)
                .coerceIn(AppearanceConfig.MIN_BACKGROUND_SCRIM_ALPHA, AppearanceConfig.MAX_BACKGROUND_SCRIM_ALPHA),
            backgroundBlurDp = (this[Keys.BACKGROUND_BLUR_DP] ?: DEFAULTS.backgroundBlurDp)
                .coerceIn(0, AppearanceConfig.MAX_BACKGROUND_BLUR_DP),
            playerLayout = enumValueOrDefault<PlayerLayout>(this[Keys.PLAYER_LAYOUT], DEFAULTS.playerLayout),
        )

        fun MutablePreferences.write(config: AppearanceConfig) {
            this[Keys.THEME_MODE] = config.themeMode.name
            this[Keys.AMOLED_BLACK] = config.useAmoledBlack
            this[Keys.DYNAMIC_COLOR] = config.useDynamicColor
            this[Keys.ACCENT_ARGB] = config.accentColorArgb
            this[Keys.FONT_FAMILY] = config.fontFamily.name
            this[Keys.TEXT_SCALE] = config.textScale.name
            this[Keys.BACKGROUND_STYLE] = config.backgroundStyle.name
            val uri = config.backgroundImageUri
            if (uri.isNullOrBlank()) remove(Keys.BACKGROUND_IMAGE_URI) else this[Keys.BACKGROUND_IMAGE_URI] = uri
            this[Keys.BACKGROUND_SCRIM_ALPHA] = config.backgroundScrimAlpha.coerceIn(
                AppearanceConfig.MIN_BACKGROUND_SCRIM_ALPHA,
                AppearanceConfig.MAX_BACKGROUND_SCRIM_ALPHA,
            )
            this[Keys.BACKGROUND_BLUR_DP] = config.backgroundBlurDp.coerceIn(
                0,
                AppearanceConfig.MAX_BACKGROUND_BLUR_DP,
            )
            this[Keys.PLAYER_LAYOUT] = config.playerLayout.name
        }
    }
}
