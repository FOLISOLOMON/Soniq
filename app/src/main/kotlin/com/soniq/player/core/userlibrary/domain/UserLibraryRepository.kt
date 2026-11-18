package com.soniq.player.core.userlibrary.domain

import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.MediaResumePosition
import com.soniq.player.core.model.media.PlaybackHistoryEntry
import com.soniq.player.core.model.media.PlaylistSummary
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for user-authored library state persisted in `soniq.db`:
 * custom playlists, favorites, playback history, and playback resume positions.
 */
interface UserLibraryRepository {

    val playlists: Flow<List<PlaylistSummary>>
    val favoriteKeys: Flow<Set<String>>
    val recentHistory: Flow<List<PlaybackHistoryEntry>>

    fun observePlaylistTracks(playlistId: Long): Flow<List<AudioTrack>>
    fun observeResumePositions(kind: MediaKind): Flow<Map<Long, MediaResumePosition>>

    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(playlistId: Long, newName: String): Boolean
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long)
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)
    suspend fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<Long>)

    /** Toggles favorite status and returns the new favorited state (`true` if now favorite). */
    suspend fun toggleFavorite(kind: MediaKind, mediaId: Long): Boolean

    suspend fun recordPlayback(kind: MediaKind, mediaId: Long, title: String, subtitle: String?)
    suspend fun clearHistory()

    suspend fun getResumePosition(kind: MediaKind, mediaId: Long): MediaResumePosition?
    suspend fun saveResumePosition(kind: MediaKind, mediaId: Long, positionMs: Long, durationMs: Long)
    suspend fun clearResumePosition(kind: MediaKind, mediaId: Long)
}
