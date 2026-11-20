package com.soniq.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import com.soniq.player.core.designsystem.component.SoniqBackground
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.VideoItem
import com.soniq.player.core.playback.AudioEffectsState
import com.soniq.player.core.playback.PlaybackState
import com.soniq.player.feature.home.HomeRoute
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
 * Root composable shell rendering the Liquid Glass [SoniqBackground], floating frosted-glass
 * navigation bar (or NavigationRail on expanded screens), docked [MiniPlayerBar], and [SoniqNavHost].
 */
@Composable
fun SoniqApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    playbackState: PlaybackState = PlaybackState(),
    audioEffectsState: AudioEffectsState = AudioEffectsState(),
    sleepTimerRemainingMs: Long? = null,
    isCurrentFavorite: Boolean = false,
    isInPipMode: Boolean = false,
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
    onStartSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    onSelectAudioTrack: (Int, Int) -> Unit = { _, _ -> },
    onSelectSubtitleTrack: (Int?, Int?) -> Unit = { _, _ -> },
    onAddExternalSubtitle: (String, String, String) -> Unit = { _, _, _ -> },
    onClearPlaybackError: () -> Unit = {},
    homeContent: @Composable () -> Unit = {
        HomeRoute(
            onPlayTracks = { tracks, index ->
                onPlayAudioQueue(tracks, index)
                navController.navigate(SoniqRoute.AudioPlayer) { launchSingleTop = true }
            },
            onOpenMusicTab = { _ ->
                navController.navigateToTopLevel(TopLevelDestination.MUSIC)
            },
            onOpenVideo = { navController.navigateToTopLevel(TopLevelDestination.VIDEO) },
            onOpenSettings = { navController.navigateToTopLevel(TopLevelDestination.SETTINGS) },
        )
    },
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
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val useRail = !isFullScreenPlayer &&
        adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    val showMiniPlayer = !isFullScreenPlayer &&
        playbackState.hasActiveItem &&
        playbackState.activeMediaKind == MediaKind.AUDIO

    SoniqBackground(modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                SoniqGlassNavigationRail(
                    currentDestination = currentDestination,
                    onSelect = { navController.navigateToTopLevel(it) },
                )
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                SoniqNavHost(
                    navController = navController,
                    modifier = Modifier.weight(1f),
                    homeContent = homeContent,
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
                            sleepTimerRemainingMs = sleepTimerRemainingMs,
                            onStartSleepTimer = onStartSleepTimer,
                            onCancelSleepTimer = onCancelSleepTimer,
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
                            isInPipMode = isInPipMode,
                            onEnterPip = onEnterPip,
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
                            navController.navigate(SoniqRoute.AudioPlayer) { launchSingleTop = true }
                        },
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                    )
                }
                if (!isFullScreenPlayer && !useRail) {
                    SoniqGlassBottomBar(
                        currentDestination = currentDestination,
                        onSelect = { navController.navigateToTopLevel(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SoniqGlassBottomBar(
    currentDestination: NavDestination?,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val glass = SoniqTheme.glass
    val metrics = SoniqTheme.glassMetrics
    val spacing = SoniqTheme.tokens.spacing
    val barShape = RoundedCornerShape(metrics.navBarRadius)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(metrics.navBarHeight)
                .clip(barShape)
                .background(glass.navBarFill)
                .border(
                    width = metrics.borderWidth,
                    brush = Brush.linearGradient(listOf(glass.borderHighlight, glass.borderSubtle)),
                    shape = barShape,
                )
                .padding(horizontal = spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopLevelDestination.entries.forEach { destination ->
                val isSelected = currentDestination.isTopLevelSelected(destination)
                val label = stringResource(destination.labelRes)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(metrics.cardRadius))
                        .semantics { this.selected = isSelected }
                        .clickable(role = Role.Tab) { onSelect(destination) }
                        .padding(vertical = spacing.xs),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = 44.dp, height = 30.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(listOf(glass.activePillStart, glass.activePillEnd))
                                } else {
                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                },
                            ),
                    ) {
                        Icon(
                            painter = painterResource(destination.iconRes),
                            contentDescription = label,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SoniqGlassNavigationRail(
    currentDestination: NavDestination?,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val glass = SoniqTheme.glass
    NavigationRail(
        containerColor = glass.navBarFill,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            val isSelected = currentDestination.isTopLevelSelected(destination)
            val label = stringResource(destination.labelRes)
            NavigationRailItem(
                selected = isSelected,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = label,
                    )
                },
                label = { Text(text = label) },
            )
        }
    }
}
