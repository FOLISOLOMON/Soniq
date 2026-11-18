package com.soniq.player.core.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.soniq.player.MainActivity
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

    override fun onCreate() {
        super.onCreate()
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
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
