package com.soniq.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.ThemeMode
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.VideoItem
import com.soniq.player.core.permissions.MediaAccessState
import com.soniq.player.feature.home.HomeScreen
import com.soniq.player.feature.home.HomeUiState
import com.soniq.player.feature.music.MusicScreen
import com.soniq.player.feature.music.MusicUiState
import com.soniq.player.feature.settings.SettingsScreen
import com.soniq.player.feature.settings.SettingsUiState
import com.soniq.player.feature.video.VideoScreen
import com.soniq.player.feature.video.VideoUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SoniqAppNavigationUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun switchesBetweenMusicVideoAndSettingsDestinationsAndRendersLibraryItems() {
        val sampleTrack = AudioTrack(
            id = 1L,
            contentUri = "content://audio/1",
            title = "Solaris",
            artist = "Rival Consoles",
            album = "Persona",
            albumId = 0L,
            albumArtUri = null,
            durationMs = 245_000L,
            sizeBytes = 8_000_000L,
            mimeType = "audio/flac",
            folderPath = "Music",
            folderName = "Music",
            trackNumber = 1,
            year = 2026,
            dateAddedSeconds = 10L,
            dateModifiedSeconds = 10L,
        )
        val sampleVideo = VideoItem(
            id = 2L,
            contentUri = "content://video/2",
            title = "Mountain Timelapse",
            displayName = "Mountain Timelapse.mp4",
            durationMs = 95_000L,
            sizeBytes = 64_000_000L,
            mimeType = "video/mp4",
            width = 1920,
            height = 1080,
            folderPath = "Movies",
            folderName = "Movies",
            dateAddedSeconds = 20L,
            dateModifiedSeconds = 20L,
        )

        composeRule.setContent {
            SoniqTheme {
                SoniqApp(
                    homeContent = {
                        HomeScreen(
                            uiState = HomeUiState(
                                featuredTrack = sampleTrack,
                                recentlyPlayed = listOf(sampleTrack),
                                totalTracks = 1,
                            ),
                            onSearchQueryChanged = {},
                            onPlayTracks = { _, _ -> },
                            onOpenMusicTab = {},
                            onOpenVideo = {},
                            onOpenSettings = {},
                        )
                    },
                    musicContent = {
                        MusicScreen(
                            uiState = MusicUiState(
                                accessState = MediaAccessState.GRANTED,
                                tracks = listOf(sampleTrack),
                            ),
                            onRequestPermission = {},
                            onOpenAppSettings = {},
                        )
                    },
                    videoContent = {
                        VideoScreen(
                            uiState = VideoUiState(
                                accessState = MediaAccessState.PARTIAL,
                                videos = listOf(sampleVideo),
                            ),
                            onRequestPermission = {},
                            onOpenAppSettings = {},
                        )
                    },
                    settingsContent = {
                        SettingsScreen(
                            uiState = SettingsUiState(),
                            dynamicColorSupported = true,
                            onThemeModeSelected = {},
                            onAmoledBlackToggled = {},
                            onDynamicColorToggled = {},
                            onAccentColorSelected = {},
                            onFontFamilySelected = {},
                            onTextScaleSelected = {},
                            onBackgroundStyleSelected = {},
                            onChooseBackgroundImage = {},
                            onPlayerLayoutSelected = {},
                            onPresetNameChanged = {},
                            onSavePreset = {},
                            onApplyPreset = {},
                            onDeletePreset = {},
                            onFeedbackDismissed = {},
                        )
                    },
                )
            }
        }

        // Start destination is Home with featured/recent track visible.
        composeRule.onNodeWithText("Solaris").assertIsDisplayed()

        // Navigate to Music and verify indexed track is displayed.
        composeRule.onNodeWithText("Music").performClick()
        composeRule.onNodeWithText("Solaris").assertIsDisplayed()

        // Navigate to Video: partial-access banner and indexed video are both displayed.
        composeRule.onNodeWithText("Video").performClick()
        composeRule.onNodeWithText("Showing only selected videos").assertIsDisplayed()
        composeRule.onNodeWithText("Mountain Timelapse").assertIsDisplayed()

        // Navigate to Settings.
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Appearance").assertIsDisplayed()
    }

    @Test
    fun amoledDarkThemeProvidesTrueBlackBackground() {
        var observedBackground by mutableStateOf(Color.Unspecified)

        composeRule.setContent {
            SoniqTheme(
                appearance = AppearanceConfig(
                    themeMode = ThemeMode.DARK,
                    useAmoledBlack = true,
                ),
            ) {
                observedBackground = MaterialTheme.colorScheme.background
            }
        }

        composeRule.waitForIdle()
        assertThat(observedBackground).isEqualTo(Color.Black)
    }

    @Test
    fun homeQuickAccessBubbleDeepLinksToRequestedMusicTab() {
        var openedTab: com.soniq.player.feature.music.MusicLibraryTab? = null

        composeRule.setContent {
            SoniqTheme {
                HomeScreen(
                    uiState = HomeUiState(),
                    onSearchQueryChanged = {},
                    onPlayTracks = { _, _ -> },
                    onOpenMusicTab = { tab -> openedTab = tab },
                    onOpenVideo = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Playlists").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertThat(openedTab).isEqualTo(com.soniq.player.feature.music.MusicLibraryTab.PLAYLISTS)
    }
}
