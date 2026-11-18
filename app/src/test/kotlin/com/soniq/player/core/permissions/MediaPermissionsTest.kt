package com.soniq.player.core.permissions

import android.Manifest
import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.model.media.MediaKind
import org.junit.Test

class MediaPermissionsTest {

    @Test
    fun usesGranularPermissionsOnAndroid13AndNewer() {
        assertThat(
            MediaPermissions.requiredPermission(MediaKind.AUDIO, sdkInt = Build.VERSION_CODES.TIRAMISU),
        ).isEqualTo(Manifest.permission.READ_MEDIA_AUDIO)

        assertThat(
            MediaPermissions.requiredPermission(MediaKind.VIDEO, sdkInt = Build.VERSION_CODES.TIRAMISU),
        ).isEqualTo(Manifest.permission.READ_MEDIA_VIDEO)

        assertThat(
            MediaPermissions.requiredPermission(MediaKind.AUDIO, sdkInt = 37),
        ).isEqualTo(Manifest.permission.READ_MEDIA_AUDIO)
    }

    @Test
    fun usesExternalStoragePermissionBeforeAndroid13() {
        assertThat(
            MediaPermissions.requiredPermission(MediaKind.AUDIO, sdkInt = Build.VERSION_CODES.S_V2),
        ).isEqualTo(Manifest.permission.READ_EXTERNAL_STORAGE)

        assertThat(
            MediaPermissions.requiredPermission(MediaKind.VIDEO, sdkInt = Build.VERSION_CODES.O),
        ).isEqualTo(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
