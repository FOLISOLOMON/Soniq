package com.soniq.player.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.soniq.player.core.designsystem.token.SoniqNeutrals

/**
 * Builds Material 3 color schemes from a single user-selected accent (seed) color.
 *
 * Accent roles are derived in HSL space with fixed lightness targets per theme, and every "on"
 * color is chosen for maximum WCAG contrast, so arbitrary user accents stay legible.
 * Neutral roles come from the [SoniqNeutrals] tokens, lightly tinted towards the accent hue.
 */
object SoniqColorSchemes {

    private const val NEUTRAL_TINT = 0.04f
    private const val SECONDARY_SATURATION_FACTOR = 0.45f
    private const val TERTIARY_HUE_SHIFT = 60f

    fun light(accentArgb: Int): ColorScheme {
        val seed = ColorMath.toHsl(accentArgb)
        val primary = tone(seed, lightness = 0.42f)
        val primaryContainer = tone(seed, lightness = 0.90f)
        val secondarySeed = seed.copy(saturation = seed.saturation * SECONDARY_SATURATION_FACTOR)
        val secondary = tone(secondarySeed, lightness = 0.40f)
        val secondaryContainer = tone(secondarySeed, lightness = 0.90f)
        val tertiarySeed = seed.copy(hue = seed.hue + TERTIARY_HUE_SHIFT)
        val tertiary = tone(tertiarySeed, lightness = 0.40f)
        val tertiaryContainer = tone(tertiarySeed, lightness = 0.90f)

        return lightColorScheme(
            primary = Color(primary),
            onPrimary = Color(ColorMath.readableOn(primary)),
            primaryContainer = Color(primaryContainer),
            onPrimaryContainer = Color(tone(seed, lightness = 0.12f)),
            inversePrimary = Color(tone(seed, lightness = 0.80f)),
            secondary = Color(secondary),
            onSecondary = Color(ColorMath.readableOn(secondary)),
            secondaryContainer = Color(secondaryContainer),
            onSecondaryContainer = Color(tone(secondarySeed, lightness = 0.12f)),
            tertiary = Color(tertiary),
            onTertiary = Color(ColorMath.readableOn(tertiary)),
            tertiaryContainer = Color(tertiaryContainer),
            onTertiaryContainer = Color(tone(tertiarySeed, lightness = 0.12f)),
            background = tint(SoniqNeutrals.LightBackground, primary),
            onBackground = SoniqNeutrals.LightOnSurface,
            surface = tint(SoniqNeutrals.LightSurface, primary),
            onSurface = SoniqNeutrals.LightOnSurface,
            surfaceVariant = tint(SoniqNeutrals.LightSurfaceVariant, primary),
            onSurfaceVariant = SoniqNeutrals.LightOnSurfaceVariant,
            surfaceTint = Color(primary),
            inverseSurface = SoniqNeutrals.DarkSurfaceContainerHighest,
            inverseOnSurface = SoniqNeutrals.DarkOnSurface,
            error = SoniqNeutrals.ErrorLight,
            onError = SoniqNeutrals.OnErrorLight,
            errorContainer = SoniqNeutrals.ErrorContainerLight,
            onErrorContainer = SoniqNeutrals.OnErrorContainerLight,
            outline = SoniqNeutrals.LightOutline,
            outlineVariant = SoniqNeutrals.LightOutlineVariant,
            surfaceBright = tint(SoniqNeutrals.LightSurface, primary),
            surfaceDim = tint(SoniqNeutrals.LightSurfaceContainerHighest, primary),
            surfaceContainerLowest = SoniqNeutrals.LightSurfaceContainerLowest,
            surfaceContainerLow = tint(SoniqNeutrals.LightSurfaceContainerLow, primary),
            surfaceContainer = tint(SoniqNeutrals.LightSurfaceContainer, primary),
            surfaceContainerHigh = tint(SoniqNeutrals.LightSurfaceContainerHigh, primary),
            surfaceContainerHighest = tint(SoniqNeutrals.LightSurfaceContainerHighest, primary),
        )
    }

