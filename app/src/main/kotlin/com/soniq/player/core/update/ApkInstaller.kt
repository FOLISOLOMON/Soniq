package com.soniq.player.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface ApkInstaller {
    fun requestInstall(apkFile: File): InstallLaunchResult
}

@Singleton
class AndroidApkInstaller @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ApkInstaller {

    override fun requestInstall(apkFile: File): InstallLaunchResult {
        if (!apkFile.isFile) {
            return InstallLaunchResult.Failed("Verified APK file is no longer available.")
        }

        if (!context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(settingsIntent) }
            return InstallLaunchResult.PermissionRequired
        }

        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.updateprovider",
                apkFile,
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            InstallLaunchResult.Launched
        }.getOrElse { error ->
            InstallLaunchResult.Failed(
                error.localizedMessage ?: "Unable to launch Android package installer.",
            )
        }
    }
}
