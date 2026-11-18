package com.soniq.player.feature.video

import androidx.lifecycle.ViewModel
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.permissions.MediaAccessState
import com.soniq.player.core.permissions.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Unidirectional UI state for the Video destination. */
data class VideoUiState(
    val accessState: MediaAccessState = MediaAccessState.NOT_GRANTED,
)

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoUiState(accessState = initialAccessState()))
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    /** Re-checks permission state, e.g., when returning from system app settings. */
    fun refreshPermission() {
        val granted = permissionChecker.hasMediaAccess(MediaKind.VIDEO)
        _uiState.update { current ->
            current.copy(
                accessState = when {
                    granted -> MediaAccessState.GRANTED
                    current.accessState == MediaAccessState.DENIED_PERMANENTLY -> MediaAccessState.DENIED_PERMANENTLY
                    else -> MediaAccessState.NOT_GRANTED
                },
            )
        }
    }

    fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        _uiState.update {
            it.copy(
                accessState = when {
                    granted -> MediaAccessState.GRANTED
                    permanentlyDenied -> MediaAccessState.DENIED_PERMANENTLY
                    else -> MediaAccessState.NOT_GRANTED
                },
            )
        }
    }

    private fun initialAccessState(): MediaAccessState =
        if (permissionChecker.hasMediaAccess(MediaKind.VIDEO)) {
            MediaAccessState.GRANTED
        } else {
            MediaAccessState.NOT_GRANTED
        }
}
