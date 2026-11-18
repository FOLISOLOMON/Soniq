package com.soniq.player.core.playback

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.VideoItem

/**
 * Converts pure domain media models into rich Media3 [MediaItem] instances with complete
 * metadata for both in-app playback UI and system media surfaces (notification, lock screen,
 * Bluetooth).
 */
object MediaItemFactory {

    internal const val EXTRA_MEDIA_KIND = "com.soniq.player.extra.MEDIA_KIND"

    fun fromAudioTrack(track: AudioTrack): MediaItem {
        val extras = Bundle().apply {
            putString(EXTRA_MEDIA_KIND, MediaKind.AUDIO.name)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(track.albumArtUri?.toUri())
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId("audio:${track.id}")
            .setUri(track.contentUri.toUri())
            .setMimeType(track.mimeType)
            .setMediaMetadata(metadata)
            .build()
    }

    fun fromVideoItem(video: VideoItem): MediaItem {
        val extras = Bundle().apply {
            putString(EXTRA_MEDIA_KIND, MediaKind.VIDEO.name)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(video.title)
            .setSubtitle(video.folderName)
            .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId("video:${video.id}")
            .setUri(video.contentUri.toUri())
            .setMimeType(video.mimeType)
            .setMediaMetadata(metadata)
            .build()
    }

    fun resolveMediaKind(item: MediaItem?): MediaKind? {
        if (item == null) return null
        val explicit = item.mediaMetadata.extras?.getString(EXTRA_MEDIA_KIND)
        if (explicit != null) {
            return MediaKind.entries.firstOrNull { it.name == explicit }
        }
        return when (item.mediaMetadata.mediaType) {
            MediaMetadata.MEDIA_TYPE_VIDEO,
            MediaMetadata.MEDIA_TYPE_MOVIE,
            -> MediaKind.VIDEO

            MediaMetadata.MEDIA_TYPE_MUSIC -> MediaKind.AUDIO
            else -> {
                val mime = item.localConfiguration?.mimeType ?: return null
                when {
                    mime.startsWith("video/") -> MediaKind.VIDEO
                    mime.startsWith("audio/") -> MediaKind.AUDIO
                    else -> null
                }
            }
        }
    }
}
