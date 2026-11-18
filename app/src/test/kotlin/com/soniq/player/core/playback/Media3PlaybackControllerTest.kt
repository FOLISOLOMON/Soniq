package com.soniq.player.core.playback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Media3PlaybackControllerTest {

    @Test
    fun disconnectAndReconnectResetStateSafely() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = Media3PlaybackController(context)

        assertThat(controller.state.value).isEqualTo(PlaybackState())

        // Connecting, disconnecting while in flight, handling service disconnection, and
        // reconnecting must leave state clean and never throw.
        controller.connect()
        controller.disconnect()
        assertThat(controller.state.value).isEqualTo(PlaybackState())

        controller.connect()
        controller.handleServiceDisconnectedForTest()
        assertThat(controller.state.value).isEqualTo(PlaybackState())

        // Safe no-ops while disconnected.
        controller.play()
        controller.pause()
        controller.stop()
        controller.seekTo(1_000L)
        assertThat(controller.state.value).isEqualTo(PlaybackState())
    }
}
