package com.soniq.player.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.soniq.player.core.datastore.PlaybackPreferences
import com.soniq.player.core.datastore.PlaybackPreferencesDataSource
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.VideoItem
import com.soniq.player.core.permissions.NotificationPermissionChecker
import com.soniq.player.core.permissions.NotificationPermissions
import com.soniq.player.core.playback.PlaybackController
import com.soniq.player.core.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Presentation-layer bridge to the application-scoped [PlaybackController].
 * Holds no player instances or media lifecycles of its own.
 */
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val playbackPreferences: PlaybackPreferencesDataSource,
    private val notificationPermissionChecker: NotificationPermissionChecker,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackController.state
    val player: StateFlow<Player?> = playbackController.player

    val preferences: StateFlow<PlaybackPreferences> = playbackPreferences.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PlaybackPreferences(),
        )

    @Volatile
    private var notificationPromptDispatched: Boolean = false

    fun ensureConnected() {
        playbackController.connect()
    }

    /**
     * Returns `true` when Android 13+ optional `POST_NOTIFICATIONS` should be requested once
     * before/alongside playback so notification and lock-screen controls are visible.
     * Playback itself proceeds regardless of the user's answer.
     */
    fun shouldPromptForNotifications(): Boolean =
        NotificationPermissions.isRuntimePermissionRequired() &&
            !notificationPermissionChecker.canPostNotifications() &&
            !notificationPromptDispatched &&
            !preferences.value.notificationPromptShown

    fun markNotificationPromptShown() {
        notificationPromptDispatched = true
        viewModelScope.launch {
            playbackPreferences.markNotificationPromptShown()
        }
    }

    fun playAudioQueue(tracks: List<AudioTrack>, startIndex: Int) {
        playbackController.playAudioQueue(tracks, startIndex)
    }

    fun playVideo(video: VideoItem) {
        playbackController.playVideo(video)
    }

    fun togglePlayPause() {
        playbackController.togglePlayPause()
    }

    fun pauseIfVideo() {
        if (
            playbackController.state.value.activeMediaKind == MediaKind.VIDEO &&
            !preferences.value.continueVideoInBackground
        ) {
            playbackController.pause()
        }
    }

    fun skipToNext() {
        playbackController.skipToNext()
    }

    fun skipToPrevious() {
        playbackController.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        playbackController.seekTo(positionMs)
    }

    fun seekBy(deltaMs: Long) {
        playbackController.seekBy(deltaMs)
    }

    fun cycleRepeatMode() {
        playbackController.cycleRepeatMode()
    }

    fun toggleShuffle() {
        playbackController.toggleShuffle()
    }

    fun cyclePlaybackSpeed() {
        val current = playbackController.state.value.playbackSpeed
        val nextIndex = SPEED_STEPS.indexOfFirst { it > current + SPEED_EPSILON }
            .let { idx -> if (idx >= 0) idx else 0 }
        playbackController.setPlaybackSpeed(SPEED_STEPS[nextIndex])
    }

    companion object {
        val SPEED_STEPS: List<Float> = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        private const val SPEED_EPSILON = 0.05f

        fun formatSpeed(speed: Float): String =
            if (speed == speed.toInt().toFloat()) {
                speed.toInt().toString()
            } else {
                speed.toString().trimEnd('0').trimEnd('.')
            }
    }
}
