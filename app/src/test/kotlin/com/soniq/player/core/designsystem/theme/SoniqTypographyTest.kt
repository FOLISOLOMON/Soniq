package com.soniq.player.core.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.model.appearance.FontFamilyOption
import com.soniq.player.core.model.appearance.TextScale
import org.junit.Test

class SoniqTypographyTest {

    @Test
    fun mapsAllFontFamilyOptions() {
        assertThat(FontFamilyOption.SYSTEM.toFontFamily()).isEqualTo(FontFamily.Default)
        assertThat(FontFamilyOption.SANS_SERIF.toFontFamily()).isEqualTo(FontFamily.SansSerif)
        assertThat(FontFamilyOption.SERIF.toFontFamily()).isEqualTo(FontFamily.Serif)
        assertThat(FontFamilyOption.MONOSPACE.toFontFamily()).isEqualTo(FontFamily.Monospace)
    }

    @Test
    fun scalesFontSizeAndLineHeightByTextScaleMultiplier() {
        val baseline = soniqTypography(FontFamilyOption.SYSTEM, TextScale.DEFAULT)
        val small = soniqTypography(FontFamilyOption.SERIF, TextScale.SMALL)
        val large = soniqTypography(FontFamilyOption.MONOSPACE, TextScale.LARGE)

        assertThat(small.bodyLarge.fontFamily).isEqualTo(FontFamily.Serif)
        assertThat(small.bodyLarge.fontSize.value)
            .isWithin(0.01f)
            .of(baseline.bodyLarge.fontSize.value * TextScale.SMALL.multiplier)

        assertThat(large.titleLarge.fontFamily).isEqualTo(FontFamily.Monospace)
        assertThat(large.titleLarge.fontSize.value)
            .isWithin(0.01f)
            .of(baseline.titleLarge.fontSize.value * TextScale.LARGE.multiplier)
        assertThat(large.titleLarge.lineHeight.value)
            .isWithin(0.01f)
            .of(baseline.titleLarge.lineHeight.value * TextScale.LARGE.multiplier)
    }
}
