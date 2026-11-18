package com.soniq.player

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.soniq.player.core.designsystem.component.SoniqBackground
import com.soniq.player.navigation.SoniqNavHost
import com.soniq.player.navigation.TopLevelDestination
import com.soniq.player.navigation.isTopLevelSelected
import com.soniq.player.navigation.navigateToTopLevel

/**
 * Root composable shell rendering the user-customized [SoniqBackground], the adaptive Material 3
 * navigation chrome (bottom bar on compact screens, rail on medium/expanded screens), and the
 * [SoniqNavHost].
 */
@Composable
fun SoniqApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    musicContent: @Composable () -> Unit = { com.soniq.player.feature.music.MusicRoute() },
    videoContent: @Composable () -> Unit = { com.soniq.player.feature.video.VideoRoute() },
    settingsContent: @Composable () -> Unit = { com.soniq.player.feature.settings.SettingsRoute() },
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    SoniqBackground(modifier = modifier) {
        NavigationSuiteScaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            navigationSuiteItems = {
                TopLevelDestination.entries.forEach { destination ->
                    item(
                        selected = currentDestination.isTopLevelSelected(destination),
                        onClick = { navController.navigateToTopLevel(destination) },
                        icon = {
                            Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = stringResource(destination.labelRes),
                            )
                        },
                        label = { Text(text = stringResource(destination.labelRes)) },
                    )
                }
            },
        ) {
            SoniqNavHost(
                navController = navController,
                musicContent = musicContent,
                videoContent = videoContent,
                settingsContent = settingsContent,
            )
        }
    }
}
