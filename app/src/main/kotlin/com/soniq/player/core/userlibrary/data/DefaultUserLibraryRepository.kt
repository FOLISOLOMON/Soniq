package com.soniq.player.core.userlibrary.data

import com.soniq.player.core.common.enumValueOrDefault
import com.soniq.player.core.database.dao.PlaylistDao
import com.soniq.player.core.database.dao.UserMediaDao
import com.soniq.player.core.database.entity.FavoriteMediaEntity
import com.soniq.player.core.database.entity.PlaybackHistoryEntity
import com.soniq.player.core.database.entity.PlaybackResumeEntity
import com.soniq.player.core.database.entity.PlaylistEntity
import com.soniq.player.core.database.entity.PlaylistTrackEntity
import com.soniq.player.core.database.media.dao.AudioTrackDao
import com.soniq.player.core.mediadiscovery.data.toDomain
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.MediaResumePosition
import com.soniq.player.core.model.media.PlaybackHistoryEntry
import com.soniq.player.core.model.media.PlaylistSummary
import com.soniq.player.core.model.media.mediaKey
import com.soniq.player.core.userlibrary.domain.UserLibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultUserLibraryRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val userMediaDao: UserMediaDao,
    private val audioTrackDao: AudioTrackDao,
    private val clock: Clock,
) : UserLibraryRepository {

    override val playlists: Flow<List<PlaylistSummary>> = playlistDao.observePlaylistsWithCounts()
        .map { rows ->
            rows.map { row ->
                PlaylistSummary(
                    id = row.id,
                    name = row.name,
                    trackCount = row.trackCount,
                    createdAtEpochMillis = row.createdAtEpochMillis,
                    updatedAtEpochMillis = row.updatedAtEpochMillis,
                )
            }
        }

    override val favoriteKeys: Flow<Set<String>> = userMediaDao.observeFavoriteKeys()
        .map { it.toSet() }

    override val recentHistory: Flow<List<PlaybackHistoryEntry>> =
        userMediaDao.observeRecentHistory(MAX_HISTORY_ENTRIES).map { rows ->
            rows.map { row ->
                PlaybackHistoryEntry(
                    id = row.id,
                    mediaKey = row.mediaKey,
                    kind = enumValueOrDefault(row.mediaKind, MediaKind.AUDIO),
                    mediaId = row.mediaId,
                    title = row.title,
                    subtitle = row.subtitle,
                    playedAtEpochMillis = row.playedAtEpochMillis,
                )
            }
        }

    override fun observePlaylistTracks(playlistId: Long): Flow<List<AudioTrack>> = combine(
        playlistDao.observePlaylistTracks(playlistId),
        audioTrackDao.observeAll(),
    ) { entries, allAudio ->
        val byId = allAudio.associateBy { it.id }
        entries.mapNotNull { entry -> byId[entry.trackId]?.toDomain() }
    }

    override fun observeResumePositions(kind: MediaKind): Flow<Map<Long, MediaResumePosition>> =
        userMediaDao.observeResumeByKind(kind.name).map { rows ->
            rows.associate { row ->
                row.mediaId to MediaResumePosition(
                    mediaKey = row.mediaKey,
                    kind = enumValueOrDefault(row.mediaKind, kind),
                    mediaId = row.mediaId,
                    positionMs = row.positionMs,
                    durationMs = row.durationMs,
                    updatedAtEpochMillis = row.updatedAtEpochMillis,
                )
            }
        }

    override suspend fun createPlaylist(name: String): Long {
        val trimmed = validatePlaylistName(name)
        val now = clock.millis()
        return playlistDao.insertPlaylist(
            PlaylistEntity(
                name = trimmed,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
    }

    override suspend fun renamePlaylist(playlistId: Long, newName: String): Boolean {
        val trimmed = validatePlaylistName(newName)
        return playlistDao.renamePlaylist(playlistId, trimmed, clock.millis()) > 0
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        val now = clock.millis()
        val nextPosition = playlistDao.getMaxPosition(playlistId) + 1
        playlistDao.insertTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = trackId,
                position = nextPosition,
                addedAtEpochMillis = now,
            ),
        )
        playlistDao.touchPlaylist(playlistId, now)
    }

    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.deleteTrack(playlistId, trackId)
        playlistDao.touchPlaylist(playlistId, clock.millis())
    }

    override suspend fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<Long>) {
        playlistDao.replacePlaylistTracks(playlistId, orderedTrackIds, clock.millis())
    }

    override suspend fun toggleFavorite(kind: MediaKind, mediaId: Long): Boolean {
        val key = mediaKey(kind, mediaId)
        return if (userMediaDao.isFavorite(key)) {
            userMediaDao.deleteFavorite(key)
            false
        } else {
            userMediaDao.upsertFavorite(
                FavoriteMediaEntity(
                    mediaKey = key,
                    mediaKind = kind.name,
                    mediaId = mediaId,
                    favoritedAtEpochMillis = clock.millis(),
                ),
            )
            true
        }
    }

    override suspend fun recordPlayback(
        kind: MediaKind,
        mediaId: Long,
        title: String,
        subtitle: String?,
    ) {
        val key = mediaKey(kind, mediaId)
        userMediaDao.insertHistory(
            PlaybackHistoryEntity(
                mediaKey = key,
                mediaKind = kind.name,
                mediaId = mediaId,
                title = title,
                subtitle = subtitle,
                playedAtEpochMillis = clock.millis(),
            ),
        )
        userMediaDao.pruneHistory(MAX_HISTORY_ENTRIES)
    }

    override suspend fun clearHistory() {
        userMediaDao.clearHistory()
    }

    override suspend fun getResumePosition(kind: MediaKind, mediaId: Long): MediaResumePosition? {
        val row = userMediaDao.getResumePosition(mediaKey(kind, mediaId)) ?: return null
        return MediaResumePosition(
            mediaKey = row.mediaKey,
            kind = enumValueOrDefault(row.mediaKind, kind),
            mediaId = row.mediaId,
            positionMs = row.positionMs,
            durationMs = row.durationMs,
            updatedAtEpochMillis = row.updatedAtEpochMillis,
        )
    }

    override suspend fun saveResumePosition(
        kind: MediaKind,
        mediaId: Long,
        positionMs: Long,
        durationMs: Long,
    ) {
        val key = mediaKey(kind, mediaId)
        if (durationMs <= 0L) return
        val progress = positionMs.toFloat() / durationMs.toFloat()
        if (positionMs < MIN_RESUME_POSITION_MS || progress >= COMPLETION_CLEAR_THRESHOLD) {
            userMediaDao.deleteResumePosition(key)
            return
        }
        userMediaDao.upsertResumePosition(
            PlaybackResumeEntity(
                mediaKey = key,
                mediaKind = kind.name,
                mediaId = mediaId,
                positionMs = positionMs.coerceIn(0L, durationMs),
                durationMs = durationMs,
                updatedAtEpochMillis = clock.millis(),
            ),
        )
    }

    override suspend fun clearResumePosition(kind: MediaKind, mediaId: Long) {
        userMediaDao.deleteResumePosition(mediaKey(kind, mediaId))
    }

    private fun validatePlaylistName(raw: String): String {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "Playlist name must not be blank" }
        require(trimmed.length <= MAX_PLAYLIST_NAME_LENGTH) {
            "Playlist name must be at most $MAX_PLAYLIST_NAME_LENGTH characters"
        }
        return trimmed
    }

    companion object {
        const val MAX_PLAYLIST_NAME_LENGTH = 60
        const val MAX_HISTORY_ENTRIES = 200
        const val MIN_RESUME_POSITION_MS = 5_000L
        const val COMPLETION_CLEAR_THRESHOLD = 0.95f
    }
}
