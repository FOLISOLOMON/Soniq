package com.soniq.player.core.permissions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.soniq.player.core.model.media.MediaKind

/**
 * UI-layer helper returned by [rememberMediaPermissionController] to launch the system runtime
 * permission prompt or open system app settings when permanently denied.
 */
class MediaPermissionController(
    val requestPermission: () -> Unit,
    val openAppSettings: () -> Unit,
)

@Composable
fun rememberMediaPermissionController(
    kind: MediaKind,
    onResult: (grant: MediaAccessGrant, permanentlyDenied: Boolean) -> Unit,
): MediaPermissionController {
    val context = LocalContext.current
    val permissions = remember(kind) { MediaPermissions.requiredPermissions(kind) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val grantedPermissions = results.filterValues { it }.keys
        val grant = MediaPermissions.resolveAccessGrant(kind, grantedPermissions)
        val activity = context.findActivity()
        val permanentlyDenied = grant == MediaAccessGrant.NONE &&
            activity != null &&
            permissions.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        onResult(grant, permanentlyDenied)
    }

    return remember(context, permissions, launcher) {
        MediaPermissionController(
            requestPermission = { launcher.launch(permissions.toTypedArray()) },
            openAppSettings = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
