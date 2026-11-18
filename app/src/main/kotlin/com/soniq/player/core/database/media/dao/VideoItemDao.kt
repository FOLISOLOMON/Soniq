package com.soniq.player.core.database.media.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.soniq.player.core.database.media.entity.VideoItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoItemDao {

    @Query("SELECT * FROM video_items ORDER BY date_added DESC, title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<VideoItemEntity>>

    @Query("SELECT * FROM video_items WHERE folder_path = :folderPath ORDER BY date_added DESC, title COLLATE NOCASE ASC")
    fun observeByFolder(folderPath: String): Flow<List<VideoItemEntity>>

    @Query("SELECT id FROM video_items")
    suspend fun getAllIds(): List<Long>

    @Upsert
    suspend fun upsertAll(videos: List<VideoItemEntity>)

    @Query("DELETE FROM video_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM video_items")
    suspend fun deleteAll()

    @Transaction
    suspend fun syncWith(videos: List<VideoItemEntity>) {
        if (videos.isEmpty()) {
            deleteAll()
            return
        }
        val incomingIds = videos.mapTo(HashSet(videos.size)) { it.id }
        val staleIds = getAllIds().filterNot { it in incomingIds }
        staleIds.chunked(SQLITE_BIND_CHUNK_SIZE).forEach { chunk ->
            deleteByIds(chunk)
        }
        upsertAll(videos)
    }

    private companion object {
        const val SQLITE_BIND_CHUNK_SIZE = 500
    }
}
