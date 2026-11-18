package com.soniq.player.core.playback

import androidx.media3.common.MediaMetadata
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.VideoItem
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaItemFactoryTest {

    @Test
    fun buildsAudioMediaItemWithRichMetadataAndAudioKind() {
        val track = AudioTrack(
            id = 42L,
            contentUri = "content://media/external/audio/media/42",
            title = "Dayvan Cowboy",
            artist = "Boards of Canada",
            album = "The Campfire Headphase",
            albumId = 7L,
            albumArtUri = "content://media/external/audio/albumart/7",
            durationMs = 300_000L,
            sizeBytes = 12_000_000L,
            mimeType = "audio/flac",
            folderPath = "Music/IDM",
            folderName = "IDM",
            trackNumber = 4,
            year = 2005,
            dateAddedSeconds = 1_000L,
            dateModifiedSeconds = 1_000L,
        )

        val item = MediaItemFactory.fromAudioTrack(track)
        assertThat(item.mediaId).isEqualTo("audio:42")
        assertThat(item.localConfiguration?.uri.toString()).isEqualTo(track.contentUri)
        assertThat(item.localConfiguration?.mimeType).isEqualTo("audio/flac")
        assertThat(item.mediaMetadata.title.toString()).isEqualTo("Dayvan Cowboy")
        assertThat(item.mediaMetadata.artist.toString()).isEqualTo("Boards of Canada")
        assertThat(item.mediaMetadata.albumTitle.toString()).isEqualTo("The Campfire Headphase")
        assertThat(item.mediaMetadata.artworkUri.toString()).isEqualTo(track.albumArtUri)
        assertThat(item.mediaMetadata.mediaType).isEqualTo(MediaMetadata.MEDIA_TYPE_MUSIC)
        assertThat(MediaItemFactory.resolveMediaKind(item)).isEqualTo(MediaKind.AUDIO)
    }

    @Test
    fun buildsVideoMediaItemWithVideoKind() {
        val video = VideoItem(
            id = 99L,
            contentUri = "content://media/external/video/media/99",
            title = "Alps Drone",
            displayName = "Alps Drone.mp4",
            durationMs = 150_000L,
            sizeBytes = 250_000_000L,
            mimeType = "video/mp4",
            width = 3840,
            height = 2160,
            folderPath = "Movies/Travel",
            folderName = "Travel",
            dateAddedSeconds = 2_000L,
            dateModifiedSeconds = 2_000L,
        )

        val item = MediaItemFactory.fromVideoItem(video)
        assertThat(item.mediaId).isEqualTo("video:99")
        assertThat(item.mediaMetadata.title.toString()).isEqualTo("Alps Drone")
        assertThat(item.mediaMetadata.subtitle.toString()).isEqualTo("Travel")
        assertThat(item.mediaMetadata.mediaType).isEqualTo(MediaMetadata.MEDIA_TYPE_VIDEO)
        assertThat(MediaItemFactory.resolveMediaKind(item)).isEqualTo(MediaKind.VIDEO)
    }
}
