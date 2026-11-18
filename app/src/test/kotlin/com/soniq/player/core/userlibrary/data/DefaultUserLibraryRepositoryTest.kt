package com.soniq.player.core.userlibrary.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.database.SoniqDatabase
import com.soniq.player.core.database.media.SoniqMediaDatabase
import com.soniq.player.core.database.media.entity.AudioTrackEntity
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.mediaKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DefaultUserLibraryRepositoryTest {

    private lateinit var userDb: SoniqDatabase
    private lateinit var mediaDb: SoniqMediaDatabase
    private lateinit var repository: DefaultUserLibraryRepository
    private val fixedClock = Clock.fixed(Instant.ofEpochMilli(1_700_000_000_000L), ZoneOffset.UTC)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        userDb = Room.inMemoryDatabaseBuilder(context, SoniqDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaDb = Room.inMemoryDatabaseBuilder(context, SoniqMediaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DefaultUserLibraryRepository(
            playlistDao = userDb.playlistDao(),
            userMediaDao = userDb.userMediaDao(),
            audioTrackDao = mediaDb.audioTrackDao(),
            clock = fixedClock,
        )
    }

    @After
    fun tearDown() {
        userDb.close()
        mediaDb.close()
    }

    @Test
    fun supportsFullPlaylistCrudOrderingAndTrackResolution() = runTest {
        mediaDb.audioTrackDao().syncWith(
            listOf(sampleAudio(1L, "First"), sampleAudio(2L, "Second"), sampleAudio(3L, "Third")),
        )

        val id = repository.createPlaylist("  Focus Flow  ")
        assertThat(repository.playlists.first().single().name).isEqualTo("Focus Flow")

        repository.addTrackToPlaylist(id, 3L)
        repository.addTrackToPlaylist(id, 1L)
        assertThat(repository.observePlaylistTracks(id).first().map { it.id })
            .containsExactly(3L, 1L)
            .inOrder()
        assertThat(repository.playlists.first().single().trackCount).isEqualTo(2)

        repository.reorderPlaylistTracks(id, listOf(1L, 3L, 2L))
        assertThat(repository.observePlaylistTracks(id).first().map { it.id })
            .containsExactly(1L, 3L, 2L)
            .inOrder()

        repository.removeTrackFromPlaylist(id, 3L)
        assertThat(repository.observePlaylistTracks(id).first().map { it.id })
            .containsExactly(1L, 2L)
            .inOrder()

        assertThat(repository.renamePlaylist(id, "Deep Focus")).isTrue()
        assertThat(repository.playlists.first().single().name).isEqualTo("Deep Focus")

        repository.deletePlaylist(id)
        assertThat(repository.playlists.first()).isEmpty()
        assertThat(repository.observePlaylistTracks(id).first()).isEmpty()

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repository.createPlaylist("   ") }
        }
    }

    @Test
    fun managesFavoritesHistoryAndResumeThresholdRules() = runTest {
        assertThat(repository.toggleFavorite(MediaKind.AUDIO, 42L)).isTrue()
        assertThat(repository.favoriteKeys.first()).containsExactly(mediaKey(MediaKind.AUDIO, 42L))
        assertThat(repository.toggleFavorite(MediaKind.AUDIO, 42L)).isFalse()
        assertThat(repository.favoriteKeys.first()).isEmpty()

        repository.recordPlayback(MediaKind.AUDIO, 42L, "Solaris", "Rival Consoles")
        assertThat(repository.recentHistory.first().single().title).isEqualTo("Solaris")
        repository.clearHistory()
        assertThat(repository.recentHistory.first()).isEmpty()

        // Under 5s does not persist resume position.
        repository.saveResumePosition(MediaKind.VIDEO, 9L, positionMs = 3_000L, durationMs = 100_000L)
        assertThat(repository.getResumePosition(MediaKind.VIDEO, 9L)).isNull()

        // Mid-playback saves cleanly.
        repository.saveResumePosition(MediaKind.VIDEO, 9L, positionMs = 45_000L, durationMs = 100_000L)
        val saved = repository.getResumePosition(MediaKind.VIDEO, 9L)
        assertThat(saved?.positionMs).isEqualTo(45_000L)
        assertThat(saved?.progressFraction).isWithin(0.01f).of(0.45f)

        // >= 95% completion clears saved resume position automatically.
        repository.saveResumePosition(MediaKind.VIDEO, 9L, positionMs = 96_000L, durationMs = 100_000L)
        assertThat(repository.getResumePosition(MediaKind.VIDEO, 9L)).isNull()
    }

    private fun sampleAudio(id: Long, title: String) = AudioTrackEntity(
        id = id,
        contentUri = "content://media/external/audio/media/$id",
        title = title,
        artist = "Artist",
        album = "Album",
        albumId = 1L,
        albumArtUri = null,
        durationMs = 180_000L,
        sizeBytes = 4_000_000L,
        mimeType = "audio/flac",
        folderPath = "Music",
        folderName = "Music",
        trackNumber = id.toInt(),
        year = 2026,
        dateAddedSeconds = id,
        dateModifiedSeconds = id,
    )
}
