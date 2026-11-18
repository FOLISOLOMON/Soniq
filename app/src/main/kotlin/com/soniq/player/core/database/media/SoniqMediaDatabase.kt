package com.soniq.player.core.database.media

import androidx.room.Database
import androidx.room.RoomDatabase
import com.soniq.player.core.database.media.dao.AudioTrackDao
import com.soniq.player.core.database.media.dao.VideoItemDao
import com.soniq.player.core.database.media.entity.AudioTrackEntity
import com.soniq.player.core.database.media.entity.VideoItemEntity

/**
 * Rebuildable local MediaStore index cache.
 *
 * Intentionally separated from [com.soniq.player.core.database.SoniqDatabase] (`soniq.db`) so
 * device-specific media index rows are NEVER included in Android cloud backup rules, while
 * user-authored data (theme presets, future playlists) stays safely backed up.
 */
@Database(
    entities = [
        AudioTrackEntity::class,
        VideoItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class SoniqMediaDatabase : RoomDatabase() {

    abstract fun audioTrackDao(): AudioTrackDao
    abstract fun videoItemDao(): VideoItemDao

    companion object {
        /** Excluded from res/xml backup rules because it is rebuilt from local MediaStore. */
        const val NAME = "soniq_media.db"
    }
}
