package com.soniq.player.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.soniq.player.core.database.dao.ThemePresetDao
import com.soniq.player.core.database.entity.ThemePresetEntity

/**
 * SONIQ's local database. Schemas are exported to `app/schemas` and must be committed so that
 * every future migration can be verified with `MigrationTestHelper`.
 *
 * Later stages add the media index, playlists, play history and playback positions here.
 */
@Database(
    entities = [ThemePresetEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SoniqDatabase : RoomDatabase() {

    abstract fun themePresetDao(): ThemePresetDao

    companion object {
        /** Referenced by the backup rules in res/xml; keep them in sync. */
        const val NAME = "soniq.db"
    }
}
