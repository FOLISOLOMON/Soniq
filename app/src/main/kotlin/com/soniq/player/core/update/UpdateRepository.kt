package com.soniq.player.core.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface UpdateRepository {
    val installedVersionCode: Int
    val installedVersionName: String
    suspend fun checkForUpdates(): UpdateCheckResult
    suspend fun downloadAndVerify(
        release: UpdateRelease,
        onDownloadProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
        onVerifying: () -> Unit,
    ): Result<File>
    fun requestInstall(apkFile: File): InstallLaunchResult
}

@Singleton
class DefaultUpdateRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val config: UpdateConfig,
    private val remoteDataSource: UpdateRemoteDataSource,
    private val apkVerifier: ApkVerifier,
    private val apkInstaller: ApkInstaller,
) : UpdateRepository {

    override val installedVersionCode: Int
        get() = config.installedVersionCode

    override val installedVersionName: String
        get() = config.installedVersionName

    override suspend fun checkForUpdates(): UpdateCheckResult {
        val manifestUrl = config.manifestUrl.trim()
        if (manifestUrl.isEmpty()) {
            return UpdateCheckResult.NotConfigured
        }

        val dto = remoteDataSource.fetchManifest(manifestUrl).getOrElse { error ->
            return UpdateCheckResult.Error(error.message ?: "Failed to check for updates.")
        }

        if (dto.schemaVersion != UpdateManifestDto.SUPPORTED_SCHEMA_VERSION) {
            return UpdateCheckResult.Error(
                "Unsupported update manifest schema v${dto.schemaVersion}.",
            )
        }
        if (dto.versionCode <= 0 || dto.versionName.isBlank()) {
            return UpdateCheckResult.Error("Invalid version metadata in update manifest.")
        }
        if (!dto.apkUrl.trim().startsWith("https://", ignoreCase = true)) {
            return UpdateCheckResult.Error("Invalid APK download URL in manifest.")
        }
        if (dto.sha256.trim().length != 64) {
            return UpdateCheckResult.Error("Invalid SHA-256 checksum in manifest.")
        }

        if (dto.versionCode <= config.installedVersionCode) {
            return UpdateCheckResult.UpToDate
        }

        val release = UpdateRelease(
            schemaVersion = dto.schemaVersion,
            versionCode = dto.versionCode,
            versionName = dto.versionName.trim(),
            releaseNotes = dto.releaseNotes.trim(),
            apkUrl = dto.apkUrl.trim(),
            apkSizeBytes = dto.apkSizeBytes,
            sha256 = dto.sha256.trim().lowercase(),
            minSupportedVersionCode = dto.minSupportedVersionCode,
            mandatory = dto.mandatory,
            publishedAt = dto.publishedAt.trim(),
        )
        return UpdateCheckResult.UpdateAvailable(
            release = release,
            isBelowMinimumSupported = config.installedVersionCode < release.minSupportedVersionCode,
        )
    }

    override suspend fun downloadAndVerify(
        release: UpdateRelease,
        onDownloadProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
        onVerifying: () -> Unit,
    ): Result<File> {
        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) {
            updatesDir.mkdirs()
        } else {
            updatesDir.listFiles()?.forEach { existing ->
                if (existing.isFile && (existing.name.endsWith(".apk") || existing.name.endsWith(".part"))) {
                    existing.delete()
                }
            }
        }
        val targetApk = File(updatesDir, "soniq-${release.versionCode}.apk")

        val downloaded = remoteDataSource.downloadApk(
            apkUrl = release.apkUrl,
            expectedSizeBytes = release.apkSizeBytes,
            destinationFile = targetApk,
            onProgress = onDownloadProgress,
        ).getOrElse { error ->
            return Result.failure(error)
        }

        onVerifying()
        return when (val verification = apkVerifier.verifyDownloadedApk(downloaded, release)) {
            is UpdateVerificationResult.Valid -> Result.success(downloaded)
            is UpdateVerificationResult.Invalid -> Result.failure(IllegalStateException(verification.reason))
        }
    }

    override fun requestInstall(apkFile: File): InstallLaunchResult =
        apkInstaller.requestInstall(apkFile)
}
