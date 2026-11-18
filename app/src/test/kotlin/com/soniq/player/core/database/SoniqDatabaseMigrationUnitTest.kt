package com.soniq.player.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.database.entity.PlaylistEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies Room's v1 -> v2 upgrade path directly in JVM/Robolectric unit tests by creating a
 * real v1 SQLite file matching `schemas/.../1.json`, inserting a user preset, running
 * [SoniqDatabaseMigrations.MIGRATION_1_2], and letting Room v2 validate the migrated schema.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SoniqDatabaseMigrationUnitTest {

    @Test
    fun migration1To2PreservesExistingPresetsAndPassesRoomV2Validation() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "soniq_unit_migration_1_2.db"
        context.deleteDatabase(dbName)
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        // Create exact v1 schema matching app/schemas/.../SoniqDatabase/1.json.
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { v1Db ->
            v1Db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `theme_presets` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `theme_mode` TEXT NOT NULL,
                    `amoled_black` INTEGER NOT NULL,
                    `dynamic_color` INTEGER NOT NULL,
                    `accent_argb` INTEGER NOT NULL,
                    `font_family` TEXT NOT NULL,
                    `text_scale` TEXT NOT NULL,
                    `background_style` TEXT NOT NULL,
                    `background_image_uri` TEXT,
                    `player_layout` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            v1Db.execSQL(
                """
                INSERT INTO `theme_presets` (
                    `name`, `theme_mode`, `amoled_black`, `dynamic_color`, `accent_argb`,
                    `font_family`, `text_scale`, `background_style`, `background_image_uri`,
                    `player_layout`, `created_at`
                ) VALUES (
                    'Warm Analog', 'DARK', 1, 0, -8626945,
                    'SERIF', 'LARGE', 'GRADIENT', NULL,
                    'ARTWORK_FOCUS', 1700000000000
                )
                """.trimIndent(),
            )
            v1Db.version = 1
        }

        // Opening with Room v3 runs MIGRATION_1_2 + MIGRATION_2_3 and validates the v3 schema strictly.
        val migrated = Room.databaseBuilder(context, SoniqDatabase::class.java, dbName)
            .addMigrations(
                SoniqDatabaseMigrations.MIGRATION_1_2,
                SoniqDatabaseMigrations.MIGRATION_2_3,
            )
            .allowMainThreadQueries()
            .build()

        try {
            val presets = migrated.themePresetDao().observeAll().first()
            assertThat(presets).hasSize(1)
            assertThat(presets.single().name).isEqualTo("Warm Analog")
            assertThat(presets.single().amoledBlack).isTrue()
            assertThat(presets.single().playerLayout).isEqualTo("ARTWORK_FOCUS")
            assertThat(presets.single().backgroundScrimAlpha).isWithin(0.01f).of(0.72f)
            assertThat(presets.single().backgroundBlurDp).isEqualTo(0)

            val playlistId = migrated.playlistDao().insertPlaylist(
                PlaylistEntity(name = "Late Night", createdAtEpochMillis = 10L, updatedAtEpochMillis = 10L),
            )
            assertThat(playlistId).isGreaterThan(0L)
            assertThat(migrated.playlistDao().observePlaylistsWithCounts().first().single().name)
                .isEqualTo("Late Night")
        } finally {
            migrated.close()
            context.deleteDatabase(dbName)
        }
    }
}
