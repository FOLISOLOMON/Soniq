package com.soniq.player

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.designsystem.theme.shouldUseDarkTheme
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.media.MediaKind
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
    private val inPipMode = mutableStateOf(false)

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
            val showLaunchEntrance by mainViewModel.shouldShowLaunchEntrance.collectAsStateWithLifecycle()
            val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
            val audioEffectsState by playbackViewModel.audioEffectsState.collectAsStateWithLifecycle()
            val sleepTimerRemainingMs by playbackViewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
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

            LaunchedEffect(playbackState.isPlaying, playbackState.activeMediaKind) {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                ) {
                    val autoPip = playbackState.isPlaying && playbackState.activeMediaKind == MediaKind.VIDEO
                    setPictureInPictureParams(buildPipParams(autoEnter = autoPip))
                }
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
                Box(modifier = Modifier.fillMaxSize()) {
                    SoniqApp(
                        playbackState = playbackState,
                        audioEffectsState = audioEffectsState,
                        sleepTimerRemainingMs = sleepTimerRemainingMs,
                        isCurrentFavorite = isCurrentFavorite,
                        isInPipMode = inPipMode.value,
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
                        onSetSpeed = playbackViewModel::setPlaybackSpeed,
                        onPauseIfVideo = playbackViewModel::pauseIfVideo,
                        onToggleCurrentFavorite = playbackViewModel::toggleCurrentFavorite,
                        onSelectQueueItem = playbackViewModel::skipToQueueIndex,
                        onMoveQueueItem = playbackViewModel::moveQueueItem,
                        onRemoveQueueItem = playbackViewModel::removeQueueItem,
                        onSetEqualizerEnabled = playbackViewModel::setEqualizerEnabled,
                        onSelectEqualizerPreset = playbackViewModel::selectEqualizerPreset,
                        onSetEqualizerBandLevel = playbackViewModel::setEqualizerBandLevel,
                        onSetBassBoostEnabled = playbackViewModel::setBassBoostEnabled,
                        onSetBassBoostStrength = playbackViewModel::setBassBoostStrength,
                        onSetGaplessEnabled = playbackViewModel::setGaplessEnabled,
                        onSetCrossfadeDurationSec = playbackViewModel::setCrossfadeDurationSec,
                        onStartSleepTimer = playbackViewModel::startSleepTimer,
                        onCancelSleepTimer = playbackViewModel::cancelSleepTimer,
                        onEnterPip = ::enterPipModeIfSupported,
                        onSelectAudioTrack = playbackViewModel::selectAudioTrack,
                        onSelectSubtitleTrack = playbackViewModel::selectSubtitleTrack,
                        onAddExternalSubtitle = playbackViewModel::addExternalSubtitle,
                        onClearPlaybackError = playbackViewModel::clearPlaybackError,
                    )

                    if (showLaunchEntrance && !inPipMode.value) {
                        SoniqLaunchEntrance(
                            isReady = uiState is MainUiState.Ready,
                            onFinished = mainViewModel::onLaunchEntranceFinished,
                        )
                    }
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPipMode.value = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val state = playbackViewModel.playbackState.value
        if (state.isPlaying && state.activeMediaKind == MediaKind.VIDEO) {
            enterPipModeIfSupported()
        }
    }

    private fun buildPipParams(autoEnter: Boolean): PictureInPictureParams {
        val state = playbackViewModel.playbackState.value
        val w = state.videoWidth.takeIf { it > 0 } ?: 16
        val h = state.videoHeight.takeIf { it > 0 } ?: 9
        val ratio = Rational(w, h).coerceIn(Rational(1, 2), Rational(2, 1))
        val decor = window.decorView
        val sourceRect = Rect(0, 0, decor.width.coerceAtLeast(1), decor.height.coerceAtLeast(1))
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(ratio)
            .setSourceRectHint(sourceRect)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
        }
        return builder.build()
    }

    private fun enterPipModeIfSupported() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
        enterPictureInPictureMode(buildPipParams(autoEnter = true))
    }

    private companion object {
        // Standard Material edge-to-edge scrims for 3-button navigation bars.
        val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
