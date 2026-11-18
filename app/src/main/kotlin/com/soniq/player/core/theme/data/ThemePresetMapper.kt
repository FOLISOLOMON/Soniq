package com.soniq.player.core.theme.data

import com.soniq.player.core.common.enumValueOrDefault
import com.soniq.player.core.database.entity.ThemePresetEntity
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.BackgroundStyle
import com.soniq.player.core.model.appearance.FontFamilyOption
import com.soniq.player.core.model.appearance.PlayerLayout
import com.soniq.player.core.model.appearance.TextScale
import com.soniq.player.core.model.appearance.ThemeMode
import com.soniq.player.core.model.appearance.ThemePreset

private val DEFAULTS = AppearanceConfig()

internal fun ThemePresetEntity.toDomain(): ThemePreset = ThemePreset(
    id = id,
    name = name,
    createdAtEpochMillis = createdAtEpochMillis,
    config = AppearanceConfig(
        themeMode = enumValueOrDefault<ThemeMode>(themeMode, DEFAULTS.themeMode),
        useAmoledBlack = amoledBlack,
        useDynamicColor = dynamicColor,
        accentColorArgb = accentArgb,
        fontFamily = enumValueOrDefault<FontFamilyOption>(fontFamily, DEFAULTS.fontFamily),
        textScale = enumValueOrDefault<TextScale>(textScale, DEFAULTS.textScale),
        backgroundStyle = enumValueOrDefault<BackgroundStyle>(backgroundStyle, DEFAULTS.backgroundStyle),
        backgroundImageUri = backgroundImageUri?.takeIf { it.isNotBlank() },
        backgroundScrimAlpha = backgroundScrimAlpha.coerceIn(
            AppearanceConfig.MIN_BACKGROUND_SCRIM_ALPHA,
            AppearanceConfig.MAX_BACKGROUND_SCRIM_ALPHA,
        ),
        backgroundBlurDp = backgroundBlurDp.coerceIn(0, AppearanceConfig.MAX_BACKGROUND_BLUR_DP),
        playerLayout = enumValueOrDefault<PlayerLayout>(playerLayout, DEFAULTS.playerLayout),
    ),
)

internal fun AppearanceConfig.toPresetEntity(name: String, createdAtEpochMillis: Long): ThemePresetEntity =
    ThemePresetEntity(
        name = name,
        themeMode = themeMode.name,
        amoledBlack = useAmoledBlack,
        dynamicColor = useDynamicColor,
        accentArgb = accentColorArgb,
        fontFamily = fontFamily.name,
        textScale = textScale.name,
        backgroundStyle = backgroundStyle.name,
        backgroundImageUri = backgroundImageUri,
        backgroundScrimAlpha = backgroundScrimAlpha.coerceIn(
            AppearanceConfig.MIN_BACKGROUND_SCRIM_ALPHA,
            AppearanceConfig.MAX_BACKGROUND_SCRIM_ALPHA,
        ),
        backgroundBlurDp = backgroundBlurDp.coerceIn(0, AppearanceConfig.MAX_BACKGROUND_BLUR_DP),
        playerLayout = playerLayout.name,
        createdAtEpochMillis = createdAtEpochMillis,
    )
