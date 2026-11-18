package com.soniq.player.core.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.soniq.player.core.database.media.dao.AudioTrackDao
import com.soniq.player.core.datastore.PlaybackPreferencesDataSource
import com.soniq.player.core.datastore.PlaybackSessionSnapshot
import com.soniq.player.core.mediadiscovery.data.toDomain
import com.soniq.player.core.model.media.MediaKind
import javax.inject.Inject
import javax.inject.Singleton

/** Restored Media3 audio queue + start index/position + playback modes. */
data class RestoredAudioSession(
    val mediaItems: List<MediaItem>,
    val startIndex: Int,
    val startPositionMs: Long,
    val repeatMode: PlaybackRepeatMode,
    val shuffleEnabled: Boolean,
    val playbackSpeed: Float,
)

/**
 * Coordinates checkpointing and restoring audio playback sessions across service destruction,
 * app restarts, and Media3 system playback resumption (`onPlaybackResumption`).
 */
@Singleton
class PlaybackRestorationManager @Inject constructor(
    private val preferences: PlaybackPreferencesDataSource,
    private val audioTrackDao: AudioTrackDao,
) {

    /**
     * Captures the current audio queue and position from [player] and persists it.
     * Video sessions are intentionally excluded from audio resumption snapshots.
     */
    suspend fun saveFromPlayer(player: Player) {
        val currentItem = player.currentMediaItem
        if (MediaItemFactory.resolveMediaKind(currentItem) != MediaKind.AUDIO) return

        val trackIds = buildList(player.mediaItemCount) {
            for (index in 0 until player.mediaItemCount) {
                val id = SoniqSessionCommands.extractAudioTrackId(player.getMediaItemAt(index).mediaId)
                if (id != null) add(id)
            }
        }
        if (trackIds.isEmpty()) return

        val snapshot = PlaybackSessionSnapshot(
            queueTrackIds = trackIds,
            currentIndex = player.currentMediaItemIndex.coerceIn(0, trackIds.lastIndex),
            positionMs = player.currentPosition.coerceAtLeast(0L),
            repeatMode = player.repeatMode.toDomainRepeatMode(),
            shuffleEnabled = player.shuffleModeEnabled,
            playbackSpeed = player.playbackParameters.speed,
        )
        preferences.saveSessionSnapshot(snapshot)
    }

    /** Clears any persisted audio queue snapshot (e.g. when the user empties the queue). */
    suspend fun clearAudioSession() {
        preferences.saveSessionSnapshot(PlaybackSessionSnapshot())
    }

    /** Persists a snapshot whose fields were already captured on the Player's looper thread. */
    suspend fun saveSnapshotDirectly(
        queueTrackIds: List<Long>,
        currentIndex: Int,
        positionMs: Long,
        repeatMode: PlaybackRepeatMode,
        shuffleEnabled: Boolean,
        playbackSpeed: Float,
    ) {
        if (queueTrackIds.isEmpty()) return
        preferences.saveSessionSnapshot(
            PlaybackSessionSnapshot(
                queueTrackIds = queueTrackIds,
                currentIndex = currentIndex.coerceIn(0, queueTrackIds.lastIndex),
                positionMs = positionMs.coerceAtLeast(0L),
                repeatMode = repeatMode,
                shuffleEnabled = shuffleEnabled,
                playbackSpeed = playbackSpeed,
            ),
        )
    }

    /**
     * Reconstructs the last persisted audio queue against currently indexed tracks in
     * [AudioTrackDao]. Preserves original queue ordering, gracefully prunes tracks that were
     * deleted from disk, and recalculates the active index cleanly.
     */
    suspend fun restoreLastAudioSession(): RestoredAudioSession? {
        val snapshot = preferences.getSessionSnapshot()
        if (!snapshot.isRestorable) return null

        val fetchedById = snapshot.queueTrackIds
            .distinct()
            .chunked(SQLITE_BIND_CHUNK_SIZE)
            .flatMap { chunk -> audioTrackDao.getByIds(chunk) }
            .associateBy { it.id }

        if (fetchedById.isEmpty()) return null

        val targetId = snapshot.queueTrackIds.getOrNull(snapshot.currentIndex)
        val restoredTracks = snapshot.queueTrackIds.mapNotNull { id -> fetchedById[id]?.toDomain() }
        if (restoredTracks.isEmpty()) return null

        val restoredIndex = targetId
            ?.let { id -> restoredTracks.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 }
            ?: snapshot.currentIndex.coerceIn(0, restoredTracks.lastIndex)
        val restoredPosition = if (restoredTracks[restoredIndex].id == targetId) {
            snapshot.positionMs.coerceIn(0L, restoredTracks[restoredIndex].durationMs.coerceAtLeast(0L))
        } else {
            0L
        }

        return RestoredAudioSession(
            mediaItems = restoredTracks.map(MediaItemFactory::fromAudioTrack),
            startIndex = restoredIndex,
            startPositionMs = restoredPosition,
            repeatMode = snapshot.repeatMode,
            shuffleEnabled = snapshot.shuffleEnabled,
            playbackSpeed = snapshot.playbackSpeed,
        )
    }

    private companion object {
        const val SQLITE_BIND_CHUNK_SIZE = 500

        fun Int.toDomainRepeatMode(): PlaybackRepeatMode = when (this) {
            Player.REPEAT_MODE_ALL -> PlaybackRepeatMode.ALL
            Player.REPEAT_MODE_ONE -> PlaybackRepeatMode.ONE
            else -> PlaybackRepeatMode.OFF
        }
    }
}
