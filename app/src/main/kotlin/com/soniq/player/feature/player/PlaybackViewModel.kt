package com.soniq.player.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.soniq.player.core.datastore.PlaybackPreferences
import com.soniq.player.core.datastore.PlaybackPreferencesDataSource
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.VideoItem
import com.soniq.player.core.permissions.NotificationPermissionChecker
import com.soniq.player.core.permissions.NotificationPermissions
import com.soniq.player.core.playback.AudioEffectsState
import com.soniq.player.core.playback.PlaybackController
import com.soniq.player.core.playback.PlaybackState
import com.soniq.player.core.playback.SoniqAudioEffectsManager
import com.soniq.player.core.userlibrary.domain.UserLibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Presentation-layer bridge to the application-scoped [PlaybackController] and
 * [UserLibraryRepository]. Automatically records playback history, persists video/audio resume
 * positions, and exposes favorite + Up Next queue actions without owning player lifecycles.
 */
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val playbackPreferences: PlaybackPreferencesDataSource,
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val userLibraryRepository: UserLibraryRepository,
    private val audioEffectsManager: SoniqAudioEffectsManager,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackController.state
    val player: StateFlow<Player?> = playbackController.player
    val audioEffectsState: StateFlow<AudioEffectsState> = audioEffectsManager.state

    val preferences: StateFlow<PlaybackPreferences> = playbackPreferences.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PlaybackPreferences(),
        )

    val isCurrentFavorite: StateFlow<Boolean> = combine(
        playbackController.state.map { it.activeMediaId }.distinctUntilChanged(),
        userLibraryRepository.favoriteKeys,
    ) { activeId, favorites ->
        activeId != null && activeId in favorites
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = false,
    )

    @Volatile
    private var notificationPromptDispatched: Boolean = false
    private var lastRecordedKey: String? = null

    init {
        observePlaybackForHistoryAndResume()
    }

    fun ensureConnected() {
        playbackController.connect()
    }

    fun shouldPromptForNotifications(): Boolean =
        NotificationPermissions.isRuntimePermissionRequired() &&
            !notificationPermissionChecker.canPostNotifications() &&
            !notificationPromptDispatched &&
            !preferences.value.notificationPromptShown

    fun markNotificationPromptShown() {
        notificationPromptDispatched = true
        viewModelScope.launch {
            playbackPreferences.markNotificationPromptShown()
        }
    }

    fun playAudioQueue(tracks: List<AudioTrack>, startIndex: Int) {
        playbackController.playAudioQueue(tracks, startIndex)
    }

    fun playNext(track: AudioTrack) {
        playbackController.playNext(track)
    }

    fun addToQueue(track: AudioTrack) {
        playbackController.addToQueue(track)
    }

    fun skipToQueueIndex(index: Int) {
        playbackController.skipToQueueIndex(index)
    }

    fun removeQueueItem(index: Int) {
        playbackController.removeQueueItem(index)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        playbackController.moveQueueItem(fromIndex, toIndex)
    }

    fun playVideo(video: VideoItem) {
        viewModelScope.launch {
            val savedPosition = userLibraryRepository.getResumePosition(MediaKind.VIDEO, video.id)
                ?.positionMs
                ?: 0L
            playbackController.playVideo(video, startPositionMs = savedPosition)
        }
    }

    fun toggleCurrentFavorite() {
        val active = playbackController.state.value
        val parsed = parseMediaKey(active.activeMediaId) ?: return
        viewModelScope.launch {
            userLibraryRepository.toggleFavorite(parsed.first, parsed.second)
        }
    }

    fun togglePlayPause() {
        saveActiveResumeIfApplicable()
        playbackController.togglePlayPause()
    }

    fun pauseIfVideo() {
        saveActiveResumeIfApplicable()
        if (
            playbackController.state.value.activeMediaKind == MediaKind.VIDEO &&
            !preferences.value.continueVideoInBackground
        ) {
            playbackController.pause()
        }
    }

    fun skipToNext() {
        playbackController.skipToNext()
    }

    fun skipToPrevious() {
        playbackController.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        playbackController.seekTo(positionMs)
    }

    fun seekBy(deltaMs: Long) {
        playbackController.seekBy(deltaMs)
    }

    fun cycleRepeatMode() {
        playbackController.cycleRepeatMode()
    }

    fun toggleShuffle() {
        playbackController.toggleShuffle()
    }

    fun cyclePlaybackSpeed() {
        val current = playbackController.state.value.playbackSpeed
        val nextIndex = SPEED_STEPS.indexOfFirst { it > current + SPEED_EPSILON }
            .let { idx -> if (idx >= 0) idx else 0 }
        playbackController.setPlaybackSpeed(SPEED_STEPS[nextIndex])
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackController.setPlaybackSpeed(speed)
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        audioEffectsManager.setEqualizerEnabled(enabled)
    }

    fun selectEqualizerPreset(presetIndex: Int) {
        audioEffectsManager.selectEqualizerPreset(presetIndex)
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMb: Int) {
        audioEffectsManager.setBandLevel(bandIndex, levelMb)
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        audioEffectsManager.setBassBoostEnabled(enabled)
    }

    fun setBassBoostStrength(strength: Int) {
        audioEffectsManager.setBassBoostStrength(strength)
    }

    fun setGaplessEnabled(enabled: Boolean) {
        audioEffectsManager.setGaplessEnabled(enabled)
    }

    fun setCrossfadeDurationSec(seconds: Int) {
        audioEffectsManager.setCrossfadeDurationSec(seconds)
    }

    fun selectAudioTrack(groupIndex: Int, trackIndex: Int) {
        playbackController.selectAudioTrack(groupIndex, trackIndex)
    }

    fun selectSubtitleTrack(groupIndex: Int?, trackIndex: Int?) {
        playbackController.selectSubtitleTrack(groupIndex, trackIndex)
    }

    fun addExternalSubtitle(uri: String, mimeType: String, label: String) {
        playbackController.addExternalSubtitle(uri, mimeType, label)
    }

    fun clearPlaybackError() {
        playbackController.clearPlaybackError()
    }

    private fun observePlaybackForHistoryAndResume() {
        viewModelScope.launch {
            playbackController.state
                .map { Triple(it.activeMediaId, it.isPlaying, it.title) }
                .distinctUntilChanged()
                .collect { (mediaId, isPlaying, title) ->
                    if (isPlaying && mediaId != null && mediaId != lastRecordedKey && !title.isNullOrBlank()) {
                        val parsed = parseMediaKey(mediaId) ?: return@collect
                        lastRecordedKey = mediaId
                        userLibraryRepository.recordPlayback(
                            kind = parsed.first,
                            mediaId = parsed.second,
                            title = title,
                            subtitle = playbackController.state.value.subtitle,
                        )
                    } else if (!isPlaying && mediaId != null) {
                        saveActiveResumeIfApplicable()
                    }
                }
        }
    }

    private fun saveActiveResumeIfApplicable() {
        val current = playbackController.state.value
        val parsed = parseMediaKey(current.activeMediaId) ?: return
        if (current.durationMs <= 0L) return
        viewModelScope.launch {
            userLibraryRepository.saveResumePosition(
                kind = parsed.first,
                mediaId = parsed.second,
                positionMs = current.positionMs,
                durationMs = current.durationMs,
            )
        }
    }

    companion object {
        val SPEED_STEPS: List<Float> = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        private const val SPEED_EPSILON = 0.05f
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun formatSpeed(speed: Float): String =
            if (speed == speed.toInt().toFloat()) {
                speed.toInt().toString()
            } else {
                speed.toString().trimEnd('0').trimEnd('.')
            }

        internal fun parseMediaKey(mediaKey: String?): Pair<MediaKind, Long>? {
            if (mediaKey.isNullOrBlank()) return null
            val parts = mediaKey.split(':')
            if (parts.size != 2) return null
            val kind = when (parts[0].lowercase()) {
                "audio" -> MediaKind.AUDIO
                "video" -> MediaKind.VIDEO
                else -> return null
            }
            val id = parts[1].toLongOrNull() ?: return null
            return kind to id
        }
    }
}
