package com.soniq.player.feature

import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.permissions.MediaAccessGrant
import com.soniq.player.core.permissions.MediaAccessState
import com.soniq.player.core.permissions.PermissionChecker
import com.soniq.player.feature.music.MusicViewModel
import com.soniq.player.feature.video.VideoViewModel
import org.junit.Test

class MediaViewModelsTest {

    @Test
    fun musicAndVideoTrackAudioAndVideoPermissionsIndependentlyIncludingPartialVideoAccess() {
        var audioGrant = MediaAccessGrant.FULL
        var videoGrant = MediaAccessGrant.NONE
        val checker = object : PermissionChecker {
            override fun checkMediaAccess(kind: MediaKind): MediaAccessGrant = when (kind) {
                MediaKind.AUDIO -> audioGrant
                MediaKind.VIDEO -> videoGrant
            }
        }

        val musicViewModel = MusicViewModel(checker)
        val videoViewModel = VideoViewModel(checker)

        assertThat(musicViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.GRANTED)
        assertThat(videoViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.NOT_GRANTED)

        // Android 14+ partial video access transitions Video to PARTIAL without touching Music.
        videoViewModel.onPermissionResult(grant = MediaAccessGrant.PARTIAL, permanentlyDenied = false)
        assertThat(videoViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.PARTIAL)

        // Permanent denial is preserved across resume until granted in system settings.
        videoViewModel.onPermissionResult(grant = MediaAccessGrant.NONE, permanentlyDenied = true)
        assertThat(videoViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.DENIED_PERMANENTLY)

        videoViewModel.refreshPermission()
        assertThat(videoViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.DENIED_PERMANENTLY)

        // Returning from system settings after granting full access transitions to GRANTED.
        videoGrant = MediaAccessGrant.FULL
        videoViewModel.refreshPermission()
        assertThat(videoViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.GRANTED)

        // Revoking audio access transitions Music back to NOT_GRANTED without affecting Video.
        audioGrant = MediaAccessGrant.NONE
        musicViewModel.refreshPermission()
        assertThat(musicViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.NOT_GRANTED)
        assertThat(videoViewModel.uiState.value.accessState).isEqualTo(MediaAccessState.GRANTED)
    }
}
