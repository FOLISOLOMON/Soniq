package com.soniq.player.core.playback

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.database.media.SoniqMediaDatabase
import com.soniq.player.core.datastore.PlaybackPreferencesDataSource
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
class Media3PlaybackControllerTest {

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
    fun disconnectAndReconnectResetStateSafely() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempFolder.newFolder(), "ctrl_playback.preferences_pb") },
        )
        val restorationManager = PlaybackRestorationManager(
            preferences = PlaybackPreferencesDataSource(dataStore),
            audioTrackDao = database.audioTrackDao(),
        )
        val controller = Media3PlaybackController(context, restorationManager)

        assertThat(controller.state.value).isEqualTo(PlaybackState())

        // Connecting, disconnecting while in flight, handling service disconnection, and
        // reconnecting must leave state clean and never throw.
        controller.connect()
        controller.disconnect()
        assertThat(controller.state.value).isEqualTo(PlaybackState())

        controller.connect()
        controller.handleServiceDisconnectedForTest()
        assertThat(controller.state.value).isEqualTo(PlaybackState())

        // Safe no-ops while disconnected.
        controller.play()
        controller.pause()
        controller.stop()
        controller.seekTo(1_000L)
        assertThat(controller.state.value).isEqualTo(PlaybackState())
    }
}
