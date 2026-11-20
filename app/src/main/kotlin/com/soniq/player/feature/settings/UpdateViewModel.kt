package com.soniq.player.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soniq.player.core.update.InstallLaunchResult
import com.soniq.player.core.update.UpdateCheckResult
import com.soniq.player.core.update.UpdateRelease
import com.soniq.player.core.update.UpdateRepository
import com.soniq.player.core.update.UpdateUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Coordinates user-initiated update checks, downloads, verification, and Android installer handoff.
 *
 * Never performs automatic checks on init, app startup, or background timers; every network request
 * is strictly triggered by an explicit user tap in Settings -> About SONIQ.
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
) : ViewModel() {

    val installedVersionCode: Int = updateRepository.installedVersionCode
    val installedVersionName: String = updateRepository.installedVersionName

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

    fun checkForUpdates() {
        if (_uiState.value is UpdateUiState.Checking || _uiState.value is UpdateUiState.Downloading) {
            return
        }
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.value = UpdateUiState.Checking
            _uiState.value = when (val result = updateRepository.checkForUpdates()) {
                is UpdateCheckResult.NotConfigured -> UpdateUiState.Error(
                    message = NOT_CONFIGURED_MESSAGE,
                )
                is UpdateCheckResult.UpToDate -> UpdateUiState.UpToDate
                is UpdateCheckResult.UpdateAvailable -> UpdateUiState.Available(
                    release = result.release,
                    isBelowMinimumSupported = result.isBelowMinimumSupported,
                )
                is UpdateCheckResult.Error -> UpdateUiState.Error(message = result.message)
            }
        }
    }

    fun downloadUpdate(release: UpdateRelease) {
        if (_uiState.value is UpdateUiState.Downloading || _uiState.value is UpdateUiState.Verifying) {
            return
        }
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.value = UpdateUiState.Downloading(
                release = release,
                bytesDownloaded = 0L,
                totalBytes = release.apkSizeBytes,
            )
            try {
                val result = updateRepository.downloadAndVerify(
                    release = release,
                    onDownloadProgress = { downloaded, total ->
                        _uiState.value = UpdateUiState.Downloading(
                            release = release,
                            bytesDownloaded = downloaded,
                            totalBytes = if (total > 0L) total else release.apkSizeBytes,
                        )
                    },
                    onVerifying = {
                        _uiState.value = UpdateUiState.Verifying(release)
                    },
                )
                _uiState.value = result.fold(
                    onSuccess = { file -> UpdateUiState.ReadyToInstall(release = release, apkFile = file) },
                    onFailure = { error ->
                        UpdateUiState.Error(
                            message = error.message ?: "Update download or verification failed.",
                            recoverableRelease = release,
                        )
                    },
                )
            } catch (_: CancellationException) {
                _uiState.value = UpdateUiState.Available(
                    release = release,
                    isBelowMinimumSupported = installedVersionCode < release.minSupportedVersionCode,
                )
            }
        }
    }

    fun cancelDownload() {
        activeJob?.cancel()
        activeJob = null
    }

    fun installUpdate(release: UpdateRelease, apkFile: File) {
        when (val result = updateRepository.requestInstall(apkFile)) {
            is InstallLaunchResult.Launched -> {
                _uiState.value = UpdateUiState.InstallRequested(release = release, apkFile = apkFile)
            }
            is InstallLaunchResult.PermissionRequired -> {
                _uiState.value = UpdateUiState.Error(
                    message = PERMISSION_REQUIRED_MESSAGE,
                    recoverableRelease = release,
                )
            }
            is InstallLaunchResult.Failed -> {
                _uiState.value = UpdateUiState.Error(
                    message = result.reason,
                    recoverableRelease = release,
                )
            }
        }
    }

    companion object {
        const val NOT_CONFIGURED_MESSAGE = "Update server URL is not configured in this build."
        const val PERMISSION_REQUIRED_MESSAGE =
            "Allow installing updates from SONIQ in Android Settings, then tap Install update again."
    }
}
