package com.soniq.player.core.permissions

import android.Manifest
import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.model.media.MediaKind
import org.junit.Test

class MediaPermissionsTest {

    @Test
    fun requestsVisualUserSelectedPermissionForVideoOnAndroid14AndNewer() {
        assertThat(
            MediaPermissions.requiredPermissions(MediaKind.AUDIO, sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE),
        ).containsExactly(Manifest.permission.READ_MEDIA_AUDIO)

        assertThat(
            MediaPermissions.requiredPermissions(MediaKind.VIDEO, sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE),
        ).containsExactly(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        assertThat(
            MediaPermissions.requiredPermissions(MediaKind.VIDEO, sdkInt = 37),
        ).containsExactly(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
    }

    @Test
    fun usesGranularPermissionsOnAndroid13() {
        assertThat(
            MediaPermissions.requiredPermissions(MediaKind.AUDIO, sdkInt = Build.VERSION_CODES.TIRAMISU),
        ).containsExactly(Manifest.permission.READ_MEDIA_AUDIO)

        assertThat(
            MediaPermissions.requiredPermissions(MediaKind.VIDEO, sdkInt = Build.VERSION_CODES.TIRAMISU),
        ).containsExactly(Manifest.permission.READ_MEDIA_VIDEO)
    }

    @Test
    fun usesExternalStoragePermissionBeforeAndroid13() {
        assertThat(
            MediaPermissions.requiredPermissions(MediaKind.AUDIO, sdkInt = Build.VERSION_CODES.S_V2),
        ).containsExactly(Manifest.permission.READ_EXTERNAL_STORAGE)

        assertThat(
            MediaPermissions.requiredPermissions(MediaKind.VIDEO, sdkInt = Build.VERSION_CODES.O),
        ).containsExactly(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @Test
    fun resolvesFullPartialAndNoneVideoAccessOnAndroid14AndNewer() {
        val api34 = Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        assertThat(
            MediaPermissions.resolveAccessGrant(
                kind = MediaKind.VIDEO,
                grantedPermissions = setOf(Manifest.permission.READ_MEDIA_VIDEO),
                sdkInt = api34,
            ),
        ).isEqualTo(MediaAccessGrant.FULL)

        assertThat(
            MediaPermissions.resolveAccessGrant(
                kind = MediaKind.VIDEO,
                grantedPermissions = setOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
                sdkInt = api34,
            ),
        ).isEqualTo(MediaAccessGrant.PARTIAL)

        assertThat(
            MediaPermissions.resolveAccessGrant(
                kind = MediaKind.VIDEO,
                grantedPermissions = setOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                ),
                sdkInt = api34,
            ),
        ).isEqualTo(MediaAccessGrant.FULL)

        assertThat(
            MediaPermissions.resolveAccessGrant(
                kind = MediaKind.VIDEO,
                grantedPermissions = emptySet(),
                sdkInt = api34,
            ),
        ).isEqualTo(MediaAccessGrant.NONE)
    }
}
