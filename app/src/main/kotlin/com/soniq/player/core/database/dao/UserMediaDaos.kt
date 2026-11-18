package com.soniq.player.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.soniq.player.core.database.entity.FavoriteMediaEntity
import com.soniq.player.core.database.entity.PlaybackHistoryEntity
import com.soniq.player.core.database.entity.PlaybackResumeEntity
import com.soniq.player.core.database.entity.PlaylistEntity
import com.soniq.player.core.database.entity.PlaylistTrackEntity
import kotlinx.coroutines.flow.Flow

/** Projection combining playlist metadata with its current track count. */
data class PlaylistWithCountRow(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "track_count") val trackCount: Int,
)

@Dao
interface PlaylistDao {

    @Query(
        """
        SELECT p.id, p.name, p.created_at, p.updated_at, COUNT(pt.id) AS track_count
        FROM playlists p
        LEFT JOIN playlist_tracks pt ON pt.playlist_id = p.id
        GROUP BY p.id
        ORDER BY p.updated_at DESC, p.id DESC
        """,
    )
    fun observePlaylistsWithCounts(): Flow<List<PlaylistWithCountRow>>

    @Query("SELECT * FROM playlist_tracks WHERE playlist_id = :playlistId ORDER BY position ASC, id ASC")
    fun observePlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlist_id = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName, updated_at = :updatedAt WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String, updatedAt: Long): Int

    @Query("UPDATE playlists SET updated_at = :updatedAt WHERE id = :playlistId")
    suspend fun touchPlaylist(playlistId: Long, updatedAt: Long)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTrack(entry: PlaylistTrackEntity): Long

    @Query("DELETE FROM playlist_tracks WHERE playlist_id = :playlistId AND track_id = :trackId")
    suspend fun deleteTrack(playlistId: Long, trackId: Long): Int

    @Query("DELETE FROM playlist_tracks WHERE playlist_id = :playlistId")
    suspend fun deleteAllTracks(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTracks(entries: List<PlaylistTrackEntity>)

    @Transaction
    suspend fun replacePlaylistTracks(
        playlistId: Long,
        orderedTrackIds: List<Long>,
        timestamp: Long,
    ) {
        deleteAllTracks(playlistId)
        if (orderedTrackIds.isNotEmpty()) {
            val entries = orderedTrackIds.mapIndexed { index, trackId ->
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    trackId = trackId,
                    position = index,
                    addedAtEpochMillis = timestamp,
                )
            }
            insertTracks(entries)
        }
        touchPlaylist(playlistId, timestamp)
    }
}

@Dao
interface UserMediaDao {

    // Favorites
    @Query("SELECT media_key FROM favorite_media")
    fun observeFavoriteKeys(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_media WHERE media_key = :mediaKey)")
    suspend fun isFavorite(mediaKey: String): Boolean

    @Upsert
    suspend fun upsertFavorite(entity: FavoriteMediaEntity)

    @Query("DELETE FROM favorite_media WHERE media_key = :mediaKey")
    suspend fun deleteFavorite(mediaKey: String): Int

    // History
    @Query("SELECT * FROM playback_history ORDER BY played_at DESC, id DESC LIMIT :limit")
    fun observeRecentHistory(limit: Int): Flow<List<PlaybackHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHistory(entity: PlaybackHistoryEntity): Long

    @Query(
        """
        DELETE FROM playback_history
        WHERE id NOT IN (
            SELECT id FROM playback_history ORDER BY played_at DESC, id DESC LIMIT :keepCount
        )
        """,
    )
    suspend fun pruneHistory(keepCount: Int)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    // Resume positions
    @Query("SELECT * FROM playback_resume WHERE media_kind = :mediaKind")
    fun observeResumeByKind(mediaKind: String): Flow<List<PlaybackResumeEntity>>

    @Query("SELECT * FROM playback_resume WHERE media_key = :mediaKey")
    suspend fun getResumePosition(mediaKey: String): PlaybackResumeEntity?

    @Upsert
    suspend fun upsertResumePosition(entity: PlaybackResumeEntity)

    @Query("DELETE FROM playback_resume WHERE media_key = :mediaKey")
    suspend fun deleteResumePosition(mediaKey: String): Int
}
