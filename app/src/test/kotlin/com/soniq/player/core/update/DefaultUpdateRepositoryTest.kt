package com.soniq.player.core.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DefaultUpdateRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun validDto(versionCode: Int = 2, apkUrl: String = "https://updates.example.com/soniq.apk") =
        UpdateManifestDto(
            schemaVersion = 1,
            versionCode = versionCode,
            versionName = "0.$versionCode.0",
            releaseNotes = "Bug fixes",
            apkUrl = apkUrl,
            apkSizeBytes = 1024L,
            sha256 = "a".repeat(64),
            minSupportedVersionCode = 1,
            mandatory = false,
            publishedAt = "2026-11-20T00:00:00Z",
        )

    @Test
    fun notConfiguredAndVersionComparisonAndInvalidManifestCases() = runTest {
        var fetchCalls = 0
        val remote = object : UpdateRemoteDataSource {
            var nextManifest: Result<UpdateManifestDto> = Result.success(validDto(versionCode = 1))
            override suspend fun fetchManifest(manifestUrl: String): Result<UpdateManifestDto> {
                fetchCalls++
                return nextManifest
            }

            override suspend fun downloadApk(
                apkUrl: String,
                expectedSizeBytes: Long,
                destinationFile: File,
                onProgress: (Long, Long) -> Unit,
            ): Result<File> = Result.success(destinationFile)
        }
        val verifier = object : ApkVerifier {
            override suspend fun verifyDownloadedApk(apkFile: File, expectedRelease: UpdateRelease) =
                UpdateVerificationResult.Valid
        }
        val installer = object : ApkInstaller {
            override fun requestInstall(apkFile: File) = InstallLaunchResult.Launched
        }

        // Empty manifest URL -> NotConfigured, 0 network calls
        val unconfigured = DefaultUpdateRepository(
            context,
            UpdateConfig("", 1, "0.1.0"),
            remote,
            verifier,
            installer,
        )
        assertThat(unconfigured.checkForUpdates()).isEqualTo(UpdateCheckResult.NotConfigured)
        assertThat(fetchCalls).isEqualTo(0)

        val repo = DefaultUpdateRepository(
            context,
            UpdateConfig("https://updates.example.com/manifest.json", 2, "0.2.0"),
            remote,
            verifier,
            installer,
        )
        // Constructing repository never auto-checks
        assertThat(fetchCalls).isEqualTo(0)

        // Same version -> UpToDate
        remote.nextManifest = Result.success(validDto(versionCode = 2))
        assertThat(repo.checkForUpdates()).isEqualTo(UpdateCheckResult.UpToDate)

        // Higher version -> UpdateAvailable
        remote.nextManifest = Result.success(validDto(versionCode = 3))
        val available = repo.checkForUpdates() as UpdateCheckResult.UpdateAvailable
        assertThat(available.release.versionCode).isEqualTo(3)
        assertThat(available.isBelowMinimumSupported).isFalse()

        // Unsupported schema
        remote.nextManifest = Result.success(validDto(versionCode = 4).copy(schemaVersion = 99))
        assertThat(repo.checkForUpdates()).isInstanceOf(UpdateCheckResult.Error::class.java)

        // Non-HTTPS APK URL rejected
        remote.nextManifest = Result.success(validDto(versionCode = 4, apkUrl = "http://insecure.example.com/a.apk"))
        assertThat(repo.checkForUpdates()).isInstanceOf(UpdateCheckResult.Error::class.java)

        // Server unavailable
        remote.nextManifest = Result.failure(IllegalStateException("HTTP 503"))
        assertThat(repo.checkForUpdates()).isInstanceOf(UpdateCheckResult.Error::class.java)
    }

    @Test
    fun downloadFailureVerificationFailureAndCancellationAreHandledCleanly() = runTest {
        val release = UpdateRelease(1, 2, "0.2.0", "", "https://updates.example.com/a.apk", 10L, "a".repeat(64), 1, false, "")
        var verificationResult: UpdateVerificationResult = UpdateVerificationResult.Invalid("SHA-256 mismatch")
        var downloadResult: Result<File>? = null

        val remote = object : UpdateRemoteDataSource {
            override suspend fun fetchManifest(manifestUrl: String) = Result.failure<UpdateManifestDto>(IllegalStateException())
            override suspend fun downloadApk(
                apkUrl: String,
                expectedSizeBytes: Long,
                destinationFile: File,
                onProgress: (Long, Long) -> Unit,
            ): Result<File> {
                destinationFile.writeText("dummy")
                onProgress(5L, 10L)
                return downloadResult ?: Result.success(destinationFile)
            }
        }
        val repo = DefaultUpdateRepository(
            context,
            UpdateConfig("https://updates.example.com/manifest.json", 1, "0.1.0"),
            remote,
            object : ApkVerifier {
                override suspend fun verifyDownloadedApk(apkFile: File, expectedRelease: UpdateRelease) =
                    verificationResult
            },
            object : ApkInstaller {
                override fun requestInstall(apkFile: File) = InstallLaunchResult.Launched
            },
        )

        // Verification failure propagates cleanly
        val failedVerify = repo.downloadAndVerify(release, { _, _ -> }, {})
        assertThat(failedVerify.isFailure).isTrue()
        assertThat(failedVerify.exceptionOrNull()?.message).contains("SHA-256 mismatch")

        // Insufficient storage / network failure
        downloadResult = Result.failure(IllegalStateException("Insufficient storage"))
        val failedDownload = repo.downloadAndVerify(release, { _, _ -> }, {})
        assertThat(failedDownload.isFailure).isTrue()

        // Valid download + verification succeeds
        downloadResult = null
        verificationResult = UpdateVerificationResult.Valid
        val ok = repo.downloadAndVerify(release, { _, _ -> }, {})
        assertThat(ok.isSuccess).isTrue()

        // CancellationException is rethrown (not swallowed as failure)
        downloadResult = Result.failure(CancellationException("cancelled"))
        // Simulate throwing CancellationException directly
        val cancellingRemote = object : UpdateRemoteDataSource {
            override suspend fun fetchManifest(manifestUrl: String) = throw UnsupportedOperationException()
            override suspend fun downloadApk(
                apkUrl: String,
                expectedSizeBytes: Long,
                destinationFile: File,
                onProgress: (Long, Long) -> Unit,
            ): Result<File> = throw CancellationException("User cancelled")
        }
        val cancelRepo = DefaultUpdateRepository(
            context,
            UpdateConfig("https://updates.example.com/manifest.json", 1, "0.1.0"),
            cancellingRemote,
            object : ApkVerifier {
                override suspend fun verifyDownloadedApk(apkFile: File, expectedRelease: UpdateRelease) =
                    UpdateVerificationResult.Valid
            },
            object : ApkInstaller {
                override fun requestInstall(apkFile: File) = InstallLaunchResult.Launched
            },
        )
        var cancelled = false
        try {
            cancelRepo.downloadAndVerify(release, { _, _ -> }, {})
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertThat(cancelled).isTrue()
    }
}
