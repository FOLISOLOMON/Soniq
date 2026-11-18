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
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.ThemeMode
import com.soniq.player.core.permissions.MediaAccessState
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
    fun switchesBetweenMusicVideoAndSettingsDestinations() {
        composeRule.setContent {
            SoniqTheme {
                SoniqApp(
                    musicContent = {
                        MusicScreen(
                            uiState = MusicUiState(accessState = MediaAccessState.NOT_GRANTED),
                            onRequestPermission = {},
                            onOpenAppSettings = {},
                        )
                    },
                    videoContent = {
                        VideoScreen(
                            uiState = VideoUiState(accessState = MediaAccessState.GRANTED),
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

        // Start destination is Music.
        composeRule.onNodeWithText("Access your music").assertIsDisplayed()

        // Navigate to Video.
        composeRule.onNodeWithText("Video").performClick()
        composeRule.onNodeWithText("Video access granted").assertIsDisplayed()

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
}
