package com.soniq.player.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.soniq.player.R
import kotlinx.serialization.Serializable

/** Type-safe Navigation Compose routes for SONIQ's top-level graphs. */
sealed interface SoniqRoute {
    @Serializable
    data object Music : SoniqRoute

    @Serializable
    data object Video : SoniqRoute

    @Serializable
    data object Settings : SoniqRoute
}

/**
 * Top-level navigation destinations displayed in the adaptive navigation shell.
 * Music and Video are intentionally separate top-level experiences.
 */
enum class TopLevelDestination(
    val route: SoniqRoute,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
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
