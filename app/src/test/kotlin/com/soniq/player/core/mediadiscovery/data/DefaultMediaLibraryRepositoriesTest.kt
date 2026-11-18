package com.soniq.player.core.mediadiscovery.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.database.media.SoniqMediaDatabase
import com.soniq.player.core.mediadiscovery.LocalAudioScanner
import com.soniq.player.core.mediadiscovery.LocalVideoScanner
import com.soniq.player.core.mediadiscovery.MediaStoreChangeObserver
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.LibrarySyncState
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.VideoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DefaultMediaLibraryRepositoriesTest {

    private lateinit var database: SoniqMediaDatabase

    private val noOpObserver = object : MediaStoreChangeObserver {
        override fun observeChanges(kind: MediaKind): Flow<Unit> = emptyFlow()
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SoniqMediaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun musicRepositorySyncsTracksDerivesAggregatesAndSurfacesErrors() = runTest {
        var shouldFail = false
        val scanner = object : LocalAudioScanner {
            override suspend fun scan(): List<AudioTrack> {
                if (shouldFail) throw IOException("Storage unavailable")
                return listOf(
                    sampleTrack(id = 1L, title = "One", artist = "Tycho", album = "Dive", albumId = 10L, folder = "Music/Ambient"),
                    sampleTrack(id = 2L, title = "Two", artist = "Tycho", album = "Dive", albumId = 10L, folder = "Music/Ambient"),
                    sampleTrack(id = 3L, title = "Three", artist = "Tycho", album = "Awake", albumId = 20L, folder = "Music/Chill"),
                )
            }
        }
        val repo = DefaultMusicLibraryRepository(scanner, database.audioTrackDao(), noOpObserver)

        val result = repo.sync()
        assertThat(result.getOrNull()).isEqualTo(3)
        assertThat(repo.syncState.value).isEqualTo(LibrarySyncState.Idle)
        assertThat(repo.albums.first().map { it.title to it.trackCount })
            .containsExactly("Awake" to 1, "Dive" to 2)
            .inOrder()
        assertThat(repo.artists.first().single().albumCount).isEqualTo(2)
        assertThat(repo.folders.first().map { it.folderName to it.itemCount })
            .containsExactly("Ambient" to 2, "Chill" to 1)
            .inOrder()

        shouldFail = true
        val failed = repo.sync()
        assertThat(failed.isFailure).isTrue()
        assertThat(repo.syncState.value).isInstanceOf(LibrarySyncState.Error::class.java)
    }

    @Test
    fun videoRepositorySyncsVideosAndFolderSummaries() = runTest {
        val scanner = object : LocalVideoScanner {
            override suspend fun scan(): List<VideoItem> = listOf(
                VideoItem(
                    id = 5L,
                    contentUri = "content://media/external/video/media/5",
                    title = "Concert",
                    displayName = "Concert.mp4",
                    durationMs = 90_000L,
                    sizeBytes = 100_000_000L,
                    mimeType = "video/mp4",
                    width = 1920,
                    height = 1080,
                    folderPath = "Movies/Live",
                    folderName = "Live",
                    dateAddedSeconds = 500L,
                    dateModifiedSeconds = 500L,
                ),
            )
        }
        val repo = DefaultVideoLibraryRepository(scanner, database.videoItemDao(), noOpObserver)

        assertThat(repo.sync().getOrNull()).isEqualTo(1)
        assertThat(repo.videos.first()).hasSize(1)
        assertThat(repo.folders.first().single().folderName).isEqualTo("Live")
    }

    private fun sampleTrack(
        id: Long,
        title: String,
        artist: String,
        album: String,
        albumId: Long,
        folder: String,
    ) = AudioTrack(
        id = id,
        contentUri = "content://media/external/audio/media/$id",
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        albumArtUri = "content://media/external/audio/albumart/$albumId",
        durationMs = 200_000L,
        sizeBytes = 4_000_000L,
        mimeType = "audio/flac",
        folderPath = folder,
        folderName = folder.substringAfterLast('/'),
        trackNumber = id.toInt(),
        year = 2026,
        dateAddedSeconds = id * 100L,
        dateModifiedSeconds = id * 100L,
    )
}
