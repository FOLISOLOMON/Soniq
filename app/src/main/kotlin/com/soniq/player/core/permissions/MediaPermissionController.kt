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
    onResult: (granted: Boolean, permanentlyDenied: Boolean) -> Unit,
): MediaPermissionController {
    val context = LocalContext.current
    val permission = remember(kind) { MediaPermissions.requiredPermission(kind) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val activity = context.findActivity()
        val permanentlyDenied = !granted &&
            activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        onResult(granted, permanentlyDenied)
    }

    return remember(context, permission, launcher) {
        MediaPermissionController(
            requestPermission = { launcher.launch(permission) },
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
