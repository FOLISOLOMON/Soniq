package com.soniq.player.core.playback

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SoniqSessionCommandsTest {

    @Test
    fun buildsOverflowNotificationButtonsForShuffleAndRepeat() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val buttons = SoniqSessionCommands.buildMediaButtonPreferences(
            context = context,
            shuffleEnabled = true,
            repeatMode = Player.REPEAT_MODE_ONE,
        )

        assertThat(buttons).hasSize(2)
        assertThat(buttons[0].sessionCommand).isEqualTo(SoniqSessionCommands.COMMAND_TOGGLE_SHUFFLE)
        assertThat(buttons[0].icon).isEqualTo(CommandButton.ICON_SHUFFLE_ON)
        assertThat(buttons[0].slots.asList()).containsExactly(CommandButton.SLOT_OVERFLOW)

        assertThat(buttons[1].sessionCommand).isEqualTo(SoniqSessionCommands.COMMAND_CYCLE_REPEAT)
        assertThat(buttons[1].icon).isEqualTo(CommandButton.ICON_REPEAT_ONE)
        assertThat(buttons[1].slots.asList()).containsExactly(CommandButton.SLOT_OVERFLOW)
    }

    @Test
    fun extractsAudioTrackIdAndRejectsNonAudioMediaIds() {
        assertThat(SoniqSessionCommands.extractAudioTrackId("audio:42")).isEqualTo(42L)
        assertThat(SoniqSessionCommands.extractAudioTrackId("video:42")).isNull()
        assertThat(SoniqSessionCommands.extractAudioTrackId("audio:not-a-number")).isNull()
        assertThat(SoniqSessionCommands.extractAudioTrackId(null)).isNull()
    }
}
