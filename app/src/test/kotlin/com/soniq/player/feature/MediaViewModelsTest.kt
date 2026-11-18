package com.soniq.player.feature

import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.permissions.MediaAccessState
import com.soniq.player.core.permissions.PermissionChecker
import com.soniq.player.feature.music.MusicViewModel
import com.soniq.player.feature.video.VideoViewModel
import org.junit.Test

class MediaViewModelsTest {

    @Test
    fun musicAndVideoTrackAudioAndVideoPermissionsIndependently() {
        var audioGranted = true
        var videoGranted = false
        val checker = object : PermissionChecker {
            override fun hasMediaAccess(kind: MediaKind): Boolean = when (kind) {
                MediaKind.AUDIO -> audioGranted
                MediaKind.VIDEO -> videoGranted
            }
        }

        val musicViewModel = MusicViewModel(checker)
        val videoViewModel = VideoViewModel(checker)

        assertThat(musicViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.GRANTED)
        assertThat(videoViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.NOT_GRANTED)

        // Permanent denial is preserved across resume until granted in system settings.
        videoViewModel.onPermissionResult(granted = false, permanentlyDenied = true)
        assertThat(videoViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.DENIED_PERMANENTLY)

        videoViewModel.refreshPermission()
        assertThat(videoViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.DENIED_PERMANENTLY)

        // Returning from system settings after granting access transitions to GRANTED.
        videoGranted = true
        videoViewModel.refreshPermission()
        assertThat(videoViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.GRANTED)

        // Revoking audio access transitions Music back to NOT_GRANTED without affecting Video.
        audioGranted = false
        musicViewModel.refreshPermission()
        assertThat(musicViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.NOT_GRANTED)
        assertThat(videoViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.GRANTED)
    }
}
