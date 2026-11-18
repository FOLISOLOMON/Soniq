package com.soniq.player.core.permissions

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.soniq.player.core.model.media.MediaKind
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPermissionChecker @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PermissionChecker {

    override fun checkMediaAccess(kind: MediaKind): MediaAccessGrant {
        val granted = MediaPermissions.requiredPermissions(kind)
            .filterTo(mutableSetOf()) { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        return MediaPermissions.resolveAccessGrant(kind, granted)
    }
}
