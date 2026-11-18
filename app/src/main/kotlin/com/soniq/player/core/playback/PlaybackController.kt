package com.soniq.player.core.playback

import com.soniq.player.core.model.media.MediaKind
import kotlinx.coroutines.flow.StateFlow

/** High-level playback engine state, decoupled from Media3 internals. */
enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
}

/** Immutable snapshot of the shared playback engine. */
data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val isPlaying: Boolean = false,
    val activeMediaId: String? = null,
    val activeMediaKind: MediaKind? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

/**
 * Shared playback controller contract used by both Music and Video experiences.
 *
 * Implementations bind to [SoniqPlaybackService] using the application context so playback never
 * depends on an Activity or Compose screen lifecycle.
 */
interface PlaybackController {
    val state: StateFlow<PlaybackState>

    fun connect()
    fun disconnect()
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
}
