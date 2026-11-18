package com.soniq.player.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.designsystem.theme.SoniqColorSchemes.toAmoled
import com.soniq.player.core.designsystem.token.SoniqAccents
import com.soniq.player.core.designsystem.token.SoniqNeutrals
import org.junit.Test

class ColorMathAndSchemesTest {

    @Test
    fun hslRoundTripPreservesAccentSwatchesWithinOneChannelUnit() {
        SoniqAccents.all.forEach { swatch ->
            val roundTrip = ColorMath.fromHsl(ColorMath.toHsl(swatch.argb))
            assertThat(ColorMath.red(roundTrip)).isWithin(1).of(ColorMath.red(swatch.argb))
            assertThat(ColorMath.green(roundTrip)).isWithin(1).of(ColorMath.green(swatch.argb))
            assertThat(ColorMath.blue(roundTrip)).isWithin(1).of(ColorMath.blue(swatch.argb))
        }
    }

    @Test
    fun readableOnMeetsWcagContrastForAllCuratedAccentsAndExtremes() {
        val samples = SoniqAccents.all.map { it.argb } + listOf(
            0xFF000000.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFFFFF00.toInt(),
            0xFF001133.toInt(),
        )
        samples.forEach { background ->
            val foreground = ColorMath.readableOn(background)
            assertThat(ColorMath.contrastRatio(background, foreground)).isAtLeast(4.5)
        }
    }

    @Test
    fun lightAndDarkSchemesMaintainReadablePrimaryContrast() {
        SoniqAccents.all.forEach { swatch ->
            val light = SoniqColorSchemes.light(swatch.argb)
            val dark = SoniqColorSchemes.dark(swatch.argb)

            assertThat(
                ColorMath.contrastRatio(light.primary.toArgb(), light.onPrimary.toArgb()),
            ).isAtLeast(4.5)
            assertThat(
                ColorMath.contrastRatio(dark.primary.toArgb(), dark.onPrimary.toArgb()),
            ).isAtLeast(4.5)
        }
    }

    @Test
    fun toAmoledUsesPureBlackSurfacesWhilePreservingAccents() {
        val dark = SoniqColorSchemes.dark(SoniqAccents.Cyan.argb)
        val amoled = dark.toAmoled()

        assertThat(amoled.background).isEqualTo(Color.Black)
        assertThat(amoled.surface).isEqualTo(Color.Black)
        assertThat(amoled.surfaceContainerLowest).isEqualTo(SoniqNeutrals.AmoledSurfaceContainerLowest)
        assertThat(amoled.primary).isEqualTo(dark.primary)
        assertThat(amoled.secondary).isEqualTo(dark.secondary)
        assertThat(amoled.tertiary).isEqualTo(dark.tertiary)
    }
}
