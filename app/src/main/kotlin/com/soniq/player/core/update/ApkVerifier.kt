package com.soniq.player.core.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
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

        if (archiveInfo.packageName != context.packageName) {
            apkFile.delete()
            return@withContext UpdateVerificationResult.Invalid(
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
            apkFile.delete()
            return@withContext UpdateVerificationResult.Invalid("APK versionCode does not match manifest.")
        }

        val installedSigners = getInstalledSigners(pm, context.packageName)
        val archiveSigners = extractSignerDigests(archiveInfo)
        if (installedSigners.isEmpty() || archiveSigners.isEmpty() || installedSigners != archiveSigners) {
            apkFile.delete()
            return@withContext UpdateVerificationResult.Invalid(
                "APK signing certificate does not match the installed SONIQ signature.",
            )
        }

        UpdateVerificationResult.Valid
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
    private fun extractSignerDigests(info: PackageInfo): Set<String> {
        val rawSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            info.signatures
        } ?: return emptySet()

        val md = MessageDigest.getInstance("SHA-256")
        return rawSignatures.map { sig ->
            md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
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
