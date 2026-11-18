package com.soniq.player

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.designsystem.theme.shouldUseDarkTheme
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.permissions.NotificationPermissions
import com.soniq.player.feature.player.PlaybackViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-Activity host for SONIQ's Compose UI.
 *
 * Holds no playback state: media playback lives in [com.soniq.player.core.playback.SoniqPlaybackService].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val playbackViewModel: PlaybackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Avoid a light/dark flash on cold start by keeping the splash screen until DataStore emits.
        splashScreen.setKeepOnScreenCondition {
            mainViewModel.uiState.value is MainUiState.Loading
        }

        enableEdgeToEdge()

        setContent {
            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
            val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
            val isCurrentFavorite by playbackViewModel.isCurrentFavorite.collectAsStateWithLifecycle()
            val player by playbackViewModel.player.collectAsStateWithLifecycle()

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) {
                playbackViewModel.markNotificationPromptShown()
            }

            LaunchedEffect(Unit) {
                playbackViewModel.ensureConnected()
            }

            // Request optional POST_NOTIFICATIONS once on API 33+ when playback first becomes active.
            LaunchedEffect(playbackState.isPlaying) {
                if (playbackState.isPlaying && playbackViewModel.shouldPromptForNotifications()) {
                    val permission = NotificationPermissions.requiredPermission()
                    if (permission != null) {
                        playbackViewModel.markNotificationPromptShown()
                        notificationPermissionLauncher.launch(permission)
                    }
                }
            }

            val appearance = when (val state = uiState) {
                is MainUiState.Ready -> state.appearance
                MainUiState.Loading -> AppearanceConfig()
            }
            val darkTheme = appearance.shouldUseDarkTheme()

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = LIGHT_SCRIM,
                        darkScrim = DARK_SCRIM,
                    ) { darkTheme },
                )
                onDispose {}
            }

            SoniqTheme(
                appearance = appearance,
                darkTheme = darkTheme,
            ) {
                SoniqApp(
                    playbackState = playbackState,
                    isCurrentFavorite = isCurrentFavorite,
                    player = player,
                    onPlayAudioQueue = playbackViewModel::playAudioQueue,
                    onPlayNext = playbackViewModel::playNext,
                    onAddToQueue = playbackViewModel::addToQueue,
                    onPlayVideo = playbackViewModel::playVideo,
                    onTogglePlayPause = playbackViewModel::togglePlayPause,
                    onSkipNext = playbackViewModel::skipToNext,
                    onSkipPrevious = playbackViewModel::skipToPrevious,
                    onSeekTo = playbackViewModel::seekTo,
                    onSeekBy = playbackViewModel::seekBy,
                    onToggleShuffle = playbackViewModel::toggleShuffle,
                    onCycleRepeatMode = playbackViewModel::cycleRepeatMode,
                    onCycleSpeed = playbackViewModel::cyclePlaybackSpeed,
                    onPauseIfVideo = playbackViewModel::pauseIfVideo,
                    onToggleCurrentFavorite = playbackViewModel::toggleCurrentFavorite,
                    onSelectQueueItem = playbackViewModel::skipToQueueIndex,
                    onMoveQueueItem = playbackViewModel::moveQueueItem,
                    onRemoveQueueItem = playbackViewModel::removeQueueItem,
                )
            }
        }
    }

    private companion object {
        // Standard Material edge-to-edge scrims for 3-button navigation bars.
        val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
