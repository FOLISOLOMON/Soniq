package com.soniq.player.core.designsystem.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass + Futuristic Glassmorphism visual tokens for SONIQ.
 *
 * Derived from the lavender/periwinkle/violet ambient palette with pink and cyan accent lighting,
 * adapting cleanly across Dark, Light, and AMOLED modes while maintaining WCAG contrast.
 */
@Immutable
data class SoniqGlassColors(
    val ambientTop: Color,
    val ambientMid: Color,
    val ambientBottom: Color,
    val glowPrimary: Color,
    val glowSecondary: Color,
    val glowTertiary: Color,
    val surfaceTop: Color,
    val surfaceBottom: Color,
    val surfaceElevated: Color,
    val borderHighlight: Color,
    val borderSubtle: Color,
    val activePillStart: Color,
    val activePillEnd: Color,
    val scrubberStart: Color,
    val scrubberEnd: Color,
    val navBarFill: Color,
)

@Immutable
data class SoniqGlassMetrics(
    val cardRadius: Dp = 22.dp,
    val heroRadius: Dp = 26.dp,
    val artworkRadius: Dp = 24.dp,
    val thumbnailRadius: Dp = 14.dp,
    val pillRadius: Dp = 50.dp,
    val borderWidth: Dp = 1.dp,
    val navBarHeight: Dp = 68.dp,
    val navBarRadius: Dp = 28.dp,
)

object SoniqGlassPalettes {

    val GlowPink = Color(0xFFFF6EC7)
    val GlowCyan = Color(0xFF47D7FF)
    val GlowViolet = Color(0xFF9E7BFF)
    val ScrubberPink = Color(0xFFFF6EB4)
    val ScrubberCyan = Color(0xFF56CCF2)

    fun dark(accent: Color, amoled: Boolean): SoniqGlassColors = SoniqGlassColors(
        ambientTop = if (amoled) Color(0xFF0A0818) else Color(0xFF3E3484),
        ambientMid = if (amoled) Color(0xFF04030B) else Color(0xFF282668),
        ambientBottom = if (amoled) Color(0xFF000000) else Color(0xFF171842),
        glowPrimary = GlowPink.copy(alpha = if (amoled) 0.14f else 0.28f),
        glowSecondary = GlowCyan.copy(alpha = if (amoled) 0.12f else 0.22f),
        glowTertiary = accent.copy(alpha = if (amoled) 0.16f else 0.30f),
        surfaceTop = Color.White.copy(alpha = if (amoled) 0.10f else 0.16f),
        surfaceBottom = Color.White.copy(alpha = if (amoled) 0.05f else 0.08f),
        surfaceElevated = Color.White.copy(alpha = if (amoled) 0.14f else 0.22f),
        borderHighlight = Color.White.copy(alpha = if (amoled) 0.22f else 0.34f),
        borderSubtle = Color.White.copy(alpha = if (amoled) 0.08f else 0.12f),
        activePillStart = accent,
        activePillEnd = GlowPink,
        scrubberStart = ScrubberPink,
        scrubberEnd = ScrubberCyan,
        navBarFill = if (amoled) Color(0xFF121024).copy(alpha = 0.88f) else Color(0xFF27245C).copy(alpha = 0.78f),
    )

    fun light(accent: Color): SoniqGlassColors = SoniqGlassColors(
        ambientTop = Color(0xFFDAD9FF),
        ambientMid = Color(0xFFC4C6FC),
        ambientBottom = Color(0xFFE7E9FF),
        glowPrimary = GlowPink.copy(alpha = 0.24f),
        glowSecondary = GlowCyan.copy(alpha = 0.22f),
        glowTertiary = accent.copy(alpha = 0.24f),
        surfaceTop = Color.White.copy(alpha = 0.72f),
        surfaceBottom = Color.White.copy(alpha = 0.48f),
        surfaceElevated = Color.White.copy(alpha = 0.82f),
        borderHighlight = Color.White.copy(alpha = 0.85f),
        borderSubtle = Color.White.copy(alpha = 0.40f),
        activePillStart = accent,
        activePillEnd = GlowViolet,
        scrubberStart = ScrubberPink,
        scrubberEnd = accent,
        navBarFill = Color.White.copy(alpha = 0.76f),
    )
}
