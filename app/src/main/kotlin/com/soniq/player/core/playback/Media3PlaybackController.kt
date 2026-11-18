package com.soniq.player.core.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.VideoItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-scoped [PlaybackController] backed by a Media3 [MediaController] connected to
 * [SoniqPlaybackService].
 *
 * Handles cold-start command queuing (if playback is requested before async binding finishes),
 * active-only position polling, service-initiated disconnections, and explicit [disconnect]
 * release cleanly without leaking controllers or holding stale references.
 */
@Singleton
class Media3PlaybackController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PlaybackController {

    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _player = MutableStateFlow<Player?>(null)
    override val player: StateFlow<Player?> = _player.asStateFlow()

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingCommand: ((MediaController) -> Unit)? = null
    private var positionTickerJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishState(player)
            updatePositionTicker(player)
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
                    _player.value = connected
                    connected.addListener(playerListener)
                    val queued = pendingCommand
                    pendingCommand = null
                    queued?.invoke(connected)
                    publishState(connected)
                    updatePositionTicker(connected)
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

    override fun playAudioQueue(tracks: List<AudioTrack>, startIndex: Int) {
        if (tracks.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val mediaItems = tracks.map(MediaItemFactory::fromAudioTrack)
        executeWhenConnected { player ->
            player.setMediaItems(mediaItems, clampedIndex, 0L)
            player.prepare()
            player.play()
        }
    }

    override fun playVideo(video: VideoItem) {
        val mediaItem = MediaItemFactory.fromVideoItem(video)
        executeWhenConnected { player ->
            player.setMediaItem(mediaItem, 0L)
            player.prepare()
            player.play()
        }
    }

    override fun togglePlayPause() {
        val active = controller ?: return
        if (active.isPlaying) {
            active.pause()
        } else {
            if (active.playbackState == Player.STATE_ENDED) {
                active.seekTo(0L)
            }
            active.play()
        }
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

    override fun skipToNext() {
        val active = controller ?: return
        if (active.hasNextMediaItem()) {
            active.seekToNextMediaItem()
        }
    }

    override fun skipToPrevious() {
        val active = controller ?: return
        if (active.currentPosition > PREVIOUS_RESTART_THRESHOLD_MS || !active.hasPreviousMediaItem()) {
            active.seekTo(0L)
        } else {
            active.seekToPreviousMediaItem()
        }
    }

    override fun seekTo(positionMs: Long) {
        val active = controller ?: return
        val duration = active.duration.takeIf { it != C.TIME_UNSET && it >= 0L }
        val target = if (duration != null) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        active.seekTo(target)
        publishState(active)
    }

    override fun seekBy(deltaMs: Long) {
        val active = controller ?: return
        seekTo(active.currentPosition + deltaMs)
    }

    override fun cycleRepeatMode() {
        val active = controller ?: return
        active.repeatMode = when (active.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    override fun toggleShuffle() {
        val active = controller ?: return
        active.shuffleModeEnabled = !active.shuffleModeEnabled
    }

    override fun setPlaybackSpeed(speed: Float) {
        val active = controller ?: return
        val clamped = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        active.playbackParameters = PlaybackParameters(clamped)
    }

    internal fun handleServiceDisconnectedForTest() {
        clearConnection(releaseFuture = false)
    }

    private fun executeWhenConnected(action: (MediaController) -> Unit) {
        val active = controller
        if (active != null && active.isConnected) {
            action(active)
        } else {
            pendingCommand = action
            connect()
        }
    }

    private fun updatePositionTicker(player: Player) {
        if (player.isPlaying) {
            if (positionTickerJob?.isActive == true) return
            positionTickerJob = controllerScope.launch {
                while (isActive) {
                    delay(POSITION_TICK_MILLIS)
                    val active = controller ?: break
                    publishState(active)
                }
            }
        } else {
            positionTickerJob?.cancel()
            positionTickerJob = null
        }
    }

    private fun clearConnection(releaseFuture: Boolean) {
        positionTickerJob?.cancel()
        positionTickerJob = null
        pendingCommand = null
        controller?.removeListener(playerListener)
        controller = null
        _player.value = null
        val future = controllerFuture
        controllerFuture = null
        if (releaseFuture && future != null) {
            MediaController.releaseFuture(future)
        }
        _state.value = PlaybackState()
    }

    private fun publishState(player: Player) {
        val currentItem = player.currentMediaItem
        val metadata = currentItem?.mediaMetadata
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0L } ?: 0L
        val subtitle = metadata?.artist?.toString()?.takeIf { it.isNotBlank() }
            ?: metadata?.subtitle?.toString()?.takeIf { it.isNotBlank() }

        _state.value = PlaybackState(
            status = player.playbackState.toPlaybackStatus(),
            isPlaying = player.isPlaying,
            activeMediaId = currentItem?.mediaId?.takeIf { it.isNotBlank() },
            activeMediaKind = MediaItemFactory.resolveMediaKind(currentItem),
            title = metadata?.title?.toString()?.takeIf { it.isNotBlank() },
            subtitle = subtitle,
            artworkUri = metadata?.artworkUri?.toString()?.takeIf { it.isNotBlank() },
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
            repeatMode = player.repeatMode.toDomainRepeatMode(),
            shuffleEnabled = player.shuffleModeEnabled,
            playbackSpeed = player.playbackParameters.speed,
            queueCount = player.mediaItemCount,
            currentQueueIndex = player.currentMediaItemIndex.coerceAtLeast(0),
        )
    }

    private companion object {
        const val TAG = "Media3PlaybackCtrl"
        const val POSITION_TICK_MILLIS = 250L
        const val PREVIOUS_RESTART_THRESHOLD_MS = 3_000L
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 2.5f

        fun Int.toPlaybackStatus(): PlaybackStatus = when (this) {
            Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
            Player.STATE_READY -> PlaybackStatus.READY
            Player.STATE_ENDED -> PlaybackStatus.ENDED
            else -> PlaybackStatus.IDLE
        }

        fun Int.toDomainRepeatMode(): PlaybackRepeatMode = when (this) {
            Player.REPEAT_MODE_ALL -> PlaybackRepeatMode.ALL
            Player.REPEAT_MODE_ONE -> PlaybackRepeatMode.ONE
            else -> PlaybackRepeatMode.OFF
        }
    }
}
