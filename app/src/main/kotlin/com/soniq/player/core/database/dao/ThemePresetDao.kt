package com.soniq.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.soniq.player.core.database.entity.ThemePresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemePresetDao {

    @Query("SELECT * FROM theme_presets ORDER BY created_at DESC, id DESC")
    fun observeAll(): Flow<List<ThemePresetEntity>>

    @Query("SELECT * FROM theme_presets WHERE id = :id")
    suspend fun getById(id: Long): ThemePresetEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(preset: ThemePresetEntity): Long

    /** @return the number of updated rows (0 or 1). */
    @Query("UPDATE theme_presets SET name = :name WHERE id = :id")
    suspend fun renameById(id: Long, name: String): Int

    /** @return the number of deleted rows (0 or 1). */
    @Query("DELETE FROM theme_presets WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
