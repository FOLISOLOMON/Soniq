package com.soniq.player.core.playback

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import com.soniq.player.R

/**
 * Custom Media3 session commands and notification overflow button builders for SONIQ.
 *
 * Compact notification slots remain cleanly reserved for standard transport actions
 * (Previous / Play-Pause / Next), while Shuffle and Repeat are placed in [CommandButton.SLOT_OVERFLOW]
 * for expanded notifications, lock-screen overflow, and external MediaSession controllers.
 */
@OptIn(UnstableApi::class)
object SoniqSessionCommands {

    const val ACTION_TOGGLE_SHUFFLE = "com.soniq.player.action.TOGGLE_SHUFFLE"
    const val ACTION_CYCLE_REPEAT = "com.soniq.player.action.CYCLE_REPEAT"

    val COMMAND_TOGGLE_SHUFFLE = SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY)
    val COMMAND_CYCLE_REPEAT = SessionCommand(ACTION_CYCLE_REPEAT, Bundle.EMPTY)

    fun buildMediaButtonPreferences(
        context: Context,
        shuffleEnabled: Boolean,
        @Player.RepeatMode repeatMode: Int,
    ): List<CommandButton> {
        val shuffleButton = CommandButton.Builder(
            if (shuffleEnabled) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF,
        )
            .setSessionCommand(COMMAND_TOGGLE_SHUFFLE)
            .setDisplayName(
                context.getString(
                    if (shuffleEnabled) {
                        R.string.notification_action_shuffle_on
                    } else {
                        R.string.notification_action_shuffle_off
                    },
                ),
            )
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()

        val (repeatIcon, repeatLabelRes) = when (repeatMode) {
            Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL to R.string.notification_action_repeat_all
            Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE to R.string.notification_action_repeat_one
            else -> CommandButton.ICON_REPEAT_OFF to R.string.notification_action_repeat_off
        }
        val repeatButton = CommandButton.Builder(repeatIcon)
            .setSessionCommand(COMMAND_CYCLE_REPEAT)
            .setDisplayName(context.getString(repeatLabelRes))
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()

        return listOf(shuffleButton, repeatButton)
    }

    /** Extracts the numeric MediaStore audio track ID from a SONIQ `audio:<id>` media ID. */
    fun extractAudioTrackId(mediaId: String?): Long? {
        if (mediaId == null || !mediaId.startsWith(AUDIO_ID_PREFIX)) return null
        return mediaId.removePrefix(AUDIO_ID_PREFIX).toLongOrNull()
    }

    private const val AUDIO_ID_PREFIX = "audio:"
}
