package com.soniq.player.feature.player

import androidx.lifecycle.ViewModel
import androidx.media3.common.Player
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.VideoItem
import com.soniq.player.core.playback.PlaybackController
import com.soniq.player.core.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Presentation-layer bridge to the application-scoped [PlaybackController].
 * Holds no player instances or media lifecycles of its own.
 */
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackController.state
    val player: StateFlow<Player?> = playbackController.player

    fun ensureConnected() {
        playbackController.connect()
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
        if (playbackController.state.value.activeMediaKind == MediaKind.VIDEO) {
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
