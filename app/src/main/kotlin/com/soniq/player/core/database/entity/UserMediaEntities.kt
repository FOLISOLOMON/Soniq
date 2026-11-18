package com.soniq.player.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["playlist_id"], name = "index_playlist_tracks_playlist_id"),
        Index(value = ["track_id"], name = "index_playlist_tracks_track_id"),
    ],
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,
    @ColumnInfo(name = "track_id")
    val trackId: Long,
    @ColumnInfo(name = "position")
    val position: Int,
    @ColumnInfo(name = "added_at")
    val addedAtEpochMillis: Long,
)

@Entity(
    tableName = "favorite_media",
    indices = [
        Index(value = ["media_kind"], name = "index_favorite_media_media_kind"),
    ],
)
data class FavoriteMediaEntity(
    @PrimaryKey
    @ColumnInfo(name = "media_key")
    val mediaKey: String,
    @ColumnInfo(name = "media_kind")
    val mediaKind: String,
    @ColumnInfo(name = "media_id")
    val mediaId: Long,
    @ColumnInfo(name = "favorited_at")
    val favoritedAtEpochMillis: Long,
)

@Entity(
    tableName = "playback_history",
    indices = [
        Index(value = ["played_at"], name = "index_playback_history_played_at"),
        Index(value = ["media_key"], name = "index_playback_history_media_key"),
    ],
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "media_key")
    val mediaKey: String,
    @ColumnInfo(name = "media_kind")
    val mediaKind: String,
    @ColumnInfo(name = "media_id")
    val mediaId: Long,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "subtitle")
    val subtitle: String?,
    @ColumnInfo(name = "played_at")
    val playedAtEpochMillis: Long,
)

@Entity(
    tableName = "playback_resume",
    indices = [
        Index(value = ["media_kind"], name = "index_playback_resume_media_kind"),
    ],
)
data class PlaybackResumeEntity(
    @PrimaryKey
    @ColumnInfo(name = "media_key")
    val mediaKey: String,
    @ColumnInfo(name = "media_kind")
    val mediaKind: String,
    @ColumnInfo(name = "media_id")
    val mediaId: Long,
    @ColumnInfo(name = "position_ms")
    val positionMs: Long,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAtEpochMillis: Long,
)
