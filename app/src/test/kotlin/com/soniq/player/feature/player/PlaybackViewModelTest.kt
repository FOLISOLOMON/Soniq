package com.soniq.player.feature.player

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.media3.common.Player
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.datastore.PlaybackPreferencesDataSource
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.VideoItem
import com.soniq.player.core.permissions.NotificationPermissionChecker
import com.soniq.player.core.playback.PlaybackController
import com.soniq.player.core.playback.PlaybackState
import com.soniq.player.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlaybackViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun cyclesSpeedAndRespectsContinueVideoInBackgroundPreference() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempFolder.newFolder(), "vm_playback.preferences_pb") },
        )
        val prefs = PlaybackPreferencesDataSource(dataStore)
        val notifChecker = object : NotificationPermissionChecker {
            override fun canPostNotifications(): Boolean = false
        }
        val fakeController = FakePlaybackController()
        val viewModel = PlaybackViewModel(fakeController, prefs, notifChecker)

        assertThat(PlaybackViewModel.formatSpeed(1.0f)).isEqualTo("1")
        assertThat(PlaybackViewModel.formatSpeed(1.25f)).isEqualTo("1.25")
        assertThat(PlaybackViewModel.formatSpeed(1.5f)).isEqualTo("1.5")

        // On SDK 35 with notification permission denied and prompt not yet shown -> should prompt.
        assertThat(viewModel.shouldPromptForNotifications()).isTrue()
        viewModel.markNotificationPromptShown()
        assertThat(viewModel.shouldPromptForNotifications()).isFalse()

        // Cycles 1.0 -> 1.25 -> 1.5 -> 2.0 -> 0.75 -> 1.0.
        viewModel.cyclePlaybackSpeed()
        assertThat(fakeController.state.value.playbackSpeed).isEqualTo(1.25f)
        viewModel.cyclePlaybackSpeed()
        assertThat(fakeController.state.value.playbackSpeed).isEqualTo(1.5f)
        viewModel.cyclePlaybackSpeed()
        assertThat(fakeController.state.value.playbackSpeed).isEqualTo(2.0f)
        viewModel.cyclePlaybackSpeed()
        assertThat(fakeController.state.value.playbackSpeed).isEqualTo(0.75f)
        viewModel.cyclePlaybackSpeed()
        assertThat(fakeController.state.value.playbackSpeed).isEqualTo(1.0f)

        // Leaving video screen never pauses audio.
        fakeController.mutableState.value = PlaybackState(
            activeMediaId = "audio:1",
            activeMediaKind = MediaKind.AUDIO,
            isPlaying = true,
        )
        viewModel.pauseIfVideo()
        assertThat(fakeController.pauseCount).isEqualTo(0)

        // Default continueVideoInBackground=false pauses active video on exit.
        fakeController.mutableState.value = PlaybackState(
            activeMediaId = "video:2",
            activeMediaKind = MediaKind.VIDEO,
            isPlaying = true,
        )
        viewModel.pauseIfVideo()
        assertThat(fakeController.pauseCount).isEqualTo(1)

        // When continueVideoInBackground=true, leaving video screen keeps audio playing.
        prefs.setContinueVideoInBackground(true)
        viewModel.preferences.first { it.continueVideoInBackground }
        viewModel.pauseIfVideo()
        assertThat(fakeController.pauseCount).isEqualTo(1)
    }

    private class FakePlaybackController : PlaybackController {
        val mutableState = MutableStateFlow(PlaybackState())
        override val state: StateFlow<PlaybackState> = mutableState
        override val player: StateFlow<Player?> = MutableStateFlow(null)
        var pauseCount = 0

        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun playAudioQueue(tracks: List<AudioTrack>, startIndex: Int) = Unit
        override fun playVideo(video: VideoItem) = Unit
        override fun togglePlayPause() = Unit
        override fun play() = Unit
        override fun pause() {
            pauseCount++
        }

        override fun stop() = Unit
        override fun skipToNext() = Unit
        override fun skipToPrevious() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun seekBy(deltaMs: Long) = Unit
        override fun cycleRepeatMode() = Unit
        override fun toggleShuffle() = Unit
        override fun setPlaybackSpeed(speed: Float) {
            mutableState.value = mutableState.value.copy(playbackSpeed = speed)
        }
    }
}
