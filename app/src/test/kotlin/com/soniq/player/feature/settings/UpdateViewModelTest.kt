package com.soniq.player.feature.settings

import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.update.InstallLaunchResult
import com.soniq.player.core.update.UpdateCheckResult
import com.soniq.player.core.update.UpdateRelease
import com.soniq.player.core.update.UpdateRepository
import com.soniq.player.core.update.UpdateUiState
import com.soniq.player.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleRelease = UpdateRelease(
        schemaVersion = 1,
        versionCode = 2,
        versionName = "0.2.0",
        releaseNotes = "Liquid Glass + Updater",
        apkUrl = "https://updates.example.com/soniq.apk",
        apkSizeBytes = 1000L,
        sha256 = "a".repeat(64),
        minSupportedVersionCode = 1,
        mandatory = false,
        publishedAt = "2026-11-20T00:00:00Z",
    )

    @Test
    fun updateChecksOnlyRunAfterExplicitTapAndHandleInstallStates() = runTest {
        var checkCount = 0
        var installResult: InstallLaunchResult = InstallLaunchResult.PermissionRequired
        val downloadGate = CompletableDeferred<Result<File>>()

        val repo = object : UpdateRepository {
            override val installedVersionCode: Int = 1
            override val installedVersionName: String = "0.1.0"

            override suspend fun checkForUpdates(): UpdateCheckResult {
                checkCount++
                return UpdateCheckResult.UpdateAvailable(sampleRelease, isBelowMinimumSupported = false)
            }

            override suspend fun downloadAndVerify(
                release: UpdateRelease,
                onDownloadProgress: (Long, Long) -> Unit,
                onVerifying: () -> Unit,
            ): Result<File> {
                onDownloadProgress(500L, 1000L)
                return downloadGate.await()
            }

            override fun requestInstall(apkFile: File): InstallLaunchResult = installResult
        }

        val viewModel = UpdateViewModel(repo)
        advanceUntilIdle()
        // Zero automatic checks on startup/init
        assertThat(checkCount).isEqualTo(0)
        assertThat(viewModel.uiState.value).isEqualTo(UpdateUiState.Idle)

        // Explicit check
        viewModel.checkForUpdates()
        advanceUntilIdle()
        assertThat(checkCount).isEqualTo(1)
        assertThat(viewModel.uiState.value).isInstanceOf(UpdateUiState.Available::class.java)

        // Explicit download + cancel
        viewModel.downloadUpdate(sampleRelease)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(UpdateUiState.Downloading::class.java)

        viewModel.cancelDownload()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(UpdateUiState.Available::class.java)

        // Complete download + permission required + launched install
        val apkFile = File.createTempFile("soniq-test", ".apk")
        apkFile.deleteOnExit()
        val readyRepo = object : UpdateRepository by repo {
            override suspend fun downloadAndVerify(
                release: UpdateRelease,
                onDownloadProgress: (Long, Long) -> Unit,
                onVerifying: () -> Unit,
            ): Result<File> {
                onVerifying()
                return Result.success(apkFile)
            }
        }
        val readyVm = UpdateViewModel(readyRepo)
        readyVm.downloadUpdate(sampleRelease)
        advanceUntilIdle()
        assertThat(readyVm.uiState.value).isInstanceOf(UpdateUiState.ReadyToInstall::class.java)

        readyVm.installUpdate(sampleRelease, apkFile)
        assertThat(readyVm.uiState.value).isInstanceOf(UpdateUiState.Error::class.java)

        installResult = InstallLaunchResult.Launched
        readyVm.installUpdate(sampleRelease, apkFile)
        assertThat(readyVm.uiState.value).isInstanceOf(UpdateUiState.InstallRequested::class.java)
    }
}
