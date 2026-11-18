package com.soniq.player.core.permissions

import android.Manifest
import android.os.Build
import com.soniq.player.core.model.media.MediaKind

/**
 * Resolves the runtime permission required to discover local media of a given [MediaKind]
 * for any Android API level supported by SONIQ (API 26+).
 */
object MediaPermissions {

    fun requiredPermission(
        kind: MediaKind,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): String = if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        when (kind) {
            MediaKind.AUDIO -> Manifest.permission.READ_MEDIA_AUDIO
            MediaKind.VIDEO -> Manifest.permission.READ_MEDIA_VIDEO
        }
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

/** Represents the access state for a single media category. */
enum class MediaAccessState {
    GRANTED,
    NOT_GRANTED,
    DENIED_PERMANENTLY,
}

/** Abstraction over Android's permission check so domain & presentation layers remain testable. */
interface PermissionChecker {
    fun hasMediaAccess(kind: MediaKind): Boolean
}
