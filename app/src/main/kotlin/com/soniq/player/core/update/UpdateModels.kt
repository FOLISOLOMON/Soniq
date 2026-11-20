package com.soniq.player.core.update

import kotlinx.serialization.Serializable
import java.io.File

/**
 * Versioned JSON manifest hosted over HTTPS on Vercel + Vercel Blob.
 *
 * Schema v1 contract:
 * - [schemaVersion]: currently `1`
 * - [versionCode] / [versionName]: target Android release version
 * - [apkUrl]: HTTPS URL to the signed release APK (e.g., Vercel Blob URL)
 * - [apkSizeBytes]: expected APK file size in bytes
 * - [sha256]: 64-char hex SHA-256 digest of the APK
 * - [minSupportedVersionCode]: lowest installed versionCode eligible for direct in-place update
 * - [mandatory]: advisory flag surfaced in UI when a release fixes critical issues
 * - [publishedAt]: ISO-8601 publication timestamp
 */
@Serializable
data class UpdateManifestDto(
    val schemaVersion: Int = SUPPORTED_SCHEMA_VERSION,
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String = "",
    val apkUrl: String,
    val apkSizeBytes: Long,
    val sha256: String,
    val minSupportedVersionCode: Int = 1,
    val mandatory: Boolean = false,
    val publishedAt: String = "",
) {
    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
    }
}

/** Validated domain model representing an available SONIQ release. */
data class UpdateRelease(
    val schemaVersion: Int,
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val sha256: String,
    val minSupportedVersionCode: Int,
    val mandatory: Boolean,
    val publishedAt: String,
)

data class UpdateConfig(
    val manifestUrl: String,
    val installedVersionCode: Int,
    val installedVersionName: String,
)

sealed interface UpdateCheckResult {
    data object NotConfigured : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class UpdateAvailable(
        val release: UpdateRelease,
        val isBelowMinimumSupported: Boolean,
    ) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

sealed interface UpdateVerificationResult {
    data object Valid : UpdateVerificationResult
    data class Invalid(val reason: String) : UpdateVerificationResult
}

sealed interface InstallLaunchResult {
    data object Launched : InstallLaunchResult
    data object PermissionRequired : InstallLaunchResult
    data class Failed(val reason: String) : InstallLaunchResult
}

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(
        val release: UpdateRelease,
        val isBelowMinimumSupported: Boolean,
    ) : UpdateUiState
    data class Downloading(
        val release: UpdateRelease,
        val bytesDownloaded: Long,
        val totalBytes: Long,
    ) : UpdateUiState {
        val progressFraction: Float
            get() = if (totalBytes > 0L) {
                (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
    }
    data class Verifying(val release: UpdateRelease) : UpdateUiState
    data class ReadyToInstall(
        val release: UpdateRelease,
        val apkFile: File,
    ) : UpdateUiState
    data class InstallRequested(
        val release: UpdateRelease,
        val apkFile: File,
    ) : UpdateUiState
    data class Error(
        val message: String,
        val recoverableRelease: UpdateRelease? = null,
    ) : UpdateUiState
}
