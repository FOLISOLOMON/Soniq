package com.soniq.player.core.permissions

import android.Manifest
import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationPermissionsTest {

    @Test
    fun requiresPostNotificationsOnlyOnAndroid13AndNewer() {
        assertThat(NotificationPermissions.isRuntimePermissionRequired(Build.VERSION_CODES.S_V2)).isFalse()
        assertThat(NotificationPermissions.requiredPermission(Build.VERSION_CODES.S_V2)).isNull()

        assertThat(NotificationPermissions.isRuntimePermissionRequired(Build.VERSION_CODES.TIRAMISU)).isTrue()
        assertThat(NotificationPermissions.requiredPermission(Build.VERSION_CODES.TIRAMISU))
            .isEqualTo(Manifest.permission.POST_NOTIFICATIONS)
        assertThat(NotificationPermissions.requiredPermission(37))
            .isEqualTo(Manifest.permission.POST_NOTIFICATIONS)
    }
}
