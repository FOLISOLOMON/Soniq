package com.soniq.player.core.playback

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.database.media.SoniqMediaDatabase
import com.soniq.player.core.database.media.entity.AudioTrackEntity
import com.soniq.player.core.datastore.PlaybackPreferencesDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlaybackPreferencesAndRestorationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

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
    fun persistsAndRestoresQueueOrderPruningDeletedTracksAndClampingPosition() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempFolder.newFolder(), "playback.preferences_pb") },
        )
        val prefs = PlaybackPreferencesDataSource(dataStore)
        val manager = PlaybackRestorationManager(prefs, database.audioTrackDao())

        // Nothing saved yet -> returns null.
        assertThat(manager.restoreLastAudioSession()).isNull()

        // Index tracks 10 and 30 (track 20 was deleted from disk).
        database.audioTrackDao().syncWith(
            listOf(
                sampleAudio(id = 10L, title = "First", durationMs = 200_000L),
                sampleAudio(id = 30L, title = "Third", durationMs = 150_000L),
            ),
        )

        manager.saveSnapshotDirectly(
            queueTrackIds = listOf(30L, 20L, 10L),
            currentIndex = 2, // points to track 10
            positionMs = 85_000L,
            repeatMode = PlaybackRepeatMode.ALL,
            shuffleEnabled = true,
            playbackSpeed = 1.25f,
        )

        val restored = manager.restoreLastAudioSession()
        assertThat(restored).isNotNull()
        assertThat(restored!!.mediaItems.map { it.mediaId })
            .containsExactly("audio:30", "audio:10")
            .inOrder()
        // Track 10 shifted from index 2 to index 1 after deleted track 20 was pruned.
        assertThat(restored.startIndex).isEqualTo(1)
        assertThat(restored.startPositionMs).isEqualTo(85_000L)
        assertThat(restored.repeatMode).isEqualTo(PlaybackRepeatMode.ALL)
        assertThat(restored.shuffleEnabled).isTrue()
        assertThat(restored.playbackSpeed).isEqualTo(1.25f)

        // Preferences toggle persists cleanly alongside snapshot.
        prefs.setContinueVideoInBackground(true)
        prefs.markNotificationPromptShown()
        prefs.setEqualizerEnabled(true)
        prefs.setEqualizerPreset(2, listOf(100, 0, -200, 300, 400))
        prefs.setBassBoost(true, 650)
        prefs.setGaplessEnabled(false)
        prefs.setCrossfadeDurationSec(6)
        val storedPrefs = prefs.preferences.first()
        assertThat(storedPrefs.continueVideoInBackground).isTrue()
        assertThat(storedPrefs.notificationPromptShown).isTrue()
        assertThat(storedPrefs.equalizerEnabled).isTrue()
        assertThat(storedPrefs.equalizerPresetIndex).isEqualTo(2)
        assertThat(storedPrefs.equalizerBandLevelsMb).containsExactly(100, 0, -200, 300, 400).inOrder()
        assertThat(storedPrefs.bassBoostEnabled).isTrue()
        assertThat(storedPrefs.bassBoostStrength).isEqualTo(650)
        assertThat(storedPrefs.gaplessEnabled).isFalse()
        assertThat(storedPrefs.crossfadeDurationSec).isEqualTo(6)

        // Clearing the queue removes any previously saved session snapshot.
        manager.clearAudioSession()
        assertThat(manager.restoreLastAudioSession()).isNull()
    }

    private fun sampleAudio(id: Long, title: String, durationMs: Long) = AudioTrackEntity(
        id = id,
        contentUri = "content://media/external/audio/media/$id",
        title = title,
        artist = "Artist",
        album = "Album",
        albumId = 1L,
        albumArtUri = null,
        durationMs = durationMs,
        sizeBytes = 4_000_000L,
        mimeType = "audio/flac",
        folderPath = "Music",
        folderName = "Music",
        trackNumber = 1,
        year = 2026,
        dateAddedSeconds = id,
        dateModifiedSeconds = id,
    )
}
