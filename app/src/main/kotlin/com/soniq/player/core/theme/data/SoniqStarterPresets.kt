package com.soniq.player.core.theme.data

import com.soniq.player.core.designsystem.token.SoniqAccents
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.BackgroundStyle
import com.soniq.player.core.model.appearance.FontFamilyOption
import com.soniq.player.core.model.appearance.PlayerLayout
import com.soniq.player.core.model.appearance.TextScale
import com.soniq.player.core.model.appearance.ThemeMode

/**
 * Curated offline starter theme presets that users can load into their saved presets list
 * without overwriting any existing user-authored presets.
 */
object SoniqStarterPresets {

    data class StarterPresetSpec(
        val name: String,
        val config: AppearanceConfig,
    )

    val all: List<StarterPresetSpec> = listOf(
        StarterPresetSpec(
            name = "SONIQ Violet",
            config = AppearanceConfig(
                themeMode = ThemeMode.DARK,
                useAmoledBlack = false,
                useDynamicColor = false,
                accentColorArgb = SoniqAccents.Violet.argb,
                backgroundStyle = BackgroundStyle.GRADIENT,
                playerLayout = PlayerLayout.CLASSIC,
            ),
        ),
        StarterPresetSpec(
            name = "AMOLED Emerald",
            config = AppearanceConfig(
                themeMode = ThemeMode.DARK,
                useAmoledBlack = true,
                useDynamicColor = false,
                accentColorArgb = SoniqAccents.Emerald.argb,
                backgroundStyle = BackgroundStyle.SOLID,
                playerLayout = PlayerLayout.ARTWORK_FOCUS,
            ),
        ),
        StarterPresetSpec(
            name = "Midnight Ocean",
            config = AppearanceConfig(
                themeMode = ThemeMode.DARK,
                useAmoledBlack = true,
                useDynamicColor = false,
                accentColorArgb = SoniqAccents.Ocean.argb,
                fontFamily = FontFamilyOption.SANS_SERIF,
                backgroundStyle = BackgroundStyle.GRADIENT,
                playerLayout = PlayerLayout.COMPACT,
            ),
        ),
        StarterPresetSpec(
            name = "Warm Amber",
            config = AppearanceConfig(
                themeMode = ThemeMode.LIGHT,
                useAmoledBlack = false,
                useDynamicColor = false,
                accentColorArgb = SoniqAccents.Amber.argb,
                fontFamily = FontFamilyOption.SERIF,
                textScale = TextScale.DEFAULT,
                backgroundStyle = BackgroundStyle.GRADIENT,
                playerLayout = PlayerLayout.CLASSIC,
            ),
        ),
    )
}
