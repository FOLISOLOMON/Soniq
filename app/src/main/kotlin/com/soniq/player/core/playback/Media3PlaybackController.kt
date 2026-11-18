package com.soniq.player.core.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.soniq.player.core.model.media.MediaKind
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-scoped [PlaybackController] backed by a Media3 [MediaController] connected to
 * [SoniqPlaybackService].
 */
@Singleton
class Media3PlaybackController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PlaybackController {

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var isConnecting: Boolean = false

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishState(player)
        }
    }

    override fun connect() {
        if (controller != null || isConnecting) return
        isConnecting = true
        val token = SessionToken(context, ComponentName(context, SoniqPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                isConnecting = false
                try {
                    val connected = future.get()
                    controller = connected
                    connected.addListener(listener)
                    publishState(connected)
                } catch (error: ExecutionException) {
                    Log.w(TAG, "Failed to connect to SoniqPlaybackService", error)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    override fun play() {
        controller?.play()
    }

    override fun pause() {
        controller?.pause()
    }

    override fun stop() {
        controller?.stop()
    }

    override fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    private fun publishState(player: Player) {
        val currentItem = player.currentMediaItem
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0L } ?: 0L
        _state.value = PlaybackState(
            status = player.playbackState.toPlaybackStatus(),
            isPlaying = player.isPlaying,
            activeMediaId = currentItem?.mediaId?.takeIf { it.isNotBlank() },
            activeMediaKind = currentItem?.inferMediaKind(),
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
        )
    }

    private companion object {
        const val TAG = "Media3PlaybackCtrl"

        fun Int.toPlaybackStatus(): PlaybackStatus = when (this) {
            Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
            Player.STATE_READY -> PlaybackStatus.READY
            Player.STATE_ENDED -> PlaybackStatus.ENDED
            else -> PlaybackStatus.IDLE
        }

        fun MediaItem.inferMediaKind(): MediaKind? {
            val mime = localConfiguration?.mimeType ?: return null
            return when {
                mime.startsWith("video/") -> MediaKind.VIDEO
                mime.startsWith("audio/") -> MediaKind.AUDIO
                else -> null
            }
        }
    }
}
