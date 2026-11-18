package com.soniq.player.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.soniq.player.core.database.dao.PlaylistDao
import com.soniq.player.core.database.dao.ThemePresetDao
import com.soniq.player.core.database.dao.UserMediaDao
import com.soniq.player.core.database.entity.FavoriteMediaEntity
import com.soniq.player.core.database.entity.PlaybackHistoryEntity
import com.soniq.player.core.database.entity.PlaybackResumeEntity
import com.soniq.player.core.database.entity.PlaylistEntity
import com.soniq.player.core.database.entity.PlaylistTrackEntity
import com.soniq.player.core.database.entity.ThemePresetEntity

/**
 * SONIQ's persistent user-authored database (`soniq.db`).
 *
 * Schemas are exported to `app/schemas` and committed so every upgrade is verified with
 * `MigrationTestHelper`. Destructive migrations are strictly forbidden on this database.
 */
@Database(
    entities = [
        ThemePresetEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        FavoriteMediaEntity::class,
        PlaybackHistoryEntity::class,
        PlaybackResumeEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class SoniqDatabase : RoomDatabase() {

    abstract fun themePresetDao(): ThemePresetDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun userMediaDao(): UserMediaDao

    companion object {
        /** Referenced by the backup rules in res/xml; keep them in sync. */
        const val NAME = "soniq.db"
    }
}