    fun dark(accentArgb: Int): ColorScheme {
        val seed = ColorMath.toHsl(accentArgb)
        val primary = tone(seed, lightness = 0.76f)
        val primaryContainer = tone(seed, lightness = 0.28f)
        val secondarySeed = seed.copy(saturation = seed.saturation * SECONDARY_SATURATION_FACTOR)
        val secondary = tone(secondarySeed, lightness = 0.78f)
        val secondaryContainer = tone(secondarySeed, lightness = 0.28f)
        val tertiarySeed = seed.copy(hue = seed.hue + TERTIARY_HUE_SHIFT)
        val tertiary = tone(tertiarySeed, lightness = 0.78f)
        val tertiaryContainer = tone(tertiarySeed, lightness = 0.28f)

        return darkColorScheme(
            primary = Color(primary),
            onPrimary = Color(ColorMath.readableOn(primary)),
            primaryContainer = Color(primaryContainer),
            onPrimaryContainer = Color(tone(seed, lightness = 0.90f)),
            inversePrimary = Color(tone(seed, lightness = 0.42f)),
            secondary = Color(secondary),
            onSecondary = Color(ColorMath.readableOn(secondary)),
            secondaryContainer = Color(secondaryContainer),
            onSecondaryContainer = Color(tone(secondarySeed, lightness = 0.90f)),
            tertiary = Color(tertiary),
            onTertiary = Color(ColorMath.readableOn(tertiary)),
            tertiaryContainer = Color(tertiaryContainer),
            onTertiaryContainer = Color(tone(tertiarySeed, lightness = 0.90f)),
            background = tint(SoniqNeutrals.DarkBackground, primary),
            onBackground = SoniqNeutrals.DarkOnSurface,
            surface = tint(SoniqNeutrals.DarkSurface, primary),
            onSurface = SoniqNeutrals.DarkOnSurface,
            surfaceVariant = tint(SoniqNeutrals.DarkSurfaceVariant, primary),
            onSurfaceVariant = SoniqNeutrals.DarkOnSurfaceVariant,
            surfaceTint = Color(primary),
            inverseSurface = SoniqNeutrals.LightSurfaceContainerHighest,
            inverseOnSurface = SoniqNeutrals.LightOnSurface,
            error = SoniqNeutrals.ErrorDark,
            onError = SoniqNeutrals.OnErrorDark,
            errorContainer = SoniqNeutrals.ErrorContainerDark,
            onErrorContainer = SoniqNeutrals.OnErrorContainerDark,
            outline = SoniqNeutrals.DarkOutline,
            outlineVariant = SoniqNeutrals.DarkOutlineVariant,
            surfaceBright = tint(SoniqNeutrals.DarkSurfaceContainerHighest, primary),
            surfaceDim = tint(SoniqNeutrals.DarkSurface, primary),
            surfaceContainerLowest = SoniqNeutrals.DarkSurfaceContainerLowest,
            surfaceContainerLow = tint(SoniqNeutrals.DarkSurfaceContainerLow, primary),
            surfaceContainer = tint(SoniqNeutrals.DarkSurfaceContainer, primary),
            surfaceContainerHigh = tint(SoniqNeutrals.DarkSurfaceContainerHigh, primary),
            surfaceContainerHighest = tint(SoniqNeutrals.DarkSurfaceContainerHighest, primary),
        )
    }

    /**
     * Converts any dark scheme (generated or wallpaper-derived) to a pure-black AMOLED variant.
     * Accent roles are preserved; only background and surface roles change.
     */
    fun ColorScheme.toAmoled(): ColorScheme = copy(
        background = SoniqNeutrals.AmoledBackground,
        surface = SoniqNeutrals.AmoledBackground,
        surfaceDim = SoniqNeutrals.AmoledBackground,
        surfaceBright = SoniqNeutrals.AmoledSurfaceContainerHighest,
        surfaceContainerLowest = SoniqNeutrals.AmoledSurfaceContainerLowest,
        surfaceContainerLow = SoniqNeutrals.AmoledSurfaceContainerLow,
        surfaceContainer = SoniqNeutrals.AmoledSurfaceContainer,
        surfaceContainerHigh = SoniqNeutrals.AmoledSurfaceContainerHigh,
        surfaceContainerHighest = SoniqNeutrals.AmoledSurfaceContainerHighest,
    )

    private fun tone(seed: ColorMath.Hsl, lightness: Float): Int =
        ColorMath.fromHsl(seed.copy(lightness = lightness))

    private fun tint(neutral: Color, accent: Int): Color =
        Color(ColorMath.blend(neutral.toArgb(), accent, NEUTRAL_TINT))
}
