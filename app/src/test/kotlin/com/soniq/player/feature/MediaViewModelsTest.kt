package com.soniq.player.feature

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.mediadiscovery.domain.MusicLibraryRepository
import com.soniq.player.core.mediadiscovery.domain.VideoLibraryRepository
import com.soniq.player.core.model.media.AlbumSummary
import com.soniq.player.core.model.media.ArtistSummary
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.LibrarySyncState
import com.soniq.player.core.model.media.MediaFolderSummary
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.MusicSortOrder
import com.soniq.player.core.model.media.VideoItem
import com.soniq.player.core.model.media.VideoSortOrder
import com.soniq.player.core.permissions.MediaAccessGrant
import com.soniq.player.core.permissions.MediaAccessState
import com.soniq.player.core.permissions.PermissionChecker
import com.soniq.player.feature.music.MusicViewModel
import com.soniq.player.feature.video.VideoViewModel
import com.soniq.player.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MediaViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun musicAndVideoTrackPermissionsSyncSortAndFilterIndependently() = runTest {
        var audioGrant = MediaAccessGrant.FULL
        var videoGrant = MediaAccessGrant.NONE
        val checker = object : PermissionChecker {
            override fun checkMediaAccess(kind: MediaKind): MediaAccessGrant = when (kind) {
                MediaKind.AUDIO -> audioGrant
                MediaKind.VIDEO -> videoGrant
            }
        }
        val musicRepo = FakeMusicLibraryRepository()
        val videoRepo = FakeVideoLibraryRepository()

        val musicViewModel = MusicViewModel(checker, musicRepo)
        val videoViewModel = VideoViewModel(checker, videoRepo)

        // Audio started granted, so it synced immediately; Video started NONE, so 0 syncs yet.
        assertThat(musicRepo.syncCount).isEqualTo(1)
        assertThat(videoRepo.syncCount).isEqualTo(0)

        musicViewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.accessState).isEqualTo(MediaAccessState.GRANTED)
            assertThat(state.tracks.map { it.title }).containsExactly("Aurora", "Zephyr").inOrder()

            musicViewModel.selectSortOrder(MusicSortOrder.DURATION_DESC)
            assertThat(awaitItem().tracks.map { it.title }).containsExactly("Zephyr", "Aurora").inOrder()

            musicViewModel.setSearchActive(true)
            awaitItem()
            musicViewModel.onSearchQueryChanged("aur")
            assertThat(awaitItem().tracks.map { it.title }).containsExactly("Aurora")

            musicViewModel.setSearchActive(false)
            awaitItem()
            musicViewModel.selectFolder(MediaFolderSummary("Music/Synth", "Synth", 1, 300_000L))
            assertThat(awaitItem().tracks.map { it.title }).containsExactly("Zephyr")

            cancelAndIgnoreRemainingEvents()
        }

        videoViewModel.uiState.test {
            assertThat(awaitItem().accessState).isEqualTo(MediaAccessState.NOT_GRANTED)

            // Android 14+ partial video access transitions to PARTIAL and triggers a library sync.
            videoGrant = MediaAccessGrant.PARTIAL
            videoViewModel.onPermissionResult(grant = MediaAccessGrant.PARTIAL, permanentlyDenied = false)
            val partial = expectMostRecentItem()
            assertThat(partial.accessState).isEqualTo(MediaAccessState.PARTIAL)
            assertThat(videoRepo.syncCount).isEqualTo(1)
            assertThat(partial.videos).hasSize(2)

            videoViewModel.selectSortOrder(VideoSortOrder.SIZE_DESC)
            assertThat(awaitItem().videos.map { it.title }).containsExactly("Large Clip", "Small Clip").inOrder()

            // Permanent denial is preserved across resume until granted in system settings.
            videoGrant = MediaAccessGrant.NONE
            videoViewModel.onPermissionResult(grant = MediaAccessGrant.NONE, permanentlyDenied = true)
            assertThat(awaitItem().accessState).isEqualTo(MediaAccessState.DENIED_PERMANENTLY)
            videoViewModel.refreshPermission()
            assertThat(videoViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.DENIED_PERMANENTLY)

            // Granting full access transitions to GRANTED.
            videoGrant = MediaAccessGrant.FULL
            videoViewModel.refreshPermission()
            assertThat(expectMostRecentItem().accessState).isEqualTo(MediaAccessState.GRANTED)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeMusicLibraryRepository : MusicLibraryRepository {
        var syncCount = 0
        private val sample = listOf(
            AudioTrack(
                id = 1L,
                contentUri = "content://audio/1",
                title = "Aurora",
                artist = "A",
                album = "Dawn",
                albumId = 1L,
                albumArtUri = null,
                durationMs = 120_000L,
                sizeBytes = 3_000_000L,
                mimeType = "audio/flac",
                folderPath = "Music/Chill",
                folderName = "Chill",
                trackNumber = 1,
                year = 2026,
                dateAddedSeconds = 100L,
                dateModifiedSeconds = 100L,
            ),
            AudioTrack(
                id = 2L,
                contentUri = "content://audio/2",
                title = "Zephyr",
                artist = "B",
                album = "Night",
                albumId = 2L,
                albumArtUri = null,
                durationMs = 300_000L,
                sizeBytes = 6_000_000L,
                mimeType = "audio/flac",
                folderPath = "Music/Synth",
                folderName = "Synth",
                trackNumber = 1,
                year = 2026,
                dateAddedSeconds = 200L,
                dateModifiedSeconds = 200L,
            ),
        )
        override val tracks: Flow<List<AudioTrack>> = MutableStateFlow(sample)
        override val albums: Flow<List<AlbumSummary>> = MutableStateFlow(emptyList())
        override val artists: Flow<List<ArtistSummary>> = MutableStateFlow(emptyList())
        override val folders: Flow<List<MediaFolderSummary>> = MutableStateFlow(emptyList())
        override val syncState: StateFlow<LibrarySyncState> = MutableStateFlow(LibrarySyncState.Idle)

        override suspend fun sync(): Result<Int> {
            syncCount++
            return Result.success(sample.size)
        }

        override fun observeExternalChanges(): Flow<Unit> = emptyFlow()
    }

    private class FakeVideoLibraryRepository : VideoLibraryRepository {
        var syncCount = 0
        private val sample = listOf(
            VideoItem(
                id = 1L,
                contentUri = "content://video/1",
                title = "Small Clip",
                displayName = "small.mp4",
                durationMs = 30_000L,
                sizeBytes = 10_000_000L,
                mimeType = "video/mp4",
                width = 1280,
                height = 720,
                folderPath = "DCIM/Camera",
                folderName = "Camera",
                dateAddedSeconds = 200L,
                dateModifiedSeconds = 200L,
            ),
            VideoItem(
                id = 2L,
                contentUri = "content://video/2",
                title = "Large Clip",
                displayName = "large.mp4",
                durationMs = 120_000L,
                sizeBytes = 500_000_000L,
                mimeType = "video/mp4",
                width = 3840,
                height = 2160,
                folderPath = "Movies",
                folderName = "Movies",
                dateAddedSeconds = 100L,
                dateModifiedSeconds = 100L,
            ),
        )
        override val videos: Flow<List<VideoItem>> = MutableStateFlow(sample)
        override val folders: Flow<List<MediaFolderSummary>> = MutableStateFlow(emptyList())
        override val syncState: StateFlow<LibrarySyncState> = MutableStateFlow(LibrarySyncState.Idle)

        override suspend fun sync(): Result<Int> {
            syncCount++
            return Result.success(sample.size)
        }

        override fun observeExternalChanges(): Flow<Unit> = emptyFlow()
    }
}
