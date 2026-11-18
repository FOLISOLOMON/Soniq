package com.soniq.player.core.playback

import androidx.media3.common.Player
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.VideoItem
import kotlinx.coroutines.flow.StateFlow

/** High-level playback engine state, decoupled from Media3 internals. */
enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
}

/** Repeat behavior for queued playback. */
enum class PlaybackRepeatMode {
    OFF,
    ALL,
    ONE,
}

/** Immutable snapshot of the shared playback engine. */
data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val isPlaying: Boolean = false,
    val activeMediaId: String? = null,
    val activeMediaKind: MediaKind? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val artworkUri: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val queueCount: Int = 0,
    val currentQueueIndex: Int = 0,
) {
    val hasActiveItem: Boolean get() = activeMediaId != null
}

/**
 * Shared playback controller contract used by both Music and Video experiences.
 *
 * Implementations bind to [SoniqPlaybackService] using the application context so playback never
 * depends on an Activity or Compose screen lifecycle.
 */
interface PlaybackController {
    val state: StateFlow<PlaybackState>

    /**
     * Exposes the service-backed [Player] handle strictly so Video playback UI can attach a
     * rendering surface without owning or releasing the underlying player instance.
     */
    val player: StateFlow<Player?>

    fun connect()
    fun disconnect()
    fun playAudioQueue(tracks: List<AudioTrack>, startIndex: Int = 0)
    fun playVideo(video: VideoItem)
    fun togglePlayPause()
    fun play()
    fun pause()
    fun stop()
    fun skipToNext()
    fun skipToPrevious()
    fun seekTo(positionMs: Long)
    fun seekBy(deltaMs: Long)
    fun cycleRepeatMode()
    fun toggleShuffle()
    fun setPlaybackSpeed(speed: Float)
}
