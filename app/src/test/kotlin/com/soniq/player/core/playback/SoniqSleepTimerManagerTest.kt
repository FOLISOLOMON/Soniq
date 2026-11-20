package com.soniq.player.core.playback

import com.google.common.truth.Truth.assertThat
import com.soniq.player.testutil.MainDispatcherRule
import org.junit.Rule
import org.junit.Test

class SoniqSleepTimerManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startsAndCancelsTimerCleanly() {
        val manager = SoniqSleepTimerManager()
        var expired = false
        manager.attachExpirationHandler { expired = true }

        assertThat(manager.remainingMillis.value).isNull()
        manager.startTimer(15)
        assertThat(manager.remainingMillis.value).isEqualTo(15 * 60_000L)

        manager.cancelTimer()
        assertThat(manager.remainingMillis.value).isNull()
        assertThat(expired).isFalse()

        manager.detachExpirationHandler()
    }
}
