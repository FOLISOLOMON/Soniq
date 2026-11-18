package com.soniq.player.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Explicit, non-destructive Room migrations for [SoniqDatabase] (`soniq.db`).
 * Every migration preserves all existing user-authored records (theme presets, playlists, etc.)
 * and is verified against exported schemas in `SoniqDatabaseMigrationTest`.
 */
object SoniqDatabaseMigrations {

    /** Adds playlists, playlist tracks, favorites, playback history, and resume positions. */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playlists` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playlist_tracks` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `playlist_id` INTEGER NOT NULL,
                    `track_id` INTEGER NOT NULL,
                    `position` INTEGER NOT NULL,
                    `added_at` INTEGER NOT NULL,
                    FOREIGN KEY(`playlist_id`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_playlist_tracks_playlist_id` ON `playlist_tracks` (`playlist_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_playlist_tracks_track_id` ON `playlist_tracks` (`track_id`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `favorite_media` (
                    `media_key` TEXT NOT NULL,
                    `media_kind` TEXT NOT NULL,
                    `media_id` INTEGER NOT NULL,
                    `favorited_at` INTEGER NOT NULL,
                    PRIMARY KEY(`media_key`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_favorite_media_media_kind` ON `favorite_media` (`media_kind`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playback_history` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `media_key` TEXT NOT NULL,
                    `media_kind` TEXT NOT NULL,
                    `media_id` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `subtitle` TEXT,
                    `played_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_playback_history_played_at` ON `playback_history` (`played_at`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_playback_history_media_key` ON `playback_history` (`media_key`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playback_resume` (
                    `media_key` TEXT NOT NULL,
                    `media_kind` TEXT NOT NULL,
                    `media_id` INTEGER NOT NULL,
                    `position_ms` INTEGER NOT NULL,
                    `duration_ms` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    PRIMARY KEY(`media_key`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_playback_resume_media_kind` ON `playback_resume` (`media_kind`)",
            )
        }
    }
}
