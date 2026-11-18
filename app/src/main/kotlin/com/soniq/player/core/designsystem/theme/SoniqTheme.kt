package com.soniq.player.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.soniq.player.core.designsystem.theme.SoniqColorSchemes.toAmoled
import com.soniq.player.core.designsystem.token.SoniqRadii
import com.soniq.player.core.designsystem.token.SoniqTokens
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.ThemeMode

/** SONIQ tokens that Material 3 has no slot for (spacing, sizes, motion, alpha...). */
val LocalSoniqTokens = staticCompositionLocalOf { SoniqTokens() }

/** The active appearance, for components that render user customizations (e.g. backgrounds). */
val LocalAppearance = staticCompositionLocalOf { AppearanceConfig() }

/** Whether the OS can provide wallpaper-based dynamic color. */
val isDynamicColorSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Resolves whether [appearance] should render dark, given the system setting. */
@Composable
@ReadOnlyComposable
fun AppearanceConfig.shouldUseDarkTheme(): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * Root theme of SONIQ. Translates the user's [AppearanceConfig] into Material 3 color scheme,
 * typography and shapes, and provides SONIQ-specific tokens via [LocalSoniqTokens].
 */
@Composable
fun SoniqTheme(
    appearance: AppearanceConfig = AppearanceConfig(),
    darkTheme: Boolean = appearance.shouldUseDarkTheme(),
    tokens: SoniqTokens = SoniqTokens(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamic = appearance.useDynamicColor && isDynamicColorSupported
    val colorScheme: ColorScheme = remember(
        darkTheme,
        useDynamic,
        appearance.accentColorArgb,
        appearance.useAmoledBlack,
    ) {
        val base = when {
            useDynamic && darkTheme -> dynamicDarkColorScheme(context)
            useDynamic -> dynamicLightColorScheme(context)
            darkTheme -> SoniqColorSchemes.dark(appearance.accentColorArgb)
            else -> SoniqColorSchemes.light(appearance.accentColorArgb)
        }
        if (darkTheme && appearance.useAmoledBlack) base.toAmoled() else base
    }
    val typography = remember(appearance.fontFamily, appearance.textScale) {
        soniqTypography(appearance.fontFamily, appearance.textScale)
    }
    val shapes = remember(tokens.radii) { tokens.radii.toShapes() }

    CompositionLocalProvider(
        LocalSoniqTokens provides tokens,
        LocalAppearance provides appearance,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}

/** Convenience accessors: `SoniqTheme.tokens.spacing.lg`. */
object SoniqTheme {
    val tokens: SoniqTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalSoniqTokens.current

    val appearance: AppearanceConfig
        @Composable
        @ReadOnlyComposable
        get() = LocalAppearance.current
}

internal fun SoniqRadii.toShapes(): Shapes = Shapes(
    extraSmall = RoundedCornerShape(extraSmall),
    small = RoundedCornerShape(small),
    medium = RoundedCornerShape(medium),
    large = RoundedCornerShape(large),
    extraLarge = RoundedCornerShape(extraLarge),
)
