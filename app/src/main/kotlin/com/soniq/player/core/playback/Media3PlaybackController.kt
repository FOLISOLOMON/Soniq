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
import com.google.common.util.concurrent.ListenableFuture
import com.soniq.player.core.model.media.MediaKind
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-scoped [PlaybackController] backed by a Media3 [MediaController] connected to
 * [SoniqPlaybackService].
 *
 * Handles service-initiated disconnections and explicit [disconnect] calls cleanly so subsequent
 * [connect] invocations establish a fresh session without leaking controllers or holding stale
 * references.
 */
@Singleton
class Media3PlaybackController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PlaybackController {

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishState(player)
        }
    }

    private val controllerListener = object : MediaController.Listener {
        override fun onDisconnected(disconnectedController: MediaController) {
            if (controller === disconnectedController) {
                clearConnection(releaseFuture = false)
            }
        }
    }

    override fun connect() {
        if (controller != null || controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, SoniqPlaybackService::class.java))
        val future = MediaController.Builder(context, token)
            .setListener(controllerListener)
            .buildAsync()
        controllerFuture = future
        future.addListener(
            {
                if (controllerFuture !== future) {
                    // disconnect() ran while the async connection was still in flight.
                    MediaController.releaseFuture(future)
                    return@addListener
                }
                try {
                    val connected = future.get()
                    if (!connected.isConnected) {
                        clearConnection(releaseFuture = true)
                        return@addListener
                    }
                    controller = connected
                    connected.addListener(playerListener)
                    publishState(connected)
                } catch (error: ExecutionException) {
                    Log.w(TAG, "Failed to connect to SoniqPlaybackService", error)
                    clearConnection(releaseFuture = false)
                } catch (_: CancellationException) {
                    clearConnection(releaseFuture = false)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    clearConnection(releaseFuture = false)
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    override fun disconnect() {
        clearConnection(releaseFuture = true)
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

    internal fun handleServiceDisconnectedForTest() {
        clearConnection(releaseFuture = false)
    }

    private fun clearConnection(releaseFuture: Boolean) {
        controller?.removeListener(playerListener)
        controller = null
        val future = controllerFuture
        controllerFuture = null
        if (releaseFuture && future != null) {
            MediaController.releaseFuture(future)
        }
        _state.value = PlaybackState()
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
