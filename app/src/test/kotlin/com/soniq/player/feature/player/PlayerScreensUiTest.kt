package com.soniq.player.feature.player

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.PlayerLayout
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.playback.AudioEffectsState
import com.soniq.player.core.playback.MediaTrackOption
import com.soniq.player.core.playback.PlaybackState
import com.soniq.player.core.playback.QueueItem
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
        var pipClicked = false

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
                    onEnterPip = { pipClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Demo Reel").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Picture-in-Picture").performClick()
        assertThat(pipClicked).isTrue()
        composeRule.onNodeWithContentDescription("Forward 10 seconds").performClick()
        assertThat(soughtDelta).isEqualTo(10_000L)
    }

    @Test
    fun nowPlayingFavoriteAndQueueSheetTriggerCallbacks() {
        var favoriteToggled = false
        var selectedQueueIndex = -1
        val stateWithQueue = activeAudioState.copy(
            queue = listOf(
                QueueItem(0, "audio:1", "Kiasmos", "Blurred", null, isCurrent = true),
                QueueItem(1, "audio:2", "Looped", "Kiasmos", null, isCurrent = false),
            ),
        )

        composeRule.setContent {
            SoniqTheme {
                NowPlayingScreen(
                    playbackState = stateWithQueue,
                    isFavorite = false,
                    onNavigateBack = {},
                    onTogglePlayPause = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekTo = {},
                    onToggleShuffle = {},
                    onCycleRepeatMode = {},
                    onCycleSpeed = {},
                    onToggleFavorite = { favoriteToggled = true },
                    onSelectQueueItem = { selectedQueueIndex = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Add to favorites").performClick()
        assertThat(favoriteToggled).isTrue()

        composeRule.onNodeWithContentDescription("Up Next queue").performClick()
        composeRule.onNodeWithText("Up Next").assertIsDisplayed()
        composeRule.onNodeWithText("Looped").performClick()
        assertThat(selectedQueueIndex).isEqualTo(1)
    }

    @Test
    fun nowPlayingAudioEffectsSheetShowsUnsupportedReasonAndCrossfadeOptions() {
        var selectedCrossfade = -1

        composeRule.setContent {
            SoniqTheme {
                NowPlayingScreen(
                    playbackState = activeAudioState,
                    audioEffectsState = AudioEffectsState(
                        isEqualizerSupported = false,
                        eqUnsupportedReason = "EQ unsupported on test output.",
                        isBassBoostSupported = false,
                        bassBoostUnsupportedReason = "Bass Boost unsupported on test output.",
                        crossfadeDurationSec = 0,
                    ),
                    onNavigateBack = {},
                    onTogglePlayPause = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekTo = {},
                    onToggleShuffle = {},
                    onCycleRepeatMode = {},
                    onCycleSpeed = {},
                    onSetCrossfadeDurationSec = { selectedCrossfade = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Audio Effects").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("EQ unsupported on test output.").assertIsDisplayed()
        composeRule.onNodeWithText("Bass Boost unsupported on test output.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Crossfade duration")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("4s")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertThat(selectedCrossfade).isEqualTo(4)
    }

    @Test
    fun videoPlayerDisplaysSubtitlesAndSelectsAudioSubtitleTracks() {
        var selectedSubtitle: Pair<Int?, Int?>? = null
        var selectedAudio: Pair<Int, Int>? = null

        composeRule.setContent {
            SoniqTheme {
                VideoPlayerScreen(
                    player = null,
                    playbackState = PlaybackState(
                        isPlaying = false,
                        activeMediaId = "video:9",
                        activeMediaKind = MediaKind.VIDEO,
                        title = "Nature Doc",
                        positionMs = 5_000L,
                        durationMs = 90_000L,
                        activeSubtitleCues = listOf("Hello world"),
                        availableSubtitleTracks = listOf(
                            MediaTrackOption(0, 0, "English (SRT)", "en", isSelected = false),
                        ),
                        availableAudioTracks = listOf(
                            MediaTrackOption(0, 0, "Japanese 5.1", "ja", isSelected = true),
                            MediaTrackOption(1, 0, "English Stereo", "en", isSelected = false),
                        ),
                    ),
                    onNavigateBack = {},
                    onTogglePlayPause = {},
                    onSeekTo = {},
                    onSeekBy = {},
                    onCycleSpeed = {},
                    onLeaveScreen = {},
                    onSelectAudioTrack = { g, t -> selectedAudio = g to t },
                    onSelectSubtitleTrack = { g, t -> selectedSubtitle = g to t },
                )
            }
        }

        composeRule.onNodeWithText("Hello world").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Audio & Subtitles").performClick()
        composeRule.onNodeWithText("English (SRT)").performClick()
        assertThat(selectedSubtitle).isEqualTo(0 to 0)

        composeRule.onNodeWithContentDescription("Audio & Subtitles").performClick()
        composeRule.onNodeWithText("English Stereo").performClick()
        assertThat(selectedAudio).isEqualTo(1 to 0)
    }
}
