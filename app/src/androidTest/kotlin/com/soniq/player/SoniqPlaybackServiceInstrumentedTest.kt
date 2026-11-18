package com.soniq.player

import android.content.ComponentName
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.playback.SoniqPlaybackService
import com.soniq.player.core.playback.SoniqSessionCommands
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SoniqPlaybackServiceInstrumentedTest {

    @Test
    fun mediaControllerConnectsExecutesCustomSessionCommandsAndReleasesCleanly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val token = SessionToken(context, ComponentName(context, SoniqPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        val controller = future.get(15, TimeUnit.SECONDS)

        try {
            assertThat(runOnMainSync { controller.isConnected }).isTrue()
            assertThat(
                runOnMainSync {
                    controller.isSessionCommandAvailable(SoniqSessionCommands.COMMAND_TOGGLE_SHUFFLE)
                },
            ).isTrue()
            assertThat(
                runOnMainSync {
                    controller.isSessionCommandAvailable(SoniqSessionCommands.COMMAND_CYCLE_REPEAT)
                },
            ).isTrue()

            val initialShuffle = runOnMainSync { controller.shuffleModeEnabled }
            val shuffleResult = runOnMainSync {
                controller.sendCustomCommand(SoniqSessionCommands.COMMAND_TOGGLE_SHUFFLE, Bundle.EMPTY)
            }.get(10, TimeUnit.SECONDS)
            assertThat(shuffleResult.resultCode).isEqualTo(SessionResult.RESULT_SUCCESS)
            awaitCondition { runOnMainSync { controller.shuffleModeEnabled } == !initialShuffle }
            assertThat(runOnMainSync { controller.shuffleModeEnabled }).isEqualTo(!initialShuffle)

            val repeatResult = runOnMainSync {
                controller.sendCustomCommand(SoniqSessionCommands.COMMAND_CYCLE_REPEAT, Bundle.EMPTY)
            }.get(10, TimeUnit.SECONDS)
            assertThat(repeatResult.resultCode).isEqualTo(SessionResult.RESULT_SUCCESS)
            awaitCondition { runOnMainSync { controller.repeatMode } == Player.REPEAT_MODE_ALL }
            assertThat(runOnMainSync { controller.repeatMode }).isEqualTo(Player.REPEAT_MODE_ALL)

            // Reset modes cleanly.
            runOnMainSync {
                controller.shuffleModeEnabled = initialShuffle
                controller.repeatMode = Player.REPEAT_MODE_OFF
            }
        } finally {
            runOnMainSync { MediaController.releaseFuture(future) }
        }
    }

    private fun awaitCondition(timeoutMs: Long = 3_000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(50L)
        }
    }

    private fun <T> runOnMainSync(block: () -> T): T {
        var result: T? = null
        var failure: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            try {
                result = block()
            } catch (error: Throwable) {
                failure = error
            }
        }
        failure?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }
}
