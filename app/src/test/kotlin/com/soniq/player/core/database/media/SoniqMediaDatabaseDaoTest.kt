package com.soniq.player.core.database.media

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.database.media.entity.AudioTrackEntity
import com.soniq.player.core.database.media.entity.VideoItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SoniqMediaDatabaseDaoTest {

    private lateinit var database: SoniqMediaDatabase

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
    fun audioTrackDaoSyncUpsertsUpdatesAndPrunesStaleRows() = runTest {
        val dao = database.audioTrackDao()
        val first = sampleAudio(id = 1L, title = "Beta", albumId = 10L)
        val second = sampleAudio(id = 2L, title = "Alpha", albumId = 10L)

        dao.syncWith(listOf(first, second))
        assertThat(dao.observeAll().first().map { it.title }).containsExactly("Alpha", "Beta").inOrder()
        assertThat(dao.observeByAlbum(10L).first()).hasSize(2)

        // Sync with id=2 removed and id=1 renamed + new id=3 added.
        val updatedFirst = first.copy(title = "Beta Remastered")
        val third = sampleAudio(id = 3L, title = "Gamma", albumId = 20L)
        dao.syncWith(listOf(updatedFirst, third))

        val synced = dao.observeAll().first()
        assertThat(synced.map { it.id to it.title })
            .containsExactly(1L to "Beta Remastered", 3L to "Gamma")
            .inOrder()

        dao.syncWith(emptyList())
        assertThat(dao.observeAll().first()).isEmpty()
    }

    @Test
    fun videoItemDaoSyncsOrdersByDateAddedAndFiltersByFolder() = runTest {
        val dao = database.videoItemDao()
        val older = sampleVideo(id = 1L, title = "Clip A", folderPath = "Movies/Trips", dateAdded = 100L)
        val newer = sampleVideo(id = 2L, title = "Clip B", folderPath = "DCIM/Camera", dateAdded = 200L)

        dao.syncWith(listOf(older, newer))
        assertThat(dao.observeAll().first().map { it.id }).containsExactly(2L, 1L).inOrder()
        assertThat(dao.observeByFolder("Movies/Trips").first().map { it.id }).containsExactly(1L)

        dao.syncWith(listOf(older))
        assertThat(dao.observeAll().first().map { it.id }).containsExactly(1L)
    }

    private fun sampleAudio(id: Long, title: String, albumId: Long) = AudioTrackEntity(
        id = id,
        contentUri = "content://media/external/audio/media/$id",
        title = title,
        artist = "Artist",
        album = "Album $albumId",
        albumId = albumId,
        albumArtUri = null,
        durationMs = 180_000L,
        sizeBytes = 5_000_000L,
        mimeType = "audio/mpeg",
        folderPath = "Music",
        folderName = "Music",
        trackNumber = 1,
        year = 2026,
        dateAddedSeconds = id * 10L,
        dateModifiedSeconds = id * 10L,
    )

    private fun sampleVideo(id: Long, title: String, folderPath: String, dateAdded: Long) = VideoItemEntity(
        id = id,
        contentUri = "content://media/external/video/media/$id",
        title = title,
        displayName = "$title.mp4",
        durationMs = 60_000L,
        sizeBytes = 25_000_000L,
        mimeType = "video/mp4",
        width = 1920,
        height = 1080,
        folderPath = folderPath,
        folderName = folderPath.substringAfterLast('/'),
        dateAddedSeconds = dateAdded,
        dateModifiedSeconds = dateAdded,
    )
}
