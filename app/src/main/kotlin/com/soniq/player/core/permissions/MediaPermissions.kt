package com.soniq.player.core.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import com.soniq.player.core.model.media.MediaKind

/**
 * Resolves the runtime permissions required to discover local media of a given [MediaKind]
 * across all Android API levels supported by SONIQ (API 26+), including Android 14+
 * Selected Photos/Videos Access (`READ_MEDIA_VISUAL_USER_SELECTED`).
 */
object MediaPermissions {

    /**
     * Returns the permission(s) that should be requested from the system for [kind] on [sdkInt].
     * Constants are compile-time inlined strings and runtime-guarded by [sdkInt], which is
     * parameterized so all API branches can be unit-tested on the JVM.
     */
    @SuppressLint("InlinedApi")
    fun requiredPermissions(
        kind: MediaKind,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): List<String> = when {
        sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> when (kind) {
            MediaKind.AUDIO -> listOf(Manifest.permission.READ_MEDIA_AUDIO)
            MediaKind.VIDEO -> listOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
        }

        sdkInt >= Build.VERSION_CODES.TIRAMISU -> when (kind) {
            MediaKind.AUDIO -> listOf(Manifest.permission.READ_MEDIA_AUDIO)
            MediaKind.VIDEO -> listOf(Manifest.permission.READ_MEDIA_VIDEO)
        }

        else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /**
     * Evaluates the effective access grant level for [kind] given the set of currently
     * [grantedPermissions] on [sdkInt].
     */
    @SuppressLint("InlinedApi")
    fun resolveAccessGrant(
        kind: MediaKind,
        grantedPermissions: Set<String>,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): MediaAccessGrant = when {
        sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> when (kind) {
            MediaKind.AUDIO -> if (Manifest.permission.READ_MEDIA_AUDIO in grantedPermissions) {
                MediaAccessGrant.FULL
            } else {
                MediaAccessGrant.NONE
            }

            MediaKind.VIDEO -> when {
                Manifest.permission.READ_MEDIA_VIDEO in grantedPermissions -> MediaAccessGrant.FULL
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED in grantedPermissions -> MediaAccessGrant.PARTIAL
                else -> MediaAccessGrant.NONE
            }
        }

        sdkInt >= Build.VERSION_CODES.TIRAMISU -> {
            val required = when (kind) {
                MediaKind.AUDIO -> Manifest.permission.READ_MEDIA_AUDIO
                MediaKind.VIDEO -> Manifest.permission.READ_MEDIA_VIDEO
            }
            if (required in grantedPermissions) MediaAccessGrant.FULL else MediaAccessGrant.NONE
        }

        else -> if (Manifest.permission.READ_EXTERNAL_STORAGE in grantedPermissions) {
            MediaAccessGrant.FULL
        } else {
            MediaAccessGrant.NONE
        }
    }
}

/** Underlying OS permission grant level for a media category. */
enum class MediaAccessGrant {
    FULL,
    PARTIAL,
    NONE,
}

/** UI-facing access state for a single media category. */
enum class MediaAccessState {
    GRANTED,
    PARTIAL,
    NOT_GRANTED,
    DENIED_PERMANENTLY,
}

/** Abstraction over Android's permission check so domain & presentation layers remain testable. */
interface PermissionChecker {
    fun checkMediaAccess(kind: MediaKind): MediaAccessGrant

    fun hasMediaAccess(kind: MediaKind): Boolean = checkMediaAccess(kind) != MediaAccessGrant.NONE
}
