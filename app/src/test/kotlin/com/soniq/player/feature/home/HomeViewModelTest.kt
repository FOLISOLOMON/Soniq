package com.soniq.player.feature.home

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
import com.soniq.player.core.model.media.MediaResumePosition
import com.soniq.player.core.model.media.PlaybackHistoryEntry
import com.soniq.player.core.model.media.PlaylistSummary
import com.soniq.player.core.model.media.VideoItem
import com.soniq.player.core.model.media.mediaKey
import com.soniq.player.core.userlibrary.domain.UserLibraryRepository
import com.soniq.player.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun homeStateResolvesFeaturedRecentAndSearchMatchesOffline() = runTest {
        val viewModel = HomeViewModel(
            musicRepository = StubMusicLibraryRepository(),
            videoRepository = StubVideoLibraryRepository(),
            userLibraryRepository = StubUserLibraryRepository(),
        )

        viewModel.uiState.test {
            val initial = awaitItem()
            assertThat(initial.totalTracks).isEqualTo(2)
            assertThat(initial.totalAlbums).isEqualTo(1)
            assertThat(initial.totalVideos).isEqualTo(1)
            assertThat(initial.totalPlaylists).isEqualTo(1)
            assertThat(initial.recentlyPlayed.map { it.title }).containsExactly("Zephyr")
            assertThat(initial.featuredTrack?.title).isEqualTo("Zephyr")
            assertThat(initial.searchResults).isEmpty()

            viewModel.onSearchQueryChanged("aur")
            val filtered = awaitItem()
            assertThat(filtered.searchQuery).isEqualTo("aur")
            assertThat(filtered.searchResults.map { it.title }).containsExactly("Aurora")

            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class StubMusicLibraryRepository : MusicLibraryRepository {
    private val sampleTracks = listOf(
        AudioTrack(1L, "content://audio/1", "Aurora", "Kiasmos", "Blurred", 1L, null, 200_000L, 5_000L, "audio/flac", "Music", "Music", 1, 2020, 10L, 10L),
        AudioTrack(2L, "content://audio/2", "Zephyr", "Jon Hopkins", "Singularity", 2L, null, 300_000L, 7_000L, "audio/flac", "Music", "Music", 2, 2021, 20L, 20L),
    )

    override val syncState: StateFlow<LibrarySyncState> = MutableStateFlow(LibrarySyncState.Idle)
    override val tracks: Flow<List<AudioTrack>> = flowOf(sampleTracks)
    override val albums: Flow<List<AlbumSummary>> = flowOf(
        listOf(
            AlbumSummary(
                albumId = 1L,
                title = "Blurred",
                artist = "Kiasmos",
                albumArtUri = null,
                trackCount = 1,
                totalDurationMs = 200_000L,
            ),
        ),
    )
    override val artists: Flow<List<ArtistSummary>> = flowOf(emptyList())
    override val folders: Flow<List<MediaFolderSummary>> = flowOf(emptyList())

    override suspend fun sync(): Result<Int> = Result.success(2)
    override fun observeExternalChanges(): Flow<Unit> = emptyFlow()
}

private class StubVideoLibraryRepository : VideoLibraryRepository {
    override val syncState: StateFlow<LibrarySyncState> = MutableStateFlow(LibrarySyncState.Idle)
    override val videos: Flow<List<VideoItem>> = flowOf(
        listOf(VideoItem(10L, "content://video/10", "Clip", "Clip.mp4", 60_000L, 10_000L, "video/mp4", 1920, 1080, "Movies", "Movies", 1L, 1L)),
    )
    override val folders: Flow<List<MediaFolderSummary>> = flowOf(emptyList())

    override suspend fun sync(): Result<Int> = Result.success(1)
    override fun observeExternalChanges(): Flow<Unit> = emptyFlow()
}

private class StubUserLibraryRepository : UserLibraryRepository {
    override val playlists: Flow<List<PlaylistSummary>> = flowOf(
        listOf(PlaylistSummary(id = 1L, name = "Chill Mix", trackCount = 1, createdAtEpochMillis = 100L, updatedAtEpochMillis = 100L)),
    )
    override val favoriteKeys: Flow<Set<String>> = flowOf(emptySet())
    override val recentHistory: Flow<List<PlaybackHistoryEntry>> = flowOf(
        listOf(
            PlaybackHistoryEntry(
                id = 1L,
                mediaKey = mediaKey(MediaKind.AUDIO, 2L),
                kind = MediaKind.AUDIO,
                mediaId = 2L,
                title = "Zephyr",
                subtitle = "Jon Hopkins",
                playedAtEpochMillis = 500L,
            ),
        ),
    )

    override fun observePlaylistTracks(playlistId: Long): Flow<List<AudioTrack>> = emptyFlow()
    override fun observeResumePositions(kind: MediaKind): Flow<Map<Long, MediaResumePosition>> = flowOf(emptyMap())
    override suspend fun createPlaylist(name: String): Long = 1L
    override suspend fun renamePlaylist(playlistId: Long, newName: String): Boolean = true
    override suspend fun deletePlaylist(playlistId: Long) = Unit
    override suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) = Unit
    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = Unit
    override suspend fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<Long>) = Unit
    override suspend fun toggleFavorite(kind: MediaKind, mediaId: Long): Boolean = true
    override suspend fun recordPlayback(kind: MediaKind, mediaId: Long, title: String, subtitle: String?) = Unit
    override suspend fun clearHistory() = Unit
    override suspend fun getResumePosition(kind: MediaKind, mediaId: Long): MediaResumePosition? = null
    override suspend fun saveResumePosition(kind: MediaKind, mediaId: Long, positionMs: Long, durationMs: Long) = Unit
    override suspend fun clearResumePosition(kind: MediaKind, mediaId: Long) = Unit
}
