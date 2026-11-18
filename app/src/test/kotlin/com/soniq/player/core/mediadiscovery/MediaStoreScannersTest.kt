package com.soniq.player.core.mediadiscovery

import android.content.Context
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaStoreScannersTest {

    @Test
    fun mapsAudioCursorRowsAndFiltersZeroDuration() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        val scanner = MediaStoreAudioScanner(resolver, Dispatchers.Unconfined)
        val collectionUri = Uri.parse("content://media/external/audio/media")
        val cursor = MatrixCursor(scanner.buildProjection(Build.VERSION_CODES.UPSIDE_DOWN_CAKE))

        cursor.addRow(
            arrayOf<Any?>(
                10L,
                "Midnight Signal",
                "01_midnight.flac",
                "<unknown>",
                "Night Drives",
                42L,
                215_000L,
                18_500_000L,
                "audio/flac",
                1,
                2026,
                1_700_000_100L,
                1_700_000_200L,
                "Music/Synth/",
            ),
        )
        // Zero-duration row must be ignored.
        cursor.addRow(
            arrayOf<Any?>(
                11L,
                "Broken File",
                "broken.mp3",
                "Artist",
                "Album",
                0L,
                0L,
                100L,
                "audio/mpeg",
                0,
                0,
                1L,
                1L,
                "Music/",
            ),
        )

        val tracks = scanner.mapCursor(cursor, collectionUri, Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        assertThat(tracks).hasSize(1)
        val track = tracks.first()
        assertThat(track.id).isEqualTo(10L)
        assertThat(track.contentUri).isEqualTo("content://media/external/audio/media/10")
        assertThat(track.title).isEqualTo("Midnight Signal")
        assertThat(track.artist).isEqualTo("Unknown Artist")
        assertThat(track.albumArtUri).isEqualTo("content://media/external/audio/albumart/42")
        assertThat(track.folderPath).isEqualTo("Music/Synth")
        assertThat(track.folderName).isEqualTo("Synth")
    }

    @Test
    fun mapsVideoCursorRowsWithDisplayNameTitleFallback() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        val scanner = MediaStoreVideoScanner(resolver, Dispatchers.Unconfined)
        val collectionUri = Uri.parse("content://media/external/video/media")
        val cursor = MatrixCursor(scanner.buildProjection(Build.VERSION_CODES.UPSIDE_DOWN_CAKE))

        cursor.addRow(
            arrayOf<Any?>(
                77L,
                "  ",
                "Ocean_Sunset_4K.mp4",
                125_000L,
                420_000_000L,
                "video/mp4",
                3840,
                2160,
                1_700_000_500L,
                1_700_000_600L,
                "DCIM/Camera/",
            ),
        )

        val videos = scanner.mapCursor(cursor, collectionUri, Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        assertThat(videos).hasSize(1)
        val video = videos.first()
        assertThat(video.id).isEqualTo(77L)
        assertThat(video.contentUri).isEqualTo("content://media/external/video/media/77")
        assertThat(video.title).isEqualTo("Ocean_Sunset_4K")
        assertThat(video.width).isEqualTo(3840)
        assertThat(video.height).isEqualTo(2160)
        assertThat(video.folderName).isEqualTo("Camera")
    }
}
