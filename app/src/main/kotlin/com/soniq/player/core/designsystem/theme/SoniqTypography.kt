package com.soniq.player.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import com.soniq.player.core.model.appearance.FontFamilyOption
import com.soniq.player.core.model.appearance.TextScale

/** Maps the user's font choice to a Compose [FontFamily]. */
fun FontFamilyOption.toFontFamily(): FontFamily = when (this) {
    FontFamilyOption.SYSTEM -> FontFamily.Default
    FontFamilyOption.SANS_SERIF -> FontFamily.SansSerif
    FontFamilyOption.SERIF -> FontFamily.Serif
    FontFamilyOption.MONOSPACE -> FontFamily.Monospace
}

/**
 * Builds SONIQ typography from the Material 3 type scale (the typography token baseline),
 * applying the user's font family and text scale to every style.
 *
 * Text scale is multiplicative with the system font scale, which Compose applies separately,
 * so accessibility settings are always respected.
 */
fun soniqTypography(
    fontFamily: FontFamilyOption = FontFamilyOption.SYSTEM,
    textScale: TextScale = TextScale.DEFAULT,
): Typography {
    val base = Typography()
    val family = fontFamily.toFontFamily()
    val scale = textScale.multiplier
    fun TextStyle.customize() = copy(
        fontFamily = family,
        fontSize = fontSize.scaled(scale),
        lineHeight = lineHeight.scaled(scale),
    )
    return Typography(
        displayLarge = base.displayLarge.customize(),
        displayMedium = base.displayMedium.customize(),
        displaySmall = base.displaySmall.customize(),
        headlineLarge = base.headlineLarge.customize(),
        headlineMedium = base.headlineMedium.customize(),
        headlineSmall = base.headlineSmall.customize(),
        titleLarge = base.titleLarge.customize(),
        titleMedium = base.titleMedium.customize(),
        titleSmall = base.titleSmall.customize(),
        bodyLarge = base.bodyLarge.customize(),
        bodyMedium = base.bodyMedium.customize(),
        bodySmall = base.bodySmall.customize(),
        labelLarge = base.labelLarge.customize(),
        labelMedium = base.labelMedium.customize(),
        labelSmall = base.labelSmall.customize(),
    )
}

private fun TextUnit.scaled(factor: Float): TextUnit = if (isSpecified) this * factor else this
