package com.soniq.player.core.database.media.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.soniq.player.core.database.media.entity.AudioTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioTrackDao {

    @Query("SELECT * FROM audio_tracks ORDER BY title COLLATE NOCASE ASC, id ASC")
    fun observeAll(): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE album_id = :albumId ORDER BY track_number ASC, title COLLATE NOCASE ASC")
    fun observeByAlbum(albumId: Long): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE artist = :artist ORDER BY album COLLATE NOCASE ASC, track_number ASC, title COLLATE NOCASE ASC")
    fun observeByArtist(artist: String): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE folder_path = :folderPath ORDER BY title COLLATE NOCASE ASC")
    fun observeByFolder(folderPath: String): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<AudioTrackEntity>

    @Query("SELECT id FROM audio_tracks")
    suspend fun getAllIds(): List<Long>

    @Upsert
    suspend fun upsertAll(tracks: List<AudioTrackEntity>)

    @Query("DELETE FROM audio_tracks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM audio_tracks")
    suspend fun deleteAll()

    /**
     * Synchronizes the cached audio table with the latest MediaStore snapshot in a single
     * transaction, removing only stale rows (chunked to stay well under SQLite's bind limit)
     * and upserting current entries.
     */
    @Transaction
    suspend fun syncWith(tracks: List<AudioTrackEntity>) {
        if (tracks.isEmpty()) {
            deleteAll()
            return
        }
        val incomingIds = tracks.mapTo(HashSet(tracks.size)) { it.id }
        val staleIds = getAllIds().filterNot { it in incomingIds }
        staleIds.chunked(SQLITE_BIND_CHUNK_SIZE).forEach { chunk ->
            deleteByIds(chunk)
        }
        upsertAll(tracks)
    }

    private companion object {
        const val SQLITE_BIND_CHUNK_SIZE = 500
    }
}
