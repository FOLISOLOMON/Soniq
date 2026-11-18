package com.soniq.player.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.soniq.player.feature.music.MusicRoute
import com.soniq.player.feature.settings.SettingsRoute
import com.soniq.player.feature.video.VideoRoute

/**
 * Navigates between top-level destinations while preserving each tab's back stack and avoiding
 * duplicate entries when re-selecting the active tab.
 */
fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/** Returns `true` when [this] destination belongs to [destination]'s route hierarchy. */
fun NavDestination?.isTopLevelSelected(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true

/** Returns `true` when [this] destination is one of the full-screen player routes. */
fun NavDestination?.isFullScreenPlayer(): Boolean =
    this?.hierarchy?.any {
        it.hasRoute(SoniqRoute.AudioPlayer::class) || it.hasRoute(SoniqRoute.VideoPlayer::class)
    } == true

/**
 * Root navigation graph for SONIQ. Feature content lambdas are parameterized with production
 * defaults so navigation behavior can be verified in isolation during UI tests.
 */
@Composable
fun SoniqNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: SoniqRoute = SoniqRoute.Music,
    musicContent: @Composable () -> Unit = { MusicRoute() },
    videoContent: @Composable () -> Unit = { VideoRoute() },
    settingsContent: @Composable () -> Unit = { SettingsRoute() },
    audioPlayerContent: @Composable () -> Unit = {},
    videoPlayerContent: @Composable () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<SoniqRoute.Music> { musicContent() }
        composable<SoniqRoute.Video> { videoContent() }
        composable<SoniqRoute.Settings> { settingsContent() }
        composable<SoniqRoute.AudioPlayer> { audioPlayerContent() }
        composable<SoniqRoute.VideoPlayer> { videoPlayerContent() }
    }
}
