package com.soniq.player.feature.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.PlayerLayout
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.playback.PlaybackState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlayerScreensUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val activeAudioState = PlaybackState(
        isPlaying = true,
        activeMediaId = "audio:1",
        activeMediaKind = MediaKind.AUDIO,
        title = "Kiasmos",
        subtitle = "Blurred",
        positionMs = 45_000L,
        durationMs = 240_000L,
        hasNext = true,
        hasPrevious = true,
        queueCount = 3,
        currentQueueIndex = 0,
    )

    @Test
    fun miniPlayerDisplaysActiveTrackAndTriggersCallbacks() {
        var opened = false
        var toggled = false

        composeRule.setContent {
            SoniqTheme {
                MiniPlayerBar(
                    playbackState = activeAudioState,
                    onOpenPlayer = { opened = true },
                    onTogglePlayPause = { toggled = true },
                    onSkipNext = {},
                )
            }
        }

        composeRule.onNodeWithText("Kiasmos").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Pause").performClick()

        assertThat(opened).isTrue()
        assertThat(toggled).isTrue()
    }

    @Test
    fun nowPlayingRendersCompactLayoutAndDispatchesTransportActions() {
        var speedClicked = false
        var pauseClicked = false

        composeRule.setContent {
            SoniqTheme(appearance = AppearanceConfig(playerLayout = PlayerLayout.COMPACT)) {
                NowPlayingScreen(
                    playbackState = activeAudioState,
                    onNavigateBack = {},
                    onTogglePlayPause = { pauseClicked = true },
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekTo = {},
                    onToggleShuffle = {},
                    onCycleRepeatMode = {},
                    onCycleSpeed = { speedClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Kiasmos").assertIsDisplayed()
        composeRule.onNodeWithText("1 / 3").assertIsDisplayed()
        composeRule.onNodeWithText("1x").performClick()
        composeRule.onNodeWithContentDescription("Pause").performClick()

        assertThat(speedClicked).isTrue()
        assertThat(pauseClicked).isTrue()
    }

    @Test
    fun videoPlayerDisplaysTitleAndSeekControls() {
        var soughtDelta = 0L

        composeRule.setContent {
            SoniqTheme {
                VideoPlayerScreen(
                    player = null,
                    playbackState = PlaybackState(
                        isPlaying = false,
                        activeMediaId = "video:7",
                        activeMediaKind = MediaKind.VIDEO,
                        title = "Demo Reel",
                        positionMs = 10_000L,
                        durationMs = 120_000L,
                    ),
                    onNavigateBack = {},
                    onTogglePlayPause = {},
                    onSeekTo = {},
                    onSeekBy = { soughtDelta = it },
                    onCycleSpeed = {},
                    onLeaveScreen = {},
                )
            }
        }

        composeRule.onNodeWithText("Demo Reel").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Forward 10 seconds").performClick()
        assertThat(soughtDelta).isEqualTo(10_000L)
    }
}
