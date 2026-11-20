package com.soniq.player.core.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.soniq.player.core.common.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

interface ApkVerifier {
    suspend fun verifyDownloadedApk(
        apkFile: File,
        expectedRelease: UpdateRelease,
    ): UpdateVerificationResult
}

@Singleton
class AndroidApkVerifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ApkVerifier {

    override suspend fun verifyDownloadedApk(
        apkFile: File,
        expectedRelease: UpdateRelease,
    ): UpdateVerificationResult = withContext(ioDispatcher) {
        if (!apkFile.isFile || apkFile.length() <= 0L) {
            return@withContext UpdateVerificationResult.Invalid("Downloaded APK file is missing or empty.")
        }

        if (expectedRelease.apkSizeBytes > 0L && apkFile.length() != expectedRelease.apkSizeBytes) {
            apkFile.delete()
            return@withContext UpdateVerificationResult.Invalid(
                "Downloaded APK size mismatch (expected ${expectedRelease.apkSizeBytes} bytes, got ${apkFile.length()}).",
            )
        }

        val actualSha256 = computeSha256Hex(apkFile)
        val expectedSha256 = expectedRelease.sha256.trim().lowercase()
        if (expectedSha256.length != SHA256_HEX_LENGTH || actualSha256 != expectedSha256) {
            apkFile.delete()
            return@withContext UpdateVerificationResult.Invalid("SHA-256 checksum verification failed.")
        }

        val pm = context.packageManager
        val archiveInfo = getArchivePackageInfo(pm, apkFile.absolutePath)
            ?: run {
                apkFile.delete()
                return@withContext UpdateVerificationResult.Invalid("Downloaded file is not a valid Android APK.")
            }

        val installedSigners = getInstalledSigners(pm, context.packageName)
        val result = verifyArchiveMetadata(archiveInfo, expectedRelease, installedSigners)
        if (result is UpdateVerificationResult.Invalid) {
            apkFile.delete()
        }
        result
    }

    internal fun verifyArchiveMetadata(
        archiveInfo: PackageInfo,
        expectedRelease: UpdateRelease,
        installedSigners: Set<String>,
    ): UpdateVerificationResult {
        if (archiveInfo.packageName != context.packageName) {
            return UpdateVerificationResult.Invalid(
                "APK package ID mismatch (${archiveInfo.packageName} != ${context.packageName}).",
            )
        }

        val archiveVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode.toLong()
        }
        if (archiveVersionCode < expectedRelease.versionCode.toLong()) {
            return UpdateVerificationResult.Invalid("APK versionCode does not match manifest.")
        }

        if (installedSigners.isEmpty()) {
            return UpdateVerificationResult.Invalid(
                "Unable to read the installed SONIQ signing certificate.",
            )
        }

        val archiveSigners = extractSignerDigests(archiveInfo)
        if (archiveSigners.isEmpty()) {
            return UpdateVerificationResult.Invalid(
                "Unable to read the APK signing certificate.",
            )
        }

        if (installedSigners != archiveSigners) {
            return UpdateVerificationResult.Invalid(
                "APK signing certificate does not match the installed SONIQ signature.",
            )
        }

        return UpdateVerificationResult.Valid
    }

    @Suppress("DEPRECATION")
    private fun getArchivePackageInfo(pm: PackageManager, path: String): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return runCatching { pm.getPackageArchiveInfo(path, flags) }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun getInstalledSigners(pm: PackageManager, packageName: String): Set<String> {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val info = runCatching { pm.getPackageInfo(packageName, flags) }.getOrNull() ?: return emptySet()
        return extractSignerDigests(info)
    }

    @Suppress("DEPRECATION")
    internal fun extractSignerDigests(info: PackageInfo): Set<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            extractSignerDigestsFromParts(
                apkContentsSigners = signingInfo.apkContentsSigners,
                signingCertificateHistory = signingInfo.signingCertificateHistory,
                hasMultipleSigners = signingInfo.hasMultipleSigners(),
                legacySignatures = null,
                isApi28Plus = true,
            )
        } else {
            extractSignerDigestsFromParts(
                apkContentsSigners = null,
                signingCertificateHistory = null,
                hasMultipleSigners = false,
                legacySignatures = info.signatures,
                isApi28Plus = false,
            )
        }
    }

    internal fun extractSignerDigestsFromParts(
        apkContentsSigners: Array<Signature>?,
        signingCertificateHistory: Array<Signature>?,
        hasMultipleSigners: Boolean,
        legacySignatures: Array<Signature>?,
        isApi28Plus: Boolean,
    ): Set<String> {
        val rawSignatures: Array<Signature>? = if (isApi28Plus) {
            val contentSigners = apkContentsSigners?.takeIf { it.isNotEmpty() }
            when {
                contentSigners != null -> contentSigners
                !hasMultipleSigners -> signingCertificateHistory?.takeIf { it.isNotEmpty() }
                else -> null
            }
        } else {
            legacySignatures?.takeIf { it.isNotEmpty() }
        }

        if (rawSignatures.isNullOrEmpty()) return emptySet()

        val md = MessageDigest.getInstance("SHA-256")
        return rawSignatures.mapNotNull { sig ->
            val bytes = sig.toByteArray()
            if (bytes.isEmpty()) return@mapNotNull null
            md.digest(bytes).joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    private fun computeSha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val SHA256_HEX_LENGTH = 64
    }
}
