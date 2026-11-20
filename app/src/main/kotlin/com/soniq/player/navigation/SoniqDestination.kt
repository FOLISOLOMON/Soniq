package com.soniq.player.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.soniq.player.R
import kotlinx.serialization.Serializable

/** Type-safe Navigation Compose routes for SONIQ's top-level graphs. */
sealed interface SoniqRoute {
    @Serializable
    data object Home : SoniqRoute

    @Serializable
    data object Music : SoniqRoute

    @Serializable
    data object Video : SoniqRoute

    @Serializable
    data object Settings : SoniqRoute

    @Serializable
    data object AudioPlayer : SoniqRoute

    @Serializable
    data object VideoPlayer : SoniqRoute
}

/**
 * Top-level navigation destinations displayed in SONIQ's Liquid Glass navigation bar.
 * Music and Video remain logically separate experiences.
 */
enum class TopLevelDestination(
    val route: SoniqRoute,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    HOME(
        route = SoniqRoute.Home,
        labelRes = R.string.destination_home,
        iconRes = R.drawable.ic_home,
    ),
    MUSIC(
        route = SoniqRoute.Music,
        labelRes = R.string.destination_music,
        iconRes = R.drawable.ic_music_note,
    ),
    VIDEO(
        route = SoniqRoute.Video,
        labelRes = R.string.destination_video,
        iconRes = R.drawable.ic_movie,
    ),
    SETTINGS(
        route = SoniqRoute.Settings,
        labelRes = R.string.destination_settings,
        iconRes = R.drawable.ic_settings,
    ),
}
