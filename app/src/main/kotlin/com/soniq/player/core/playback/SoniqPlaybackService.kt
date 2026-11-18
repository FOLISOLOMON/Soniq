package com.soniq.player.core.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.soniq.player.MainActivity
import com.soniq.player.R
import com.soniq.player.core.common.di.IoDispatcher
import com.soniq.player.core.model.media.MediaKind
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Standalone playback service hosting SONIQ's single ExoPlayer and MediaSession.
 *
 * Lives independently of Activity and Compose lifecycles so audio playback continues seamlessly
 * across configuration changes, navigation, task dismissal, and screen-off backgrounding.
 * Provides branded Media3 notifications, offline album art loading, Bluetooth/headset media
 * button routing, dynamic audio focus & noisy-audio handling, and `onPlaybackResumption` queue
 * restoration.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class SoniqPlaybackService : MediaSessionService() {

    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var offlineBitmapLoader: SoniqOfflineBitmapLoader

    @Inject
    lateinit var restorationManager: PlaybackRestorationManager

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSession? = null
    private var currentAudioContentType: Int = C.AUDIO_CONTENT_TYPE_MUSIC

    private val servicePlayerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateAudioAttributesFor(mediaItem)
            persistAudioSnapshot()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) {
                persistAudioSnapshot()
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            refreshNotificationButtons()
            persistAudioSnapshot()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            refreshNotificationButtons()
            persistAudioSnapshot()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            persistAudioSnapshot()
        }
    }

    private val sessionCallback = object : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SoniqSessionCommands.COMMAND_TOGGLE_SHUFFLE)
                .add(SoniqSessionCommands.COMMAND_CYCLE_REPEAT)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            return when (customCommand.customAction) {
                SoniqSessionCommands.ACTION_TOGGLE_SHUFFLE -> {
                    player.shuffleModeEnabled = !player.shuffleModeEnabled
                    refreshNotificationButtons()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                SoniqSessionCommands.ACTION_CYCLE_REPEAT -> {
                    player.repeatMode = when (player.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                    refreshNotificationButtons()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                else -> super.onCustomCommand(session, controller, customCommand, args)
            }
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                try {
                    val restored = restorationManager.restoreLastAudioSession()
                    if (restored == null) {
                        future.setException(IllegalStateException("No saved audio session to resume"))
                        return@launch
                    }
                    player.repeatMode = restored.repeatMode.toMedia3RepeatMode()
                    player.shuffleModeEnabled = restored.shuffleEnabled
                    player.playbackParameters = PlaybackParameters(restored.playbackSpeed)
                    refreshNotificationButtons()
                    future.set(
                        MediaSession.MediaItemsWithStartPosition(
                            restored.mediaItems,
                            restored.startIndex,
                            restored.startPositionMs,
                        ),
                    )
                } catch (error: Throwable) {
                    future.setException(error)
                }
            }
            return future
        }
    }

    override fun onCreate() {
        super.onCreate()
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.notification_channel_playback)
            .build()
            .apply { setSmallIcon(R.drawable.ic_music_note) }
        setMediaNotificationProvider(notificationProvider)

        player.addListener(servicePlayerListener)

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .setBitmapLoader(offlineBitmapLoader)
            .setSessionActivity(sessionActivityIntent)
            .setMediaButtonPreferences(
                SoniqSessionCommands.buildMediaButtonPreferences(
                    context = this,
                    shuffleEnabled = player.shuffleModeEnabled,
                    repeatMode = player.repeatMode,
                ),
            )
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = mediaSession?.player
        val activeKind = MediaItemFactory.resolveMediaKind(currentPlayer?.currentMediaItem)
        val keepRunningInBackground = currentPlayer != null &&
            activeKind == MediaKind.AUDIO &&
            currentPlayer.playWhenReady &&
            currentPlayer.mediaItemCount > 0 &&
            currentPlayer.playbackState != Player.STATE_ENDED

        if (!keepRunningInBackground) {
            persistAudioSnapshot()
            pauseAllPlayersAndStopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        player.removeListener(servicePlayerListener)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun updateAudioAttributesFor(mediaItem: MediaItem?) {
        val desiredType = when (MediaItemFactory.resolveMediaKind(mediaItem)) {
            MediaKind.VIDEO -> C.AUDIO_CONTENT_TYPE_MOVIE
            else -> C.AUDIO_CONTENT_TYPE_MUSIC
        }
        if (desiredType != currentAudioContentType) {
            currentAudioContentType = desiredType
            val attributes = AudioAttributes.Builder()
                .setContentType(desiredType)
                .setUsage(C.USAGE_MEDIA)
                .build()
            player.setAudioAttributes(attributes, /* handleAudioFocus = */ true)
        }
    }

    private fun refreshNotificationButtons() {
        mediaSession?.setMediaButtonPreferences(
            SoniqSessionCommands.buildMediaButtonPreferences(
                context = this,
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
            ),
        )
    }

    private fun persistAudioSnapshot() {
        val currentPlayer = mediaSession?.player ?: return
        if (MediaItemFactory.resolveMediaKind(currentPlayer.currentMediaItem) != MediaKind.AUDIO) return
        // Capture Player fields on the Main/Application looper before hopping to IO.
        val count = currentPlayer.mediaItemCount
        if (count == 0) return
        val ids = buildList(count) {
            for (i in 0 until count) {
                SoniqSessionCommands.extractAudioTrackId(currentPlayer.getMediaItemAt(i).mediaId)
                    ?.let(::add)
            }
        }
        if (ids.isEmpty()) return
        val index = currentPlayer.currentMediaItemIndex.coerceIn(0, ids.lastIndex)
        val position = currentPlayer.currentPosition.coerceAtLeast(0L)
        val repeat = currentPlayer.repeatMode.toDomainRepeatMode()
        val shuffle = currentPlayer.shuffleModeEnabled
        val speed = currentPlayer.playbackParameters.speed

        serviceScope.launch(ioDispatcher) {
            restorationManager.saveSnapshotDirectly(
                queueTrackIds = ids,
                currentIndex = index,
                positionMs = position,
                repeatMode = repeat,
                shuffleEnabled = shuffle,
                playbackSpeed = speed,
            )
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "soniq_playback_channel"

        private fun PlaybackRepeatMode.toMedia3RepeatMode(): Int = when (this) {
            PlaybackRepeatMode.OFF -> Player.REPEAT_MODE_OFF
            PlaybackRepeatMode.ALL -> Player.REPEAT_MODE_ALL
            PlaybackRepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }

        private fun Int.toDomainRepeatMode(): PlaybackRepeatMode = when (this) {
            Player.REPEAT_MODE_ALL -> PlaybackRepeatMode.ALL
            Player.REPEAT_MODE_ONE -> PlaybackRepeatMode.ONE
            else -> PlaybackRepeatMode.OFF
        }
    }
}
