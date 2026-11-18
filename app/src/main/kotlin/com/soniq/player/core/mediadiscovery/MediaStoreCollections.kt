package com.soniq.player.core.mediadiscovery

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.soniq.player.core.model.media.MediaKind

/**
 * Resolves the canonical MediaStore content URIs for local audio and video collections.
 * Stage 1.2 builds the full MediaStore scanner and Room-backed index on top of this boundary.
 */
object MediaStoreCollections {

    fun collectionUri(
        kind: MediaKind,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Uri = when (kind) {
        MediaKind.AUDIO -> if (sdkInt >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        MediaKind.VIDEO -> if (sdkInt >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
    }
}
