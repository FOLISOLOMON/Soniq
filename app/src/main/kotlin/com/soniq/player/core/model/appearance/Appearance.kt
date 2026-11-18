package com.soniq.player.core.model.appearance

/** How the app chooses between light and dark color schemes. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Typeface families selectable by the user. Bundled custom fonts can be added later. */
enum class FontFamilyOption { SYSTEM, SANS_SERIF, SERIF, MONOSPACE }

/** Global text scaling applied on top of the system font scale. */
enum class TextScale(val multiplier: Float) {
    SMALL(0.9f),
    DEFAULT(1.0f),
    LARGE(1.15f),
}

/** Background treatment drawn behind every top-level screen. */
enum class BackgroundStyle { SOLID, GRADIENT, IMAGE }

/** Arrangement of the now-playing screen. Rendered by the player feature in a later stage. */
enum class PlayerLayout { CLASSIC, ARTWORK_FOCUS, COMPACT }

/**
 * The complete, user-customizable appearance of SONIQ.
 *
 * This is a pure domain model: it has no Android or Compose dependencies so that it can be
 * persisted, saved as a preset, and unit-tested in isolation. The design system translates it
 * into Material 3 color schemes, typography and shapes.
 */
data class AppearanceConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** When the dark theme is active, use true-black surfaces (saves power on OLED panels). */
    val useAmoledBlack: Boolean = false,
    /** Use wallpaper-derived Material You colors when the OS supports them (API 31+). */
    val useDynamicColor: Boolean = false,
    /** Seed accent color as ARGB. Ignored while dynamic color is active and supported. */
    val accentColorArgb: Int = DEFAULT_ACCENT_ARGB,
    val fontFamily: FontFamilyOption = FontFamilyOption.SYSTEM,
    val textScale: TextScale = TextScale.DEFAULT,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.SOLID,
    /** Persisted content:// URI of a user-picked background image. */
    val backgroundImageUri: String? = null,
    val playerLayout: PlayerLayout = PlayerLayout.CLASSIC,
) {
    /**
     * The background style that can actually be rendered: an image background without an image
     * degrades to a solid background rather than showing nothing.
     */
    val effectiveBackgroundStyle: BackgroundStyle
        get() = if (backgroundStyle == BackgroundStyle.IMAGE && backgroundImageUri.isNullOrBlank()) {
            BackgroundStyle.SOLID
        } else {
            backgroundStyle
        }

    companion object {
        /** SONIQ violet. Must stay in sync with the design token `SoniqAccents.Violet`. */
        const val DEFAULT_ACCENT_ARGB: Int = 0xFF7C5CFF.toInt()
    }
}

/** A named, saved snapshot of an [AppearanceConfig]. */
data class ThemePreset(
    val id: Long,
    val name: String,
    val config: AppearanceConfig,
    val createdAtEpochMillis: Long,
)
