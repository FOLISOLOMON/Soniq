package com.soniq.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.designsystem.theme.SoniqTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SoniqLaunchEntranceTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun launchEntranceWaitsForReadyThenAnimatesSymbolAndWordmarkOnce() {
        var ready by mutableStateOf(false)
        var finishedCount = 0

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SoniqTheme {
                SoniqLaunchEntrance(
                    isReady = ready,
                    onFinished = { finishedCount++ },
                )
            }
        }

        composeRule.onNodeWithTag(LAUNCH_ENTRANCE_TEST_TAG).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(1_000L)
        assertThat(finishedCount).isEqualTo(0)

        ready = true
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.onNodeWithTag(LAUNCH_SYMBOL_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(LAUNCH_WORDMARK_TEST_TAG).assertIsDisplayed()
        assertThat(finishedCount).isEqualTo(0)

        composeRule.mainClock.advanceTimeBy(2_000L)
        composeRule.waitForIdle()
        assertThat(finishedCount).isEqualTo(1)
    }
}
