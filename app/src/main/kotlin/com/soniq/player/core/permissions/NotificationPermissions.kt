package com.soniq.player.core.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure-Kotlin helper for Android 13+ optional `POST_NOTIFICATIONS` permission used by playback
 * notifications and lock-screen media controls. Playback itself is never blocked if declined.
 */
object NotificationPermissions {

    fun isRuntimePermissionRequired(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        sdkInt >= Build.VERSION_CODES.TIRAMISU

    @SuppressLint("InlinedApi")
    fun requiredPermission(sdkInt: Int = Build.VERSION.SDK_INT): String? =
        if (isRuntimePermissionRequired(sdkInt)) Manifest.permission.POST_NOTIFICATIONS else null
}

/** Checks whether the app may post media playback notifications. */
interface NotificationPermissionChecker {
    fun canPostNotifications(): Boolean
}

@Singleton
class AndroidNotificationPermissionChecker @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NotificationPermissionChecker {

    override fun canPostNotifications(): Boolean {
        val permission = NotificationPermissions.requiredPermission() ?: return true
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
