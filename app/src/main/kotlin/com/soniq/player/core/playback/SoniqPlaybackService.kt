package com.soniq.player.core.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.soniq.player.MainActivity
import com.soniq.player.core.model.media.MediaKind
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Standalone playback service hosting SONIQ's single ExoPlayer and MediaSession.
 *
 * Lives independently of Activity and Compose lifecycles so audio playback continues seamlessly
 * across configuration changes, navigation, and backgrounding, and exposes standard controls to
 * system media surfaces (notification shade, lock screen, Bluetooth, Android Auto).
 */
@AndroidEntryPoint
class SoniqPlaybackService : MediaSessionService() {

    @Inject
    lateinit var player: ExoPlayer

    private var mediaSession: MediaSession? = null
    private var currentAudioContentType: Int = C.AUDIO_CONTENT_TYPE_MUSIC

    private val audioAttributeListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
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
    }

    override fun onCreate() {
        super.onCreate()
        player.addListener(audioAttributeListener)
        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = mediaSession?.player
        val isActivelyPlaying = currentPlayer != null &&
            currentPlayer.playWhenReady &&
            currentPlayer.mediaItemCount > 0 &&
            currentPlayer.playbackState != Player.STATE_ENDED
        if (!isActivelyPlaying) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        player.removeListener(audioAttributeListener)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
