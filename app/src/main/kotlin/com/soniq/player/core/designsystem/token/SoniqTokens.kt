package com.soniq.player.core.designsystem.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * SONIQ design tokens.
 *
 * Every visual constant used by the UI is defined here. Screens and components must read these
 * values (through MaterialTheme or SoniqTheme) instead of hardcoding colors, sizes or durations.
 */

/** Neutral (non-accent) palette used as the base of generated color schemes. */
object SoniqNeutrals {
    // Light
    val LightBackground = Color(0xFFFBF8FF)
    val LightSurface = Color(0xFFFBF8FF)
    val LightOnSurface = Color(0xFF1B1B21)
    val LightOnSurfaceVariant = Color(0xFF47464F)
    val LightSurfaceVariant = Color(0xFFE4E1EC)
    val LightOutline = Color(0xFF787680)
    val LightOutlineVariant = Color(0xFFC8C5D0)
    val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
    val LightSurfaceContainerLow = Color(0xFFF5F2FA)
    val LightSurfaceContainer = Color(0xFFEFEDF4)
    val LightSurfaceContainerHigh = Color(0xFFEAE7EF)
    val LightSurfaceContainerHighest = Color(0xFFE4E1E9)

    // Dark
    val DarkBackground = Color(0xFF121016)
    val DarkSurface = Color(0xFF121016)
    val DarkOnSurface = Color(0xFFE5E1E9)
    val DarkOnSurfaceVariant = Color(0xFFC8C5D0)
    val DarkSurfaceVariant = Color(0xFF47464F)
    val DarkOutline = Color(0xFF928F9A)
    val DarkOutlineVariant = Color(0xFF47464F)
    val DarkSurfaceContainerLowest = Color(0xFF0D0B11)
    val DarkSurfaceContainerLow = Color(0xFF1B1A20)
    val DarkSurfaceContainer = Color(0xFF1F1E24)
    val DarkSurfaceContainerHigh = Color(0xFF2A282F)
    val DarkSurfaceContainerHighest = Color(0xFF35333A)

    // AMOLED (pure black) – containers stay barely above black to preserve hierarchy.
    val AmoledBackground = Color(0xFF000000)
    val AmoledSurfaceContainerLowest = Color(0xFF000000)
    val AmoledSurfaceContainerLow = Color(0xFF0A0A0C)
    val AmoledSurfaceContainer = Color(0xFF101013)
    val AmoledSurfaceContainerHigh = Color(0xFF17171B)
    val AmoledSurfaceContainerHighest = Color(0xFF1F1F24)

    // Semantic
    val ErrorLight = Color(0xFFBA1A1A)
    val OnErrorLight = Color(0xFFFFFFFF)
    val ErrorContainerLight = Color(0xFFFFDAD6)
    val OnErrorContainerLight = Color(0xFF410002)
    val ErrorDark = Color(0xFFFFB4AB)
    val OnErrorDark = Color(0xFF690005)
    val ErrorContainerDark = Color(0xFF93000A)
    val OnErrorContainerDark = Color(0xFFFFDAD6)
}

/** A named accent swatch offered in settings. */
@Immutable
data class AccentSwatch(val name: String, val argb: Int) {
    val color: Color get() = Color(argb)
}

/** Curated accent swatches. Users can still store any ARGB accent value. */
object SoniqAccents {
    val Violet = AccentSwatch("Violet", 0xFF7C5CFF.toInt())
    val Ocean = AccentSwatch("Ocean", 0xFF2F80ED.toInt())
    val Cyan = AccentSwatch("Cyan", 0xFF00ACC1.toInt())
    val Emerald = AccentSwatch("Emerald", 0xFF1DB98A.toInt())
    val Amber = AccentSwatch("Amber", 0xFFFFB020.toInt())
    val Coral = AccentSwatch("Coral", 0xFFFF6B5C.toInt())
    val Rose = AccentSwatch("Rose", 0xFFE8457C.toInt())
    val Graphite = AccentSwatch("Graphite", 0xFF8A8F98.toInt())

    val all: List<AccentSwatch> = listOf(Violet, Ocean, Cyan, Emerald, Amber, Coral, Rose, Graphite)
}

/** Spacing scale (4dp grid). */
@Immutable
data class SoniqSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
    /** Horizontal padding of screen content. */
    val screenHorizontal: Dp = 16.dp,
)

/** Corner radius scale feeding Material 3 shapes. */
@Immutable
data class SoniqRadii(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 20.dp,
    val extraLarge: Dp = 28.dp,
)

/** Component dimensions. */
@Immutable
data class SoniqSizes(
    val iconSmall: Dp = 18.dp,
    val icon: Dp = 24.dp,
    val iconLarge: Dp = 48.dp,
    val emptyStateIcon: Dp = 72.dp,
    val accentSwatch: Dp = 44.dp,
    val minTouchTarget: Dp = 48.dp,
    val contentMaxWidth: Dp = 640.dp,
)

/** Tonal elevation levels. */
@Immutable
data class SoniqElevation(
    val level0: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 3.dp,
    val level3: Dp = 6.dp,
    val level4: Dp = 8.dp,
    val level5: Dp = 12.dp,
)

/** Motion durations in milliseconds. */
@Immutable
data class SoniqMotion(
    val short: Int = 150,
    val medium: Int = 300,
    val long: Int = 500,
)

/** Opacity tokens for overlays and backgrounds. */
@Immutable
data class SoniqAlpha(
    /** Scrim drawn over a user background image to keep content legible. */
    val backgroundImageScrim: Float = 0.72f,
    /** Strength of the accent tint at the top of the gradient background. */
    val gradientAccent: Float = 0.35f,
    val disabled: Float = 0.38f,
)

/** Aggregate of all non-Material tokens, provided through `LocalSoniqTokens`. */
@Immutable
data class SoniqTokens(
    val spacing: SoniqSpacing = SoniqSpacing(),
    val radii: SoniqRadii = SoniqRadii(),
    val sizes: SoniqSizes = SoniqSizes(),
    val elevation: SoniqElevation = SoniqElevation(),
    val motion: SoniqMotion = SoniqMotion(),
    val alpha: SoniqAlpha = SoniqAlpha(),
)
