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

/** Aspect-ratio resize mode for video playback. */
enum class VideoResizeMode {
    FIT,
    FILL,
    ZOOM,
}

/** Selectable embedded or external audio/subtitle track inside the active media item. */
data class MediaTrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean,
)

/** Lightweight projection of an item currently in the active playback queue. */
data class QueueItem(
    val index: Int,
    val mediaId: String,
    val title: String,
    val subtitle: String?,
    val artworkUri: String?,
    val isCurrent: Boolean,
)

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
    val queue: List<QueueItem> = emptyList(),
    val playbackError: String? = null,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val availableAudioTracks: List<MediaTrackOption> = emptyList(),
    val availableSubtitleTracks: List<MediaTrackOption> = emptyList(),
    val activeSubtitleCues: List<String> = emptyList(),
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
    fun playNext(track: AudioTrack)
    fun addToQueue(track: AudioTrack)
    fun skipToQueueIndex(index: Int)
    fun removeQueueItem(index: Int)
    fun moveQueueItem(fromIndex: Int, toIndex: Int)
    fun playVideo(video: VideoItem, startPositionMs: Long = 0L)
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
    fun selectAudioTrack(groupIndex: Int, trackIndex: Int)
    fun selectSubtitleTrack(groupIndex: Int?, trackIndex: Int?)
    fun addExternalSubtitle(uri: String, mimeType: String, label: String)
    fun clearPlaybackError()
}
