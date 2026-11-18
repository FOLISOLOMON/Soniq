package com.soniq.player.feature.player

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.media3.common.Player
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.datastore.PlaybackPreferencesDataSource
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.MediaResumePosition
import com.soniq.player.core.model.media.PlaybackHistoryEntry
import com.soniq.player.core.model.media.PlaylistSummary
import com.soniq.player.core.model.media.VideoItem
import com.soniq.player.core.model.media.mediaKey
import com.soniq.player.core.permissions.NotificationPermissionChecker
import com.soniq.player.core.playback.PlaybackController
import com.soniq.player.core.playback.PlaybackState
import com.soniq.player.core.userlibrary.domain.UserLibraryRepository
import com.soniq.player.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
    fun cyclesSpeedRestoresVideoResumePositionAndRespectsBackgroundPreference() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempFolder.newFolder(), "vm_playback.preferences_pb") },
        )
        val prefs = PlaybackPreferencesDataSource(dataStore)
        val notifChecker = object : NotificationPermissionChecker {
            override fun canPostNotifications(): Boolean = false
        }
        val fakeController = FakePlaybackController()
        val fakeUserLibrary = FakeUserLibraryRepository(
            savedVideoResumeMs = 42_000L,
        )
        val viewModel = PlaybackViewModel(fakeController, prefs, notifChecker, fakeUserLibrary)

        assertThat(PlaybackViewModel.formatSpeed(1.0f)).isEqualTo("1")
        assertThat(PlaybackViewModel.formatSpeed(1.25f)).isEqualTo("1.25")
        assertThat(PlaybackViewModel.formatSpeed(1.5f)).isEqualTo("1.5")

        // Playing a video restores its saved resume position automatically.
        val sampleVideo = VideoItem(
            id = 7L,
            contentUri = "content://video/7",
            title = "Documentary",
            displayName = "Documentary.mp4",
            durationMs = 300_000L,
            sizeBytes = 100_000_000L,
            mimeType = "video/mp4",
            width = 1920,
            height = 1080,
            folderPath = "Movies",
            folderName = "Movies",
            dateAddedSeconds = 1L,
            dateModifiedSeconds = 1L,
        )
        viewModel.playVideo(sampleVideo)
        assertThat(fakeController.lastVideoStartPositionMs).isEqualTo(42_000L)

        // Notification prompt fires once.
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

    private class FakeUserLibraryRepository(
        private val savedVideoResumeMs: Long,
    ) : UserLibraryRepository {
        override val playlists: Flow<List<PlaylistSummary>> = flowOf(emptyList())
        override val favoriteKeys: Flow<Set<String>> = flowOf(emptySet())
        override val recentHistory: Flow<List<PlaybackHistoryEntry>> = flowOf(emptyList())
        override fun observePlaylistTracks(playlistId: Long): Flow<List<AudioTrack>> = flowOf(emptyList())
        override fun observeResumePositions(kind: MediaKind): Flow<Map<Long, MediaResumePosition>> =
            flowOf(emptyMap())

        override suspend fun createPlaylist(name: String): Long = 1L
        override suspend fun renamePlaylist(playlistId: Long, newName: String): Boolean = true
        override suspend fun deletePlaylist(playlistId: Long) = Unit
        override suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) = Unit
        override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = Unit
        override suspend fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<Long>) = Unit
        override suspend fun toggleFavorite(kind: MediaKind, mediaId: Long): Boolean = true
        override suspend fun recordPlayback(kind: MediaKind, mediaId: Long, title: String, subtitle: String?) = Unit
        override suspend fun clearHistory() = Unit
        override suspend fun getResumePosition(kind: MediaKind, mediaId: Long): MediaResumePosition? =
            if (kind == MediaKind.VIDEO) {
                MediaResumePosition(
                    mediaKey = mediaKey(kind, mediaId),
                    kind = kind,
                    mediaId = mediaId,
                    positionMs = savedVideoResumeMs,
                    durationMs = 300_000L,
                    updatedAtEpochMillis = 1L,
                )
            } else {
                null
            }

        override suspend fun saveResumePosition(kind: MediaKind, mediaId: Long, positionMs: Long, durationMs: Long) = Unit
        override suspend fun clearResumePosition(kind: MediaKind, mediaId: Long) = Unit
    }

    private class FakePlaybackController : PlaybackController {
        val mutableState = MutableStateFlow(PlaybackState())
        override val state: StateFlow<PlaybackState> = mutableState
        override val player: StateFlow<Player?> = MutableStateFlow(null)
        var pauseCount = 0
        var lastVideoStartPositionMs = -1L

        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun playAudioQueue(tracks: List<AudioTrack>, startIndex: Int) = Unit
        override fun playNext(track: AudioTrack) = Unit
        override fun addToQueue(track: AudioTrack) = Unit
        override fun skipToQueueIndex(index: Int) = Unit
        override fun removeQueueItem(index: Int) = Unit
        override fun moveQueueItem(fromIndex: Int, toIndex: Int) = Unit
        override fun playVideo(video: VideoItem, startPositionMs: Long) {
            lastVideoStartPositionMs = startPositionMs
        }

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
