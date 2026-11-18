package com.soniq.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.soniq.player.core.designsystem.component.SoniqBackground
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.VideoItem
import com.soniq.player.core.playback.AudioEffectsState
import com.soniq.player.core.playback.PlaybackState
import com.soniq.player.feature.music.MusicRoute
import com.soniq.player.feature.player.MiniPlayerBar
import com.soniq.player.feature.player.NowPlayingScreen
import com.soniq.player.feature.player.VideoPlayerScreen
import com.soniq.player.feature.settings.SettingsRoute
import com.soniq.player.feature.video.VideoRoute
import com.soniq.player.navigation.SoniqNavHost
import com.soniq.player.navigation.SoniqRoute
import com.soniq.player.navigation.TopLevelDestination
import com.soniq.player.navigation.isFullScreenPlayer
import com.soniq.player.navigation.isTopLevelSelected
import com.soniq.player.navigation.navigateToTopLevel

/**
 * Root composable shell rendering the user-customized [SoniqBackground], the adaptive Material 3
 * navigation chrome (hidden on full-screen player destinations), the docked audio [MiniPlayerBar],
 * and the [SoniqNavHost].
 */
@Composable
fun SoniqApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    playbackState: PlaybackState = PlaybackState(),
    audioEffectsState: AudioEffectsState = AudioEffectsState(),
    isCurrentFavorite: Boolean = false,
    player: Player? = null,
    onPlayAudioQueue: (List<AudioTrack>, Int) -> Unit = { _, _ -> },
    onPlayNext: (AudioTrack) -> Unit = {},
    onAddToQueue: (AudioTrack) -> Unit = {},
    onPlayVideo: (VideoItem) -> Unit = {},
    onTogglePlayPause: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    onSeekBy: (Long) -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onCycleRepeatMode: () -> Unit = {},
    onCycleSpeed: () -> Unit = {},
    onSetSpeed: (Float) -> Unit = {},
    onPauseIfVideo: () -> Unit = {},
    onToggleCurrentFavorite: () -> Unit = {},
    onSelectQueueItem: (Int) -> Unit = {},
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onRemoveQueueItem: (Int) -> Unit = {},
    onSetEqualizerEnabled: (Boolean) -> Unit = {},
    onSelectEqualizerPreset: (Int) -> Unit = {},
    onSetEqualizerBandLevel: (Int, Int) -> Unit = { _, _ -> },
    onSetBassBoostEnabled: (Boolean) -> Unit = {},
    onSetBassBoostStrength: (Int) -> Unit = {},
    onSetGaplessEnabled: (Boolean) -> Unit = {},
    onSetCrossfadeDurationSec: (Int) -> Unit = {},
    onSelectAudioTrack: (Int, Int) -> Unit = { _, _ -> },
    onSelectSubtitleTrack: (Int?, Int?) -> Unit = { _, _ -> },
    onAddExternalSubtitle: (String, String, String) -> Unit = { _, _, _ -> },
    onClearPlaybackError: () -> Unit = {},
    musicContent: @Composable () -> Unit = {
        MusicRoute(
            onPlayTrack = { tracks, index ->
                onPlayAudioQueue(tracks, index)
                navController.navigate(SoniqRoute.AudioPlayer) { launchSingleTop = true }
            },
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
        )
    },
    videoContent: @Composable () -> Unit = {
        VideoRoute(
            onPlayVideo = { video ->
                onPlayVideo(video)
                navController.navigate(SoniqRoute.VideoPlayer) { launchSingleTop = true }
            },
        )
    },
    settingsContent: @Composable () -> Unit = { SettingsRoute() },
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isFullScreenPlayer = currentDestination.isFullScreenPlayer()
    val layoutType = if (isFullScreenPlayer) {
        NavigationSuiteType.None
    } else {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    }
    val showMiniPlayer = !isFullScreenPlayer &&
        playbackState.hasActiveItem &&
        playbackState.activeMediaKind == MediaKind.AUDIO

    SoniqBackground(modifier = modifier) {
        NavigationSuiteScaffold(
            layoutType = layoutType,
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
            Column(modifier = Modifier.fillMaxSize()) {
                SoniqNavHost(
                    navController = navController,
                    modifier = Modifier.weight(1f),
                    musicContent = musicContent,
                    videoContent = videoContent,
                    settingsContent = settingsContent,
                    audioPlayerContent = {
                        NowPlayingScreen(
                            playbackState = playbackState,
                            isFavorite = isCurrentFavorite,
                            onNavigateBack = { navController.popBackStack() },
                            onTogglePlayPause = onTogglePlayPause,
                            onSkipNext = onSkipNext,
                            onSkipPrevious = onSkipPrevious,
                            onSeekTo = onSeekTo,
                            onToggleShuffle = onToggleShuffle,
                            onCycleRepeatMode = onCycleRepeatMode,
                            onCycleSpeed = onCycleSpeed,
                            onToggleFavorite = onToggleCurrentFavorite,
                            onSelectQueueItem = onSelectQueueItem,
                            onMoveQueueItem = onMoveQueueItem,
                            onRemoveQueueItem = onRemoveQueueItem,
                            audioEffectsState = audioEffectsState,
                            onSetEqualizerEnabled = onSetEqualizerEnabled,
                            onSelectEqualizerPreset = onSelectEqualizerPreset,
                            onSetEqualizerBandLevel = onSetEqualizerBandLevel,
                            onSetBassBoostEnabled = onSetBassBoostEnabled,
                            onSetBassBoostStrength = onSetBassBoostStrength,
                            onSetGaplessEnabled = onSetGaplessEnabled,
                            onSetCrossfadeDurationSec = onSetCrossfadeDurationSec,
                        )
                    },
                    videoPlayerContent = {
                        VideoPlayerScreen(
                            player = player,
                            playbackState = playbackState,
                            onNavigateBack = { navController.popBackStack() },
                            onTogglePlayPause = onTogglePlayPause,
                            onSeekTo = onSeekTo,
                            onSeekBy = onSeekBy,
                            onCycleSpeed = onCycleSpeed,
                            onLeaveScreen = onPauseIfVideo,
                            onSetSpeed = onSetSpeed,
                            onSelectAudioTrack = onSelectAudioTrack,
                            onSelectSubtitleTrack = onSelectSubtitleTrack,
                            onAddExternalSubtitle = onAddExternalSubtitle,
                            onClearError = onClearPlaybackError,
                        )
                    },
                )
                if (showMiniPlayer) {
                    MiniPlayerBar(
                        playbackState = playbackState,
                        onOpenPlayer = {
                            navController.navigate(SoniqRoute.AudioPlayer) {
                                launchSingleTop = true
                            }
                        },
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                    )
                }
            }
        }
    }
}
